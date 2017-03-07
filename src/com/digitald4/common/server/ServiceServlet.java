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
import com.digitald4.common.util.ProviderThreadLocalImpl;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

public class ServiceServlet extends HttpServlet {
	private DBConnector connector;
	private Map<String, JSONService> services = new HashMap<>();
	protected UserStore userStore;
	protected UserService userService;
	protected ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();
	protected ProviderThreadLocalImpl<HttpServletRequest> requestProvider = new ProviderThreadLocalImpl<>();
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

	protected ServiceServlet addService(String entity, JSONService service) {
		services.put(entity, service);
		return this;
	}

	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		requestProvider.set(request);
		try {
			Object json = null;
			try {
				String[] urlParts = request.getRequestURL().toString().split("/");
				String entity = urlParts[urlParts.length - 2];
				String action = urlParts[urlParts.length - 1];
				JSONService service = services.get(entity);
				if (service == null) {
					throw new DD4StorageException("Unknown service: " + entity);
				}
				if (service.requiresLogin(action) && !checkLogin(request, response)) return;
				json = service.performAction(action, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
			} catch (Exception e) {
				json = new JSONObject()
						.put("error", e.getMessage())
						.put("stackTrace", formatStackTrace(e))
						.put("requestParams", "" + request.getParameterMap().keySet())
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
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		processRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		processRequest(request, response);
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

    if (queryString == null) {
        return requestURL.toString();
    } else {
        return requestURL.append('?').append(queryString).toString();
    }
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
