package com.digitald4.common.servlet;

import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.digitald4.common.dao.DataAccessObject;
import com.digitald4.common.model.GenData;
import com.digitald4.common.model.User;
import com.digitald4.common.servlet.ParentServlet;

@WebServlet(name = "UserAddServlet", urlPatterns = {"/useradd"})
public class UserAddServlet extends ParentServlet {
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (!checkAdminLogin(request, response)) return;
			forward2JSP(request, response, new User(getEntityManager()).setType(GenData.UserType_Standard.get(getEntityManager())));
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}
	
	private void forward2JSP(HttpServletRequest request, HttpServletResponse response, User user) throws Exception {
		request.setAttribute("user", user);
		getLayoutPage(request, "/WEB-INF/jsp/useradd.jsp").forward(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (!checkAdminLogin(request, response)) return;
			User user = new User(getEntityManager()).setType(GenData.UserType_Standard.get(getEntityManager()));
			String paramName = null;
			Enumeration<String> paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				paramName = paramNames.nextElement();
				if (paramName.toLowerCase().startsWith("user.")) {
					Object attr = request.getParameter(paramName);
					user.setPropertyValue(paramName, (String)attr);
				}
			}
			
			String password = request.getParameter("password");
			String passConf = request.getParameter("passConf");
			if (!DataAccessObject.isSame(password, passConf)) {
				request.setAttribute("error", "Passwords don't match");
				forward2JSP(request, response, user);
			} else {
				if (!DataAccessObject.isNull(password)) {
					user.setUserPassword(password);
				}
				user.insert();
				response.sendRedirect("users");
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
