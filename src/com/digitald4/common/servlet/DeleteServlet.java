package com.digitald4.common.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.digitald4.common.dao.DataAccessObject;

@WebServlet(name = "DeleteServlet", urlPatterns = {"/delete"})
public class DeleteServlet extends ParentServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			JSONObject json = new JSONObject();
			try {
				if (!checkLoginAutoRedirect(request, response)) return;
				String className = request.getParameter("classname");
				int id = Integer.parseInt(request.getParameter("id"));
				Class<?> c = Class.forName(className);
				DataAccessObject dao = (DataAccessObject)c.getMethod("getInstance", Integer.class).invoke(null, id);
				if (dao != null) {
					dao.delete();
				}
				json.put("valid", true);
			} catch (Exception e) {
				json.put("valid", false).put("error", e.getMessage());
				throw e;
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
		doGet(request, response);
	}
}
