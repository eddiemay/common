package com.digitald4.common.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.digitald4.common.dao.DataAccessObject;
import com.digitald4.common.model.User;

public class UserServlet extends ParentServlet {
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (!checkLoginAutoRedirect(request, response)) return;
			User activeUser = (User)request.getSession(true).getAttribute("user");
			String id = request.getParameter("id");
			User user;
			if (id != null) {
				user = User.getInstance(getEntityManager(), Integer.parseInt(id));
			} else {
				user = activeUser;
			}
			if (activeUser != user && !checkAdminLogin(request, response)) return;
			forward2JSP(request, response, user);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	private void forward2JSP(HttpServletRequest request, HttpServletResponse response, User user) throws Exception {
		request.setAttribute("user", user);
		getLayoutPage(request, "/WEB-INF/jsp/user.jsp").forward(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (!checkLoginAutoRedirect(request, response)) return;
			User activeUser = (User)request.getSession(true).getAttribute("user");
			User user = User.getInstance(getEntityManager(), Integer.parseInt(request.getParameter("id")));
			if (activeUser != user && !checkAdminLogin(request, response)) return;
			String password = request.getParameter("password");
			String passConf = request.getParameter("passConf");
			if (!DataAccessObject.isSame(password, passConf)) {
				request.setAttribute("error", "Passwords don't match");
			} else {
				if (!DataAccessObject.isNull(password)) {
					user.setUserPassword(password);
				}
				user.save();
			}
			forward2JSP(request, response, user);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
