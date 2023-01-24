package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.DataFile;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.json.JSONObject;

/**
 * Service for handling the uploading, retrieving, replacing and deleting of files.
 */
@Api(
		name = "file",
		version = "v1",
		namespace =
		@ApiNamespace(
				ownerDomain = "nbastats.digitald4.com",
				ownerName = "nbastats.digitald4.com"
		),
		// [START_EXCLUDE]
		issuers = {
				@ApiIssuer(
						name = "firebase",
						issuer = "https://securetoken.google.com/fantasy-predictor",
						jwksUri =
								"https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system"
										+ ".gserviceaccount.com"
				)
		}
		// [END_EXCLUDE]
)
public class FileService<U extends User> extends EntityServiceImpl<DataFile, Long> implements JSONService {
	private static final Logger LOGGER = Logger.getLogger(FileService.class.getCanonicalName());

	private final Store<DataFile, Long> dataFileStore;
	private final Provider<HttpServletRequest> requestProvider;
	private final Provider<HttpServletResponse> responseProvider;

	@Inject
	public FileService(
			Store<DataFile, Long> dataFileStore, SessionStore<U> sessionStore, Provider<HttpServletRequest> requestProvider,
			Provider<HttpServletResponse> responseProvider) {
		super(dataFileStore, sessionStore, true);
		this.dataFileStore = dataFileStore;
		this.requestProvider = requestProvider;
		this.responseProvider = responseProvider;
	}

	public JSONObject create() {
		try {
			return toJSON.apply(dataFileStore.create(converter.apply(requestProvider.get().getPart("file"))));
		} catch (ServletException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public JSONObject get(JSONObject request) {
		DataFile df = dataFileStore.get((long) request.getInt("id"));
		HttpServletResponse response = responseProvider.get();
		byte[] bytes = df.getData();
		response.setContentType("application/" + (!df.getType().isEmpty() ? df.getType() : "pdf"));
		response.setHeader("Cache-Control", "no-cache, must-revalidate");
		response.setContentLength(bytes.length);
		try {
			response.getOutputStream().write(bytes);
		} catch (IOException ioe) {
			throw new DD4StorageException("Error fetching file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	public JSONObject update() {
		HttpServletRequest request = requestProvider.get();
		String[] urlParts = request.getRequestURL().toString().split("/");
		long id = Long.parseLong(urlParts[urlParts.length - 1]);
		try {
			DataFile replacement = converter.apply(request.getPart("file"));
			return toJSON.apply(dataFileStore.update(id, dataFile -> replacement));
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
			return new DataFile()
					.setName(fileName)
					.setType(fileName.substring(fileName.length() - 4))
					.setSize(data.length)
					.setData(data);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", e.getMessage());
			throw new RuntimeException("You either did not specify a file to upload or are "
					+ "trying to upload a file to a protected or nonexistent "
					+ "location. " + e.getMessage(), e);
		}
	};

	private static final Function<DataFile, JSONObject> toJSON = dataFile -> new JSONObject(dataFile.setData(null));

	@Override
	public JSONObject performAction(String action, JSONObject request) {
		switch (action) {
			case "create": return create();
			case "update": return update();
			case "delete":
				case "list": // super.performAction(action, request);
			case "get":
				default:
					return get(request);
		}
	}

	@Override
	public boolean requiresLogin(String action) {
		return false;
	}
}
