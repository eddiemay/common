package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.DataFile;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.storage.GenericDAOStore;
import com.digitald4.common.util.Provider;
import com.google.protobuf.ByteString;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Service for handling the uploading, retrieving, replacing and deleting of files.
 */
public class FileService implements JSONService {
	private static final Logger LOGGER = Logger.getLogger(FileService.class.getCanonicalName());

	private final GenericDAOStore<DataFile> dataFileStore;
	private final Provider<HttpServletRequest> requestProvider;
	private final Provider<HttpServletResponse> responseProvider;

	public FileService(GenericDAOStore<DataFile> dataFileStore, Provider<HttpServletRequest> requestProvider,
										 Provider<HttpServletResponse> responseProvider) {
		this.dataFileStore = dataFileStore;
		this.requestProvider = requestProvider;
		this.responseProvider = responseProvider;
	}

	private static final Function<Part, DataFile> converter = new Function<Part, DataFile>() {
		@Override
		public DataFile apply(Part filePart) {
			try (InputStream filecontent = filePart.getInputStream();
					 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

				// Create path components to save the file
				int read;
				final byte[] bytes = new byte[1024];

				while ((read = filecontent.read(bytes)) != -1) {
					buffer.write(bytes, 0, read);
				}
				byte[] data = buffer.toByteArray();
				DataFile df = DataFile.newBuilder()
						.setName(getFileName(filePart))
						.setType(DataFile.FileType.MISC)
						.setSize(data.length)
						.setData(ByteString.copyFrom(data))
						.build();
				LOGGER.log(Level.INFO, "File {0} being uploaded.", df.getName());
				return df;
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}",
						new Object[]{e.getMessage()});
				throw new RuntimeException("You either did not specify a file to upload or are "
						+ "trying to upload a file to a protected or nonexistent "
						+ "location.", e);
			}
		}
	};

	public DataFile create(HttpServletRequest request) throws DD4StorageException, IOException, ServletException {
		return dataFileStore.create(converter.apply(request.getPart("file")));
	}

	public DataFile get(GetRequest request) throws DD4StorageException {
		return dataFileStore.get(request.getId());
	}

	public DataFile update(HttpServletRequest request) throws DD4StorageException, IOException, ServletException {
		String[] urlParts = request.getRequestURL().toString().split("/");
		int id = Integer.parseInt(urlParts[urlParts.length - 1]);
		DataFile replacement = converter.apply(request.getPart("file"));

		return dataFileStore.update(id, new UnaryOperator<DataFile>() {
			@Override
			public DataFile apply(DataFile dataFile) {
				return dataFile.toBuilder()
						.mergeFrom(replacement)
						.build();
			}
		});
	}

	public boolean delete(DeleteRequest request) throws DD4StorageException {
		return dataFileStore.delete(request.getId());
	}

	private static String getFileName(final Part part) {
		final String partHeader = part.getHeader("content-disposition");
		LOGGER.log(Level.INFO, "Part Header = {0}", partHeader);
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}


	@Override
	public boolean requiresLogin(String action) {
		return true;
	}

	@Override
	public Object performAction(String action, JSONObject json) throws Exception {
		switch (action) {
			case "create":
				return create(requestProvider.get());
			case "get":
				DataFile df = get(JSONService.transformJSONRequest(GetRequest.getDefaultInstance(), json));
				HttpServletResponse response = responseProvider.get();
				byte[] bytes = df.getData().toByteArray();
				response.setContentType("application/" + df.getType());
				response.setHeader("Cache-Control", "no-cache, must-revalidate");
				response.setContentLength(bytes.length);
				response.getOutputStream().write(bytes);
				return null;
			case "update":
				return update(requestProvider.get());
			case "delete":
				return JSONService.convertToJSON(delete(
						JSONService.transformJSONRequest(DeleteRequest.getDefaultInstance(), json)));
		}
		return null;
	}
}
