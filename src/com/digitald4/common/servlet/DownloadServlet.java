package com.digitald4.common.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.digitald4.common.model.FileAttachable;

@WebServlet(name = "DownloadServlet", urlPatterns = {"/download"})
public class DownloadServlet extends ParentServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (!checkLoginAutoRedirect(request, response)) return;
			String className = request.getParameter("classname");
			int id = Integer.parseInt(request.getParameter("id"));
			Class<?> c = Class.forName(className);
			FileAttachable fa = (FileAttachable)c.getMethod("getInstance", Integer.class).invoke(null, id);
			byte[] bytes = fa.getDataFile().getData();
			//response.setContentType("application/pdf");
			response.setHeader("Cache-Control", "no-cache, must-revalidate");
			response.setContentLength(bytes.length);
			response.getOutputStream().write(bytes);
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		doGet(request, response);
	}
}
