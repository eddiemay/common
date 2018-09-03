package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.DataFile;
import com.digitald4.common.proto.DD4UIProtos;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.common.util.Provider;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.json.JSONObject;

/**
 * Service for handling the uploading, retrieving, replacing and deleting of files.
 */
public class FileService extends DualProtoService<DD4UIProtos.DataFile, DataFile> {
	private static final Logger LOGGER = Logger.getLogger(FileService.class.getCanonicalName());

	private final Store<DataFile> dataFileStore;
	private final Provider<HttpServletRequest> requestProvider;
	private final Provider<HttpServletResponse> responseProvider;

	FileService(Store<DataFile> dataFileStore, Provider<HttpServletRequest> requestProvider,
							Provider<HttpServletResponse> responseProvider) {
		super(DD4UIProtos.DataFile.class, dataFileStore);
		this.dataFileStore = dataFileStore;
		this.requestProvider = requestProvider;
		this.responseProvider = responseProvider;
	}

	protected JSONObject create() {
		try {
			return toJSON.apply(dataFileStore.create(converter.apply(requestProvider.get().getPart("file"))));
		} catch (ServletException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public JSONObject get(JSONObject request) {
		DataFile df = dataFileStore.get(request.getInt("id"));
		HttpServletResponse response = responseProvider.get();
		byte[] bytes = df.getData().toByteArray();
		response.setContentType("application/" + (!df.getType().isEmpty() ? df.getType() : "pdf"));
		response.setHeader("Cache-Control", "no-cache, must-revalidate");
		response.setContentLength(bytes.length);
		try {
			response.getOutputStream().write(bytes);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		return null;
	}

	public JSONObject update() {
		HttpServletRequest request = requestProvider.get();
		String[] urlParts = request.getRequestURL().toString().split("/");
		int id = Integer.parseInt(urlParts[urlParts.length - 1]);
		try {
			DataFile replacement = converter.apply(request.getPart("file"));
			return toJSON.apply(dataFileStore.update(id, dataFile -> dataFile.toBuilder()
					.mergeFrom(replacement)
					.build())
			);
		} catch (ServletException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getFileName(final Part part) {
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		throw new RuntimeException("No filename");
	}

	private static final Function<Part, DataFile> converter = filePart -> {
		if (filePart == null) {
			throw new RuntimeException("Part is null");
		}
		try (InputStream filecontent = filePart.getInputStream();
				 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

			// Create path components to save the file
			int read;
			final byte[] bytes = new byte[1024];

			while ((read = filecontent.read(bytes)) != -1) {
				buffer.write(bytes, 0, read);
			}
			String fileName = getFileName(filePart);
			// LOGGER.log(Level.INFO, "File {0} being uploaded.", fileName);
			byte[] data = buffer.toByteArray();
			return DataFile.newBuilder()
					.setName(fileName)
					.setType(fileName.substring(fileName.length() - 4))
					.setSize(data.length)
					.setData(ByteString.copyFrom(data))
					.build();
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", e.getMessage());
			throw new RuntimeException("You either did not specify a file to upload or are "
					+ "trying to upload a file to a protected or nonexistent "
					+ "location. " + e.getMessage(), e);
		}
	};

	private static final Function<DataFile, JSONObject> toJSON = dataFile ->
			new JSONObject(ProtoUtil.print(dataFile.toBuilder().clearData().build()));

	static class FileJSONService extends JSONServiceImpl<DD4UIProtos.DataFile> {
		private final FileService fileService;

		FileJSONService(FileService fileService) {
			super(DD4UIProtos.DataFile.class, fileService, true);
			this.fileService = fileService;
		}

		@Override
		public JSONObject performAction(String action, JSONObject request) {
			switch (action) {
				case "create": return fileService.create();
				case "update": return fileService.update();
				case "delete": case "list": super.performAction(action, request);
				case "get": default: return fileService.get(request);
			}
		}
	}
}
