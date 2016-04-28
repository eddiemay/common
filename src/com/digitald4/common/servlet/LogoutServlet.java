package com.digitald4.common.servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends ParentServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		try {
			request.getSession().invalidate();
			request.setAttribute("message", "You have successfully logged out");
      getLayoutPage(request, "/WEB-INF/jsp/login.jsp").forward(request, response);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response){
		doGet(request,response);
	}
	
	public String getLayoutURL(){
		return "/WEB-INF/jsp/login.jsp";
	}
}
