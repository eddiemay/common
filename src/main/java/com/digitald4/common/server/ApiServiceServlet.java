package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.model.User;
import com.digitald4.common.proto.DD4Protos.ActiveSession;
import com.digitald4.common.proto.DD4Protos.DataFile;
import com.digitald4.common.server.service.FileService;
import com.digitald4.common.server.service.FileService.FileJSONService;
import com.digitald4.common.server.service.JSONService;
import com.digitald4.common.server.service.JSONServiceImpl;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.server.service.UserService;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.Emailer;
import com.digitald4.common.util.Encryptor;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Provider;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.protobuf.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ApiServiceServlet extends HttpServlet {
	public enum ServerType {TOMCAT, APPENGINE};
	protected ServerType serverType;
	private static final List<String> SPECIAL_PARAMETERS = Arrays.asList("json", "idToken", "orderBy", "pageSize", "pageToken");

	private final Map<String, JSONService> services = new HashMap<>();
	private final IdTokenResolver idTokenResolver;
	private DAO dao;
	private Emailer emailer;
	private Encryptor encryptor;
	protected final Provider<DAO> daoProvider = () -> dao;
	protected final GeneralDataStore generalDataStore;
	protected final UserStore userStore;
	protected final UserService userService;
	protected final Store<DataFile> dataFileStore;
	protected final ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();
	protected final ProviderThreadLocalImpl<HttpServletRequest> requestProvider = new ProviderThreadLocalImpl<>();
	protected final ProviderThreadLocalImpl<HttpServletResponse> responseProvider = new ProviderThreadLocalImpl<>();
	protected boolean useViews;

	public ApiServiceServlet() {
		Clock clock = Clock.systemUTC();

		userStore = new BasicUserStore(daoProvider, clock);

		idTokenResolver = new IdTokenResolverDD4Impl(new GenericStore<ActiveSession>(ActiveSession.class, daoProvider), userStore, clock);

		generalDataStore = new GeneralDataStore(daoProvider);
		addService("generalData",
				new JSONServiceImpl<>(new EntityServiceImpl<>(generalDataStore), false));
		addService("user",
				new UserService.UserJSONService(userService = new UserService(userStore, userProvider, idTokenResolver, clock)));

		dataFileStore = new GenericStore<>(DataFile.class, daoProvider);
		FileService fileService = new FileService(dataFileStore, requestProvider, responseProvider);
		addService("file", new FileJSONService(fileService));
	}

	public void init() {
		ServletContext sc = getServletContext();
		serverType = sc.getServerInfo().contains("Tomcat") ? ServerType.TOMCAT : ServerType.APPENGINE;
		TypedDAO<Message> messageDAO;
		DAO defaultDAO;
		if (serverType == ServerType.TOMCAT) {
			// We use Tomcat with MySQL, so if Tomcat, MySQL
			messageDAO = new DAOSQLImpl(
					new DBConnectorThreadPoolImpl(
							sc.getInitParameter("dbdriver"),
							sc.getInitParameter("dburl"),
							sc.getInitParameter("dbuser"),
							sc.getInitParameter("dbpass")),
					useViews);
			defaultDAO = null;
		} else {
			// We use CloudDataStore with AppEngine.
			messageDAO = new DAOCloudDSProto(DatastoreOptions.getDefaultInstance().getService());
			defaultDAO = new DAOCloudDS(DatastoreServiceFactory.getDatastoreService());
		}
		dao = new DAORouterImpl(messageDAO, new HasProtoDAO(messageDAO), defaultDAO);
	}

	protected ApiServiceServlet addService(String entity, JSONService service) {
		services.put(entity.toLowerCase(), service);
		return this;
	}

	private JSONService getService(String entity) {
		JSONService service = services.get(entity.toLowerCase());
		if (service == null) {
			throw new DD4StorageException("Unknown service: " + entity, HttpServletResponse.SC_BAD_REQUEST);
		}
		return service;
	}

	private boolean isService(String entity) {
		return services.containsKey(entity.toLowerCase());
	}

	protected void processRequest(String entity, String action, JSONObject jsonRequest,
																HttpServletRequest request, HttpServletResponse response) throws ServletException {
		userProvider.set(idTokenResolver.resolve(request.getParameter("idToken")));
		requestProvider.set(request);
		responseProvider.set(response);
		try {
			JSONObject json = null;
			try {
				JSONService service = getService(entity);
				if (service.requiresLogin(action)) {
					checkLogin(request, response);
				}
				json = service.performAction(action, jsonRequest);
			} catch (DD4StorageException e) {
				response.setStatus(e.getErrorCode());
				json = new JSONObject()
						.put("error", e.getMessage())
						.put("stackTrace", formatStackTrace(e))
						.put("requestParams", String.valueOf(request.getParameterMap().keySet()))
						.put("jsonRequest", jsonRequest.toString())
						.put("queryString", request.getQueryString());
				e.printStackTrace();
			} finally {
				if (json != null) {
					response.setContentType("application/json");
					response.setHeader("Cache-Control", "no-cache, must-revalidate");
					response.getWriter().println(json);
				}
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		Pair<EntityKey, JSONObject> pair = parseRequest(request);
		EntityKey entityKey = pair.getLeft();
		String action = entityKey.action != null ? entityKey.action : entityKey.key == null ? "create" : "update";
		processRequest(entityKey.entity, action, pair.getRight(), request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		Pair<EntityKey, JSONObject> pair = parseRequest(request);
		EntityKey entityKey = pair.getLeft();
		String action = entityKey.action != null ? entityKey.action : entityKey.key == null ? "list" : "get";
		processRequest(entityKey.entity, action, pair.getRight(), request, response);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		Pair<EntityKey, JSONObject> pair = parseRequest(request);
		EntityKey entityKey = pair.getLeft();
		String action = entityKey.action != null ? entityKey.action : "update";
		processRequest(entityKey.entity, action, pair.getRight(), request, response);
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		Pair<EntityKey, JSONObject> pair = parseRequest(request);
		EntityKey entityKey = pair.getLeft();
		String action = entityKey.action != null ? entityKey.action : "delete";
		processRequest(entityKey.entity, action, pair.getRight(), request, response);
	}

	private class EntityKey {
		private final String entity;
		private String key;
		private String action;

		private EntityKey(String entity, String key) {
			this.entity = entity;
			this.key = key;
		}
	}

	private List<EntityKey> getEntitykeys(HttpServletRequest request) {
		/* /entity
			/entity/id
			/entity:action
			/entity/id:action

			/pentity/id/entity
			/pentity/id/entity/id
			/pentity/id/entity:action
			/pentity/id/entity/id:action

			/pentity/id/pentity/id/entity
			/pentity/id/pentity/id/entity/id
			/pentity/id/pentity/id/entity:action
			/pentity/id/pentity/id/entity/id:action
		 */
		List<EntityKey> entityGroups = new ArrayList<>();
		String url = request.getRequestURL().toString();
		String subUrl = url.substring(url.indexOf("/api/") + 5);
		String customAction = null;
		if (subUrl.contains(":")) {
			customAction = subUrl.substring(subUrl.indexOf(":") + 1);
			subUrl = subUrl.substring(0, subUrl.indexOf(":"));
		}
		String[] parts = subUrl.split("/");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			String entityName = part.substring(0, part.length() - 1);
			entityName = isService(entityName) ? entityName : part;
			entityGroups.add(new EntityKey(entityName, (i + 1 < parts.length) ? parts[++i] : null));
		}
		entityGroups.get(entityGroups.size() - 1).action = customAction;
		return entityGroups;
	}

	private Pair<EntityKey, JSONObject> parseRequest(HttpServletRequest request) {
		List<EntityKey> entitykeys = getEntitykeys(request);
		String payload = request.getParameter("json");
		try {
			EntityKey entity = entitykeys.get(entitykeys.size() - 1);
			JSONObject json = payload != null ? new JSONObject(payload) : new JSONObject();
			json.put("id", entity.key);
			for (int i = 0; i < entitykeys.size() - 1; i++) {
				EntityKey entitykey = entitykeys.get(i);
				String column = entitykey.entity + (isService(entitykey.entity) ? "_id" : "");
				json.put(column, entitykey.key);
			}

			JSONArray parameters = json.has("filter") ? json.getJSONArray("filter") : new JSONArray();
			for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
				if (!SPECIAL_PARAMETERS.contains(entry.getKey())) {
					for (String value : entry.getValue()) {
						int pos = 0;
						while (value.charAt(pos) >= '<' && value.charAt(pos) <= '>') {
							pos++;
						}
						String col = entry.getKey();
						parameters.put(new JSONObject()
								.put("column", col.endsWith("_1") || col.endsWith("_2") ? col.substring(0, col.length() - 2) : col)
								.put("operator", pos > 0 ? value.substring(0, pos) : "=")
								.put("value", value.substring(pos)));
					}
				} else if (!entry.getKey().equals("json") && !entry.getKey().equals("idToken")) {
					json.put(entry.getKey(), entry.getValue()[0]);
				}
			}
			if (parameters.length() > 0) {
				json.put("filter", parameters);
			}
			return Pair.of(entity, json);
		} catch (JSONException e) {
			throw new DD4StorageException("Malformed request", e, HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
	public Emailer getEmailer() {
		if (emailer == null) {
			ServletContext sc = getServletContext();
			emailer = new Emailer(sc.getInitParameter("emailserver"),
					sc.getInitParameter("emailuser"), sc.getInitParameter("emailpass"));
		}
		return emailer;
	}
	
	public static String getFullURL(HttpServletRequest request) {
    StringBuffer requestURL = request.getRequestURL();
    String queryString = request.getQueryString();
    return (queryString == null) ? requestURL.toString() : requestURL.append('?').append(queryString).toString();
	}
	
	public void checkLogin(HttpServletRequest request, HttpServletResponse response, int level) {
		User user = userProvider.get();
		if (user == null || user.getId() == 0) {
			throw new DD4StorageException("Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
		}
		if (user.getTypeId() > level) {
			throw new DD4StorageException("Access Denied", HttpServletResponse.SC_FORBIDDEN);
		}
	}

	public void checkLogin(HttpServletRequest request, HttpServletResponse response) {
		checkLogin(request, response, 4);
	}
	
	public void checkAdminLogin(HttpServletRequest request, HttpServletResponse response) {
		checkLogin(request, response, 1);
	}

	private static String formatStackTrace(Exception e) {
		StringBuilder out = new StringBuilder();
		for (StackTraceElement elem : e.getStackTrace()) {
			out.append(elem).append("\n");
		}
		return out.toString();
	}
}
