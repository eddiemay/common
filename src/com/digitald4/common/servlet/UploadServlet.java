package com.digitald4.common.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.digitald4.common.model.DataFile;
import com.digitald4.common.model.FileAttachable;
import com.digitald4.common.model.GenData;

@WebServlet(name = "FileUploadServlet", urlPatterns = {"/upload"})
@MultipartConfig
public class UploadServlet extends ParentServlet {
	
	private final static Logger LOGGER = 
			Logger.getLogger(UploadServlet.class.getCanonicalName());
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}
	 
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		response.setContentType("text/html;charset=UTF-8");

		// Create path components to save the file
		final Part filePart = request.getPart("file");
		final String fileName = getFileName(filePart);

		ByteArrayOutputStream buffer = null;
		InputStream filecontent = null;
		final PrintWriter writer = response.getWriter();

		try {
			if (!checkLogin(request.getSession(true))) {
				throw new Exception("Session Expired");
			}
			String className = request.getParameter("classname");
			int id = Integer.parseInt(request.getParameter("id"));
			Class<?> c = Class.forName(className);
			FileAttachable fa = (FileAttachable)c.getMethod("getInstance", Integer.class).invoke(null, id);
			filecontent = filePart.getInputStream();
			buffer = new ByteArrayOutputStream();
			
			int read = 0;
			final byte[] bytes = new byte[1024];

			while ((read = filecontent.read(bytes)) != -1) {
				buffer.write(bytes, 0, read);
			}
			
			byte[] data = buffer.toByteArray();
			DataFile df = new DataFile(getEntityManager());
			df.setName(fileName);
			df.setType(GenData.FileType_Misc.get(getEntityManager()));
			df.setSize(data.length);
			df.setData(data);
			df.insert();
			fa.setDataFile(df);
			fa.save();
			LOGGER.log(Level.INFO, "File{0}being uploaded.", 
					new Object[]{fileName});
		} catch (Exception e) {
			e.printStackTrace();
			writer.println("You either did not specify a file to upload or are "
					+ "trying to upload a file to a protected or nonexistent "
					+ "location.");
			writer.println("<br/> ERROR: " + e.getMessage());

			LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", 
					new Object[]{e.getMessage()});
		} finally {
			if (filecontent != null) {
				filecontent.close();
			}
			if (buffer != null) {
				buffer.close();
			}
			if (writer != null) {
				writer.close();
			}
		}
	}

	private String getFileName(final Part part) {
		final String partHeader = part.getHeader("content-disposition");
		LOGGER.log(Level.INFO, "Part Header = {0}", partHeader);
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(
						content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}
}
