package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.model.DataFile;
import com.digitald4.common.model.FileReference;
import com.digitald4.common.model.User;
import com.digitald4.common.server.ApiServiceServlet.ServerType;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.PasswordStore;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.SearchIndexerAppEngineImpl;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.Store;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.json.JSONObject;

/**
 * A servlet for uploading and fetching files.
 * With @WebServlet annotation the webapp/WEB-INF/web.xml is no longer required.
 * Use the following annotations in the subclass.
 * - @WebServlet(name = "files", description = "Handle file related requests", urlPatterns = "/files/*")
 * - @MultipartConfig(fileSizeThreshold=1024*1024*10, maxFileSize=1024*1024*32, maxRequestSize=1024*1024*32)
 */
public class FileServlet extends HttpServlet {
  private static final Logger LOGGER = Logger.getLogger(FileServlet.class.getCanonicalName());

  private final Clock clock = Clock.systemUTC();
  private DAO dao;
  protected final Provider<DAO> daoProvider = () -> dao;
  private final Store<DataFile, Long> dataFileStore;
  private final ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();
  protected UserStore userStore;
  private LoginResolver loginResolver;

  @Inject
  public FileServlet() {
    dataFileStore = new GenericStore<>(DataFile.class, daoProvider);
  }

  @Override
  public void init() throws ServletException {
    super.init();
    PasswordStore passwordStore = new PasswordStore(daoProvider);
    loginResolver = new SessionStore<>(
        daoProvider, userStore, passwordStore, userProvider, Duration.ofMinutes(30), false, clock);
    ServletContext sc = getServletContext();
    ServerType serverType =
        sc.getServerInfo().contains("Tomcat") ? ServerType.TOMCAT : ServerType.APPENGINE;
    if (serverType == ServerType.TOMCAT) {
      // We use MySQL with Tomcat, so if Tomcat, MySQL
      dao = new DAOSQLImpl(
          new DBConnectorThreadPoolImpl(
              sc.getInitParameter("dbdriver"),
              sc.getInitParameter("dburl"),
              sc.getInitParameter("dbuser"),
              sc.getInitParameter("dbpass")),
          new ChangeTracker(daoProvider, userProvider, null, null, clock),
          true);
    } else {
      // We use CloudDataStore with AppEngine.
      SearchIndexer searchIndexer = new SearchIndexerAppEngineImpl();
      ChangeTracker changeTracker =
          new ChangeTracker(daoProvider, userProvider, null, searchIndexer, clock);
      dao = new DAOCloudDS(
          DatastoreServiceFactory.getDatastoreService(), changeTracker, searchIndexer, () -> DAOCloudDS.Context.NONE);
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    String idToken = request.getParameter("idToken");
    Long id = Long.parseLong(request.getParameter("id"));
    try {
      resolveLogin(idToken, "getFileContents");
      DataFile df = dataFileStore.get(id);
      byte[] bytes = df.getData();
      response.setContentType("application/" + (!df.getType().isEmpty() ? df.getType() : "pdf"));
      response.setHeader("Cache-Control", "no-cache, must-revalidate");
      response.setContentLength(bytes.length);
      response.getOutputStream().write(bytes);
    } catch (DD4StorageException | IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    try {
      String idToken = request.getParameter("idToken");
      resolveLogin(idToken, "upload");
      FileReference reference = FileReference.of(dataFileStore.create(converter.apply(request)));
      postUpload(request, reference);
      String json = new JSONObject(reference).toString();
      response.setContentType("application/json");
      response.setHeader("Cache-Control", "no-cache, must-revalidate");
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.setContentLength(json.length());
      response.getOutputStream().write(json.getBytes());
    } catch (DD4StorageException | IOException e) {
      throw new ServletException(e);
    }
  }

  protected void postUpload(HttpServletRequest request, FileReference reference) {
  }

  protected String getFileName(HttpServletRequest request) throws ServletException, IOException {
    return Arrays.stream(request.getPart("file").getHeader("content-disposition").split(";"))
        .map(String::trim)
        .filter(content -> content.startsWith("filename"))
        .map(content -> content.substring(content.indexOf('=') + 1).trim().replace("\"", ""))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No filename"));
  }

  private final Function<HttpServletRequest, DataFile> converter = request -> {
    Part filePart;
    String fileName;
    try {
      filePart = request.getPart("file");
      if (filePart == null) {
        throw new RuntimeException("Part is null");
      }
      fileName = getFileName(request);
    } catch (IOException | ServletException e) {
      throw new RuntimeException("Error reading file part", e);
    }
    try (InputStream filecontent = filePart.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

      // Create path components to save the file
      int read;
      final byte[] bytes = new byte[1024];

      while ((read = filecontent.read(bytes)) != -1) {
        buffer.write(bytes, 0, read);
      }
      // LOGGER.log(Level.INFO, "File {0} being uploaded.", fileName);
      byte[] data = buffer.toByteArray();
      return new DataFile()
          .setName(fileName)
          .setType(fileName.substring(fileName.lastIndexOf('.') + 1))
          .setData(data);
    } catch (IOException e) {
      e.printStackTrace();
      LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", e.getMessage());
      throw new RuntimeException("You either did not specify a file to upload or are trying to "
          + "upload a file to a protected or nonexistent location. " + e.getMessage(), e);
    }
  };

  protected boolean requiresLogin(String method) {
    return true;
  }

  protected void resolveLogin(String idToken, boolean requiresLogin) {
    loginResolver.resolve(idToken, requiresLogin);
  }

  protected void resolveLogin(String idToken) {
    loginResolver.resolve(idToken, true);
  }

  protected void resolveLogin(String idToken, String method) {
    resolveLogin(idToken, requiresLogin(method));
  }
}
