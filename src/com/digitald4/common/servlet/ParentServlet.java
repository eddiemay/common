package com.digitald4.common.servlet;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.digitald4.common.jpa.EntityManagerHelper;
import com.digitald4.common.model.GenData;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.User;
import com.digitald4.common.util.Emailer;

public class ParentServlet extends HttpServlet {
	private static Emailer emailer;
	private static EntityManager em;
	private RequestDispatcher layoutPage;
	
	public void init() throws ServletException {
		layoutPage = getServletContext().getRequestDispatcher(getLayoutURL());
		if (layoutPage == null) {
			throw new ServletException(getLayoutURL() + " not found");
		}
	}
	// 951-220-0116 Shanel
	public EntityManager getEntityManager() throws ServletException {
		if (em == null) {
			synchronized (this) {
				if (em == null) {
					ServletContext sc = getServletContext();
					try {
						System.out.println("*********** Loading driver");
						em = EntityManagerHelper.getEntityManagerFactory(
								sc.getInitParameter("dbdriver"), 
								sc.getInitParameter("dburl"), 
								sc.getInitParameter("dbuser"), 
								sc.getInitParameter("dbpass")).createEntityManager();
					} catch(Exception e) {
						System.out.println("******************* error init entity manager **********************");
						throw new ServletException(e);
					}
				}
			}
		}
		return em;
	}
	
	public static boolean isAjax(HttpServletRequest request) {
		// 909-565-8067
		// 951-268-4644 Mark
		// 909-478-4355 Pa
		return "xmlhttprequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
	}
	
	public Emailer getEmailer() {
		if (emailer == null) {
			synchronized (this) {
				if (emailer == null) {
					ServletContext sc = getServletContext();
					emailer = new Emailer(sc.getInitParameter("emailserver"),
							sc.getInitParameter("emailuser"), sc.getInitParameter("emailpass"));
				}
			}
		}
		return emailer;
	}
	
	public RequestDispatcher getLayoutPage(HttpServletRequest request, String pageURL) {
		if (isAjax(request)) {
			return getServletContext().getRequestDispatcher(pageURL);
		}
		request.setAttribute("body", pageURL);
		return layoutPage;
	}
	
	public String getLayoutURL() {
		return "/WEB-INF/jsp/layout.jsp";
	}
	
	protected void goBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(true);
		String backPage = (String) session.getAttribute("backPage");
		if (backPage != null) {
			session.removeAttribute("backPage");
			response.sendRedirect(backPage);
		} else {
			response.sendRedirect("home");
		}
	}
	
	public boolean checkLogin(HttpSession session) throws Exception {
		User user = (User)session.getAttribute("user");
		if (user == null || user.getId() == null) {
			String autoLoginId = getServletContext().getInitParameter("auto_login_id");
			if (autoLoginId != null) {
				user = (User)User.getInstance(getEntityManager(), Integer.parseInt(autoLoginId));
				session.setAttribute("user", user);
				User.setActiveUser(user);
				user.setLastLogin().save();
				return true;
			}
			return false;
		}
		User.setActiveUser(user);
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
	
	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response, GeneralData level) throws Exception {
		if (!checkLoginAutoRedirect(request,response)) return false;
		HttpSession session = request.getSession(true);
		if (!((User)session.getAttribute("user")).isOfRank(level)) {
			response.sendRedirect("denied.html");
			return false;
		}
		return true;
	}
	
	public boolean checkAdminLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return checkLogin(request, response, GenData.UserType_Admin.get(getEntityManager()));
	}
	
	public static String formatStackTrace(Exception e) {
		String out = "";
		for (StackTraceElement elem : e.getStackTrace()) {
			out += elem + "\n";
		}
		return out;
	}
}
