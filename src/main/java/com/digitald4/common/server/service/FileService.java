package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.DataFile;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/** Service for handling the uploading, retrieving, replacing and deleting of files. */
@Api(
		name = "files",
		version = "v1",
		namespace = @ApiNamespace(ownerDomain = "dd4common.digitald4.com", ownerName = "dd4common.digitald4.com")
)
@MultipartConfig(fileSizeThreshold=1024*1024*10, maxFileSize=1024*1024*32, maxRequestSize=1024*1024*32)
public class FileService extends EntityServiceImpl<DataFile, String> {
	private static final Logger LOGGER = Logger.getLogger(FileService.class.getCanonicalName());

	protected final Store<DataFile, String> dataFileStore;
	protected final Provider<HttpServletRequest> requestProvider;
	protected final Provider<HttpServletResponse> responseProvider;

	@Inject
	public FileService(Store<DataFile, String> dataFileStore, LoginResolver loginResolver,
			Provider<HttpServletRequest> requestProvider, Provider<HttpServletResponse> responseProvider) {
		super(dataFileStore, loginResolver);
		this.dataFileStore = dataFileStore;
		this.requestProvider = requestProvider;
		this.responseProvider = responseProvider;
	}

	@ApiMethod(httpMethod = HttpMethod.POST, path = "upload")
	public DataFile upload() {
		try {
			HttpServletRequest request = requestProvider.get();
			return dataFileStore.create(converter.apply(request.getPart("file")));
		} catch (ServletException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@ApiMethod(httpMethod = HttpMethod.GET, path = "{fileName}")
	public Empty getFileContents(@Named("fileName") String fileName, @Nullable @Named("idToken") String idToken) {
		try {
			resolveLogin(idToken, "getFileContents");
			DataFile df = dataFileStore.get(fileName);
			HttpServletResponse response = responseProvider.get();
			byte[] bytes = df.getData();
			response.setContentType("application/" + (!df.getType().isEmpty() ? df.getType() : "pdf"));
			response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
			// Cache for the max of 1 year bcz we like to add a new file rather than update files.
			response.setHeader("Cache-Control", "max-age=30");
			response.setContentLength(bytes.length);
			response.getOutputStream().write(bytes);
		} catch (IOException ioe) {
			throw new DD4StorageException("Error fetching file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	/* public DataFile update() {
		HttpServletRequest request = requestProvider.get();
		String[] urlParts = request.getRequestURL().toString().split("/");
		long id = Long.parseLong(urlParts[urlParts.length - 1]);
		try {
			DataFile replacement = converter.apply(request.getPart("file"));
			return dataFileStore.update(id, dataFile -> replacement);
		} catch (ServletException | IOException e) {
			throw new RuntimeException(e);
		}
	} */

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
					.setData(data);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", e.getMessage());
			throw new RuntimeException("You either did not specify a file to upload or are trying to "
					+ "upload a file to a protected or nonexistent location. " + e.getMessage(), e);
		}
	};
}
