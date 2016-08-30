package com.digitald4.common.servlet;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import com.digitald4.common.model.Company;
import com.digitald4.common.model.User;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends ParentServlet {
	
	private final static String defaultPage = "dashboard";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(true);
		session.setMaxInactiveInterval(-5000);
		User user = (User)session.getAttribute("user");
		if (user != null && user.getId() != null) {
			response.sendRedirect(defaultPage);
			return;
		}
		if (request.getParameter("username") != null && request.getParameter("pass") != null) {
			doPost(request,response);
		} else {
			forward2Jsp(request, response);
		}
	}
	
	public String getLayoutURL(){
		return "/WEB-INF/jsp/login.jsp";
	}
	
	protected void forward2Jsp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (isAjax(request)) {
			JSONObject json = new JSONObject();
			try {
				json.put("valid", false)
					.put("error", request.getAttribute("error"));
				response.setContentType("application/json");
				response.setHeader("Cache-Control", "no-cache, must-revalidate");
				response.getWriter().println(json);
			} catch (JSONException e) {
				throw new ServletException(e);
			}
		} else {
			getLayoutPage(request, "/WEB-INF/jsp/login.jsp").forward(request, response);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();
		String username = request.getParameter("username");
		if (username == null || username.length() == 0) {
			request.setAttribute("error", "Username is required.");
			forward2Jsp(request, response);
			return;
		}
		String passwd = request.getParameter("pass");
		if (passwd == null || passwd.length() == 0) {
			request.setAttribute("error", "password is required.");
			forward2Jsp(request, response);
			return;
		}
		User user;
		try {
			user = User.get(getEntityManager(), username, passwd);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		if (user == null) {
			request.setAttribute("error", "Login incorrect");
			forward2Jsp(request, response);
			return;
		}
		try {
			User.setActiveUser(user);
			session.setAttribute("user", user.setLastLogin().save());
		} catch (Exception e) {
			throw new ServletException(e);
		}
		String redirect = (String)session.getAttribute("redirect");
		if (redirect == null) {
			redirect = defaultPage;
		} else {
			session.removeAttribute("redirect");
		}
		if (isAjax(request)) {
			JSONObject json = new JSONObject();
			try {
				json.put("valid", true)
					.put("redirect", redirect);
				response.setContentType("application/json");
				response.setHeader("Cache-Control", "no-cache, must-revalidate");
				response.getWriter().println(json);
			} catch (JSONException e) {
				throw new ServletException(e);
			}
		} else {
			response.sendRedirect(redirect);
		}
	}
	protected void sendPassword(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String to = request.getParameter("to");
		if(to == null || to.length() == 0)
			to="";
		User user = User.getByEmail(getEntityManager(), to);
		if(user != null){

			String password = "";
			for (int x = 0; x < 6; x++) {
				password += (char) ('a' + Math.random() * 26);
			}

			try {
				user.setUserPassword(password);
			} catch (Exception e) {
				throw new ServletException(e);
			}

			//Send the email

			Company company = Company.get();
			//String subject = company.getWebsite() + ": New Password for " + to;
			String message = "New Password for " + to + " is <b>" + password + "</b><br/><br/>"+
							"Please change your password on the <a href=http://"+company.getWebsite()+"/account>Account Page</a> now.<br/><br/>"+
							"<p>"+
							"Please note: If you have any questions you can contact us via our website.<br>"+
							"Thank You, <a href=http://"+company.getWebsite()+">"+company.getWebsite()+"</a>."+
							"</p>";
			message.trim();
			//String host[] = new String[]{getServletContext().getInitParameter("emailserver"),getServletContext().getInitParameter("emailuser"),getServletContext().getInitParameter("emailpass")};
			//emailer.sendmail(company.getEmail(), user.getEmail(), host, subject, message);
			request.setAttribute("action", "sent");
		}
		else{
			request.setAttribute("action", "cantSend");
		}
		forward2Jsp(request, response);
	}
}
