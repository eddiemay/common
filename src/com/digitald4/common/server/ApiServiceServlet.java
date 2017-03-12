package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4Protos.User.UserType;
import com.digitald4.common.proto.DD4UIProtos.GeneralDataUI;
import com.digitald4.common.storage.DAOProtoSQLImpl;
import com.digitald4.common.storage.GenericDAOStore;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Emailer;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.ProviderThreadLocalImpl;

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
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ApiServiceServlet extends HttpServlet {
	private DBConnector connector;
	private Map<String, JSONService> services = new HashMap<>();
	protected UserStore userStore;
	protected UserService userService;
	protected ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();
	protected ProviderThreadLocalImpl<HttpServletRequest> requestProvider = new ProviderThreadLocalImpl<>();
	protected ProviderThreadLocalImpl<HttpServletResponse> responseProvider = new ProviderThreadLocalImpl<>();
	private Emailer emailer;
	
	public void init() throws ServletException {
		DBConnector dbConnector = getDBConnector();

		addService("general_data", new DualProtoService<>(GeneralDataUI.class, new GenericDAOStore<>(
				new DAOProtoSQLImpl<>(GeneralData.class, dbConnector, null, "general_data"))));

		userStore = new UserStore(new DAOProtoSQLImpl<>(User.class, dbConnector, "V_USER"));
		addService("user", userService = new UserService(userStore, requestProvider));
	}
	
	public DBConnector getDBConnector() throws ServletException {
		if (connector == null) {
			synchronized (this) {
				if (connector == null) {
					ServletContext sc = getServletContext();
					try {
						System.out.println("*********** Loading driver");
						connector = new DBConnectorThreadPoolImpl(
								sc.getInitParameter("dbdriver"), 
								sc.getInitParameter("dburl"), 
								sc.getInitParameter("dbuser"), 
								sc.getInitParameter("dbpass"));
					} catch(Exception e) {
						System.out.println("****************** error connecting to database ****************");
						throw new ServletException(e);
					}
				}
			}
		}
		return connector;
	}
	
	public static boolean isAjax(HttpServletRequest request) {
		return "xmlhttprequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
	}

	protected ApiServiceServlet addService(String entity, JSONService service) {
		services.put(entity, service);
		return this;
	}

	protected void processRequest(String entity, String action, JSONObject jsonRequest, HttpServletRequest request, HttpServletResponse response) throws ServletException {
		requestProvider.set(request);
		responseProvider.set(response);
		try {
			Object json = null;
			try {
				JSONService service = services.get(entity);
				if (service == null) {
					throw new DD4StorageException("Unknown service: " + entity);
				}
				if (service.requiresLogin(action) && !checkLogin(request, response)) return;
				json = service.performAction(action, jsonRequest);
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				json = new JSONObject()
						.put("error", e.getMessage())
						.put("stackTrace", formatStackTrace(e))
						.put("requestParams", "" + request.getParameterMap().keySet())
						.put("jsonRequest", jsonRequest.toString())
						.put("queryString", request.getQueryString());
				e.printStackTrace();
			} finally {
				response.setContentType("application/json");
				response.setHeader("Cache-Control", "no-cache, must-revalidate");
				response.getWriter().println(json);
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
		String action = entityKey.action != null ? pair.getLeft().action : "delete";
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
			/entity/action
			/entity/id/action

			/pentity/id/entity
			/pentity/id/entity/id
			/pentity/id/entity/action
			/pentity/id/entity/id/action

			/pentity/id/pentity/id/entity
			/pentity/id/pentity/id/entity/id
			/pentity/id/pentity/id/entity/action
			/pentity/id/pentity/id/entity/id/action
		 */
		List<EntityKey> entityGroups = new ArrayList<>();
		String url = request.getRequestURL().toString();
		String subUrl = url.substring(url.indexOf("/api/") + 5);
		String[] parts = subUrl.split("/");
		int i;
		for (i = 0; i < parts.length - 1; i++) {
			String part = parts[i];
			String entityName = part.substring(0, part.length() -1);
			entityName = services.containsKey(entityName) ? entityName : part;
			entityGroups.add(new EntityKey(entityName, (i + 1 < parts.length - 1) ? parts[++i] : null));
		}

		// Now figure out if the last part is an entity, id or action.
		String lastPart = parts[parts.length - 1];
		if (services.containsKey(lastPart.substring(0, lastPart.length() -1))) {
			entityGroups.add(new EntityKey(lastPart.substring(0, lastPart.length() -1), null));
		} else if (lastPart.charAt(0) >= '1' && lastPart.charAt(0) <= '9') {
			entityGroups.get(entityGroups.size() - 1).key = lastPart;
		} else {
			entityGroups.get(entityGroups.size() - 1).action = lastPart;
		}
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

			JSONArray parameters = json.has("query_param") ? json.getJSONArray("query_param") : new JSONArray();
			for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
				if (!entry.getKey().equals("json")) {
					for (String value : entry.getValue()) {
						int pos = 0;
						while (value.charAt(pos) >= '<' && value.charAt(pos) <= '>') {
							pos++;
						}
						parameters.put(new JSONObject()
								.put("column", entry.getKey())
								.put("operan", pos > 0 ? value.substring(0, pos) : "=")
								.put("value", value.substring(pos)));
					}
				}
			}
			if (parameters.length() > 0) {
				json.put("query_param", parameters);
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
	
	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response, UserType level) throws Exception {
		HttpSession session = request.getSession(true);
		User user = (User) session.getAttribute("puser");
		if (user == null || user.getId() == 0) {
			String autoLoginId = getServletContext().getInitParameter("auto_login_id");
			if (autoLoginId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}
			user = userStore.get(Integer.parseInt(autoLoginId));
			session.setAttribute("puser", userStore.updateLastLogin(user));
		}
		if (user.getType().getNumber() > level.getNumber()) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		userProvider.set(user);
		return true;
	}

	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return checkLogin(request, response, UserType.STANDARD);
	}
	
	public boolean checkAdminLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return checkLogin(request, response, UserType.ADMIN);
	}
	
	public static String formatStackTrace(Exception e) {
		String out = "";
		for (StackTraceElement elem : e.getStackTrace()) {
			out += elem + "\n";
		}
		return out;
	}
}
