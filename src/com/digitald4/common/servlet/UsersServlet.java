package com.digitald4.common.servlet;

import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;

import com.digitald4.common.component.Column;
import com.digitald4.common.model.User;
import com.digitald4.common.servlet.ParentServlet;
import com.digitald4.common.util.FormatText;

@WebServlet(name = "UsersServlet", urlPatterns = {"/users"})
public class UsersServlet extends ParentServlet {
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (!checkAdminLogin(request, response)) return;
			ArrayList<Column<User>> columns = new ArrayList<Column<User>>();
			columns.add(new Column<User>("Name", "Link", String.class, false) {
				@Override
				public Object getValue(User user) {
					return "<a title=\"" + user + "\" href=\"user?id=" + user.getId() + "\">" + user + "</a>";
				}
			});
			columns.add(new Column<User>("Type", "type", String.class, false));
			columns.add(new Column<User>("Email Address", "email", String.class, false));
			columns.add(new Column<User>("Disabled", "disabled", Boolean.class, false));
			columns.add(new Column<User>("Last Login", "Last Login", DateTime.class, false) {
				@Override
				public Object getValue(User user) {
					return FormatText.formatDate(user.getLastLogin(), FormatText.USER_DATETIME);
				}
			});
			columns.add(new Column<User>("Notes", "notes", String.class, false));
			request.setAttribute("columns", columns);
			request.setAttribute("users", User.getAll(getEntityManager()));
			getLayoutPage(request, "/WEB-INF/jsp/users.jsp" ).forward(request, response);
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		doGet(request, response);
	}
}
