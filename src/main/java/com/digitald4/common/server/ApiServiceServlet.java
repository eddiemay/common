package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.DataFile;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.storage.DAOConnectorImpl;
import com.digitald4.common.storage.DataConnector;
import com.digitald4.common.storage.DataConnectorCloudDS;
import com.digitald4.common.storage.DataConnectorSQLImpl;
import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Emailer;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.Provider;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ApiServiceServlet extends HttpServlet {
	public enum ServerType {TOMCAT, APPENGINE};
	protected ServerType serverType;

	private final Map<String, JSONService> services = new HashMap<>();
	private final IdTokenResolver idTokenResolver;
	private DataConnector dataConnector;
	private Emailer emailer;
	protected final Provider<DataConnector> dataConnectorProvider = () -> dataConnector;
	protected final GeneralDataStore generalDataStore;
	protected final UserStore userStore;
	protected final UserService userService;
	protected final GenericStore<DataFile> dataFileStore;
	protected final ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();
	protected final ProviderThreadLocalImpl<HttpServletRequest> requestProvider = new ProviderThreadLocalImpl<>();
	protected final ProviderThreadLocalImpl<HttpServletResponse> responseProvider = new ProviderThreadLocalImpl<>();
	private String autoLoginId;

	public ApiServiceServlet() {
		Clock clock = Clock.systemUTC();
		idTokenResolver = new IdTokenResolverDD4Impl(clock);

		generalDataStore = new GeneralDataStore(new DAOConnectorImpl<>(GeneralData.class, dataConnectorProvider));
		addService("general_data", new SingleProtoService<>(generalDataStore));

		userStore = new UserStore(new DAOConnectorImpl<>(User.class, dataConnectorProvider), clock);
		addService("user", userService = new UserService(userStore, userProvider, idTokenResolver));

		dataFileStore = new GenericStore<>(new DAOConnectorImpl<>(DataFile.class, dataConnectorProvider));
		addService("file", new FileService(dataFileStore, requestProvider, responseProvider));
	}

	public void init() {
		ServletContext sc = getServletContext();
		serverType = sc.getServerInfo().contains("Tomcat") ? ServerType.TOMCAT : ServerType.APPENGINE;
		autoLoginId = getServletContext().getInitParameter("auto_login_id");
		if (serverType == ServerType.TOMCAT) {
			// We use Tomcat with MySQL, so if Tomcat, MySQL
			dataConnector = new DataConnectorSQLImpl(new DBConnectorThreadPoolImpl()
					.connect(sc.getInitParameter("dbdriver"),
							sc.getInitParameter("dburl"),
							sc.getInitParameter("dbuser"),
							sc.getInitParameter("dbpass")));
		} else {
			// We use CloudDataStore with AppEngine.
			dataConnector = new DataConnectorCloudDS();
		}
	}

	protected ApiServiceServlet addService(String entity, JSONService service) {
		services.put(entity, service);
		return this;
	}

	protected void processRequest(String entity, String action, JSONObject jsonRequest,
																HttpServletRequest request, HttpServletResponse response) throws ServletException {
		userProvider.set(idTokenResolver.resolve(request.getParameter("idToken")));
		requestProvider.set(request);
		responseProvider.set(response);
		try {
			JSONObject json = null;
			try {
				JSONService service = services.get(entity);
				if (service == null) {
					throw new DD4StorageException("Unknown service: " + entity);
				}
				if (service.requiresLogin(action) && !checkLogin(request, response)) return;
				json = service.performAction(action, jsonRequest);
			/*} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				json = new JSONObject()
						.put("error", e.getMessage())
						.put("stackTrace", formatStackTrace(e))
						.put("requestParams", "" + request.getParameterMap().keySet())
						.put("jsonRequest", jsonRequest.toString())
						.put("queryString", request.getQueryString());
				e.printStackTrace();*/
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
		public final String entity;
		public String key;
		public String action;

		public EntityKey(String entity, String key) {
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
			entityName = services.containsKey(entityName) ? entityName : part;
			entityGroups.add(new EntityKey(entityName, (i + 1 < parts.length) ? parts[++i] : null));
		}
		entityGroups.get(entityGroups.size() - 1).action = customAction;
		return entityGroups;
	}

	private JSONService getService(String entity) throws ServletException {
		JSONService service = services.get(entity);
		if (service == null) {
			throw new ServletException("Unknown service: " + entity);
		}
		return service;
	}

	private Pair<EntityKey, JSONObject> parseRequest(HttpServletRequest request) throws ServletException {
		List<EntityKey> entitykeys = getEntitykeys(request);
		String payload = request.getParameter("json");
		try {
			EntityKey entity = entitykeys.get(entitykeys.size() - 1);
			JSONObject json = payload != null ? new JSONObject(payload) : new JSONObject();
			json.put("id", entity.key);
			for (int i = 0; i < entitykeys.size() - 1; i++) {
				EntityKey entitykey = entitykeys.get(i);
				String column = entitykey.entity + (services.containsKey(entitykey.entity) ? "_id" : "");
				json.put(column, entitykey.key);
			}

			JSONArray parameters = json.has("filter") ? json.getJSONArray("filter") : new JSONArray();
			for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
				if (!entry.getKey().equals("json") && !entry.getKey().equals("idToken")) {
					for (String value : entry.getValue()) {
						int pos = 0;
						while (value.charAt(pos) >= '<' && value.charAt(pos) <= '>') {
							pos++;
						}
						String col = entry.getKey();
						parameters.put(new JSONObject()
								.put("column", col.endsWith("_1") || col.endsWith("_2") ? col.substring(0, col.length() - 2) : col)
								.put("operan", pos > 0 ? value.substring(0, pos) : "=")
								.put("value", value.substring(pos)));
					}
				}
			}
			if (parameters.length() > 0) {
				json.put("filter", parameters);
			}
			return Pair.of(entity, json);
		} catch (JSONException e) {
			throw new ServletException(e);
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
	
	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response, int level) throws Exception {
		User user = userProvider.get();
		if (user == null || user.getId() == 0) {
			if (autoLoginId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}
			user = userStore.get(Long.valueOf(autoLoginId));
			if (user == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}
			((IdTokenResolverDD4Impl) idTokenResolver).put(userStore.updateLastLogin(user));
			userProvider.set(user);
		}
		if (user.getTypeId() > level) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		return true;
	}

	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return checkLogin(request, response, 4);
	}
	
	public boolean checkAdminLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return checkLogin(request, response, 1);
	}
}
