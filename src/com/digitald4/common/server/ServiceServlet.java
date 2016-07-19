package com.digitald4.common.server;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.joda.time.DateTime;

import com.digitald4.common.dao.sql.DAOProtoSQLImpl;
import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4Protos.User.UserType;
import com.digitald4.common.store.impl.UserStore;
import com.digitald4.common.util.Emailer;
import com.digitald4.common.util.UserProvider;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.Descriptor;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

public class ServiceServlet extends HttpServlet {
	private DBConnector connector;
	private UserStore userStore;
	protected UserProvider userProvider = new UserProvider();
	private Emailer emailer;
	
	public void init() throws ServletException {
		userStore = new UserStore(new DAOProtoSQLImpl<>(User.getDefaultInstance(), getDBConnector()));
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
	
	public Emailer getEmailer() {
		if (emailer == null) {
			ServletContext sc = getServletContext();
			emailer = new Emailer(sc.getInitParameter("emailserver"),
					sc.getInitParameter("emailuser"), sc.getInitParameter("emailpass"));
		}
		return emailer;
	}
	
	public boolean checkLogin(HttpSession session) throws Exception {
		User user = (User) session.getAttribute("puser");
		if (user == null || user.getId() == 0) {
			String autoLoginId = getServletContext().getInitParameter("auto_login_id");
			if (autoLoginId == null) {
				return false;
			}
			user = userStore.get(Integer.parseInt(autoLoginId));
			session.setAttribute("puser", userStore.updateLastLogin(user));
		}
		userProvider.set(user);
		return true;
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
	
	public boolean checkLoginAutoRedirect(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(true);
		if (!checkLogin(session)) {
			session.setAttribute("redirect", getFullURL(request));
			response.sendRedirect("login");
			return false;
		}
		return true;
	}
	
	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response, UserType level)
			throws Exception {
		if (!checkLoginAutoRedirect(request,response)) return false;
		HttpSession session = request.getSession(true);
		if (((User) session.getAttribute("puser")).getType().getNumber() > level.getNumber()) {
			response.sendRedirect("denied.html");
			return false;
		}
		return true;
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
	
	@SuppressWarnings("unchecked")
	public static <R extends Message> R transformRequest(R msgRequest,
			HttpServletRequest httpRequest) throws ParseException {
		R.Builder builder = msgRequest.toBuilder();
		Descriptor descriptor = builder.getDescriptorForType();
		for (Map.Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()) {
			FieldDescriptor field = descriptor.findFieldByName(entry.getKey());
			builder.setField(field, transformValue(field, entry.getValue()[0]));
		}
		return (R) builder.build();
	}
	
	@SuppressWarnings("unchecked")
	public static <R extends Message> R transformJSONRequest(R msgRequest,
			HttpServletRequest httpRequest) throws ParseException {
		R.Builder builder = msgRequest.toBuilder();
		JsonFormat.merge(httpRequest.getParameterMap().values().iterator().next()[0], builder);
		return (R) builder.build();
	}
	
	@SuppressWarnings("unchecked")
	public static <R extends Message> R transformRequest(R msgRequest, String json)
			throws ParseException {
		R.Builder builder = msgRequest.toBuilder();
		JsonFormat.merge(json, builder);
		return (R) builder.build();
	}
	
	public static Object transformValue(FieldDescriptor field, String value) throws ParseException {
		switch (field.getJavaType()) {
			case BOOLEAN: return Boolean.valueOf(value);
			case INT: return Integer.valueOf(value);
			case LONG: {
				try {
					return Long.valueOf(value);
				} catch (NumberFormatException nfe) {
					// If the value is not a number it must be in date format.
					return DateTime.parse(value).getMillis();
				}
			}
			case FLOAT: return Float.valueOf(value);
			case DOUBLE: return Double.valueOf(value);
			case STRING: return value;
			case ENUM: {
				try {
					return field.getEnumType().findValueByNumber(Integer.valueOf(value));
				} catch (NumberFormatException nfe) {
					return field.getEnumType().findValueByName(value);
				}
			}
			case BYTE_STRING: break;
			case MESSAGE: break;
			default: break;
		}
		return value;
	}
}
