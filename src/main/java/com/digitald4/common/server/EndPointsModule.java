package com.digitald4.common.server;

import com.digitald4.common.model.*;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import com.google.api.control.ServiceManagementConfigFilter;
import com.google.api.control.extensions.appengine.GoogleAppEngineControlFilter;
import com.google.api.server.spi.guice.EndpointsModule;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.inject.TypeLiteral;
import com.google.protobuf.Message;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

public class EndPointsModule extends EndpointsModule {
	private static final String DEFAULT_API_URL_PATTERN = "/_api/*";

	private final ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();
	private final String projectId;
	private final String apiUrlPattern;

	protected EndPointsModule(String projectId, String apiUrlPattern) {
		this.projectId = projectId;
		this.apiUrlPattern = apiUrlPattern;
	}

	protected EndPointsModule(String projectId) {
		this(projectId, DEFAULT_API_URL_PATTERN);
	}

	protected String getEndPointsProjectId() {
		return projectId;
	}

	public String getApiUrlPattern() {
	  return apiUrlPattern;
  }

	@Override
	public void configureServlets() {
		super.configureServlets();

		bind(ServiceManagementConfigFilter.class).in(Singleton.class);
		filter(getApiUrlPattern()).through(ServiceManagementConfigFilter.class);

		Map<String, String> apiController = new HashMap<>();
		apiController.put("endpoints.projectId", getEndPointsProjectId());
		apiController.put("endpoints.serviceName", getEndPointsProjectId() + ".appspot.com");
		bind(GoogleAppEngineControlFilter.class).in(Singleton.class);
		filter(getApiUrlPattern()).through(GoogleAppEngineControlFilter.class, apiController);

		bind(Clock.class).toInstance(Clock.systemUTC());
		bind(User.class).toProvider(userProvider);

		bind(Datastore.class).toInstance(DatastoreOptions.getDefaultInstance().getService());
		bind(DatastoreService.class).toInstance(DatastoreServiceFactory.getDatastoreService());
		bind(new TypeLiteral<TypedDAO<Message>>(){}).to(DAOCloudDSProto.class);
		bind(new TypeLiteral<TypedDAO<HasProto>>(){}).to(DAOHasProto.class);
		bind(DAO.class).annotatedWith(Annotations.DefaultDAO.class).to(DAOCloudDS.class);
		bind(DAO.class).to(DAORouterImpl.class);

		bind(new TypeLiteral<Store<DataFile, Long>>(){}).to(new TypeLiteral<GenericStore<DataFile, Long>>(){});

		//configureEndpoints(getApiUrlPattern(),
			//	ImmutableList.of(FileService.class, GeneralDataService.class, UserService.class));
	}
}
