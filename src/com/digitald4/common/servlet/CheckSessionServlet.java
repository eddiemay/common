package com.digitald4.common.servlet;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import com.digitald4.common.model.User;

@WebServlet(name = "CheckSessionServlet", urlPatterns = {"/checkSession"})
public class CheckSessionServlet extends ParentServlet {
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(-5000);
		User user = (User)session.getAttribute("user");
		JSONObject json = new JSONObject();
		try {
			json.put("valid", user != null && user.getId() != null);
			response.setContentType("application/json");
			response.setHeader("Cache-Control", "no-cache, must-revalidate");
			response.getWriter().println(json);
		} catch (JSONException e) {
			throw new ServletException(e);
		}
	}
}
