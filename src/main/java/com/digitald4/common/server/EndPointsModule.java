package com.digitald4.common.server;

import com.digitald4.common.model.*;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.DAO.Context;
import com.google.api.control.ServiceManagementConfigFilter;
import com.google.api.control.extensions.appengine.GoogleAppEngineControlFilter;
import com.google.api.server.spi.guice.EndpointsModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import java.time.Clock;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

public class EndPointsModule extends EndpointsModule {
	private static final String DEFAULT_API_URL_PATTERN = "/_api/*";

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

		ImmutableMap<String, String> apiController = ImmutableMap.of(
				"endpoints.projectId", getEndPointsProjectId(),
				"endpoints.serviceName", getEndPointsProjectId() + ".appspot.com");
		bind(GoogleAppEngineControlFilter.class).in(Singleton.class);
		filter(getApiUrlPattern()).through(GoogleAppEngineControlFilter.class, apiController);

		bind(Clock.class).toInstance(Clock.systemUTC());

		bind(DAO.class).to(DAOAppEngineDatastore.class);

		bind(new TypeLiteral<Store<DataFile, String>>(){}).to(new TypeLiteral<GenericStore<DataFile, String>>(){});
		// bind(new TypeLiteral<Store<Flag, String>>(){}).to(new TypeLiteral<GenericStore<Flag, String>>(){});
	}

	@Provides @Singleton
	public static Context contextProvider(Provider<HttpServletRequest> requestProvider) {
		return requestProvider.get().getServerName().startsWith("test") ? Context.TEST : Context.NONE;
	}
}
