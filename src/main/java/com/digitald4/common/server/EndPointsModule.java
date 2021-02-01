package com.digitald4.common.server;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.proto.DD4Protos.ActiveSession;
import com.digitald4.common.proto.DD4Protos.DataFile;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import com.google.api.control.ServiceManagementConfigFilter;
import com.google.api.control.extensions.appengine.GoogleAppEngineControlFilter;
import com.google.api.server.spi.guice.EndpointsModule;
import com.google.inject.TypeLiteral;
import com.google.protobuf.Message;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Provider;
import javax.inject.Singleton;

public abstract class EndPointsModule extends EndpointsModule {
	protected static final String API_URL_PATTERN = "/_ah/api/*";

	protected final DAO<Message> dao = new DAOCloudDS();
	protected final Provider<DAO<Message>> daoProvider = () -> dao;
	private final ProviderThreadLocalImpl<User> userProvider = new ProviderThreadLocalImpl<>();

	@Override
	public void configureServlets() {
		super.configureServlets();

		bind(ServiceManagementConfigFilter.class).in(Singleton.class);
		filter(API_URL_PATTERN).through(ServiceManagementConfigFilter.class);

		Map<String, String> apiController = new HashMap<>();
		apiController.put("endpoints.projectId", getEndPointsProjectId());
		apiController.put("endpoints.serviceName", getEndPointsProjectId() + ".appspot.com");

		bind(GoogleAppEngineControlFilter.class).in(Singleton.class);
		filter(API_URL_PATTERN).through(GoogleAppEngineControlFilter.class, apiController);

		bind(Clock.class).toInstance(Clock.systemUTC());
		// bind(IdTokenResolver.class).to(IdTokenResolverDD4Impl.class);
		bind(User.class).toProvider(userProvider);

		bind(new TypeLiteral<DAO<Message>>(){}).toProvider(daoProvider);
		DAO<HasProto> modelDao = new DAOModelWrapper(daoProvider);
		bind(new TypeLiteral<DAO<HasProto>>(){}).toProvider(() -> modelDao);

		bind(new TypeLiteral<Store<ActiveSession>>(){}).toInstance(new ProtoStore<>(ActiveSession.class, daoProvider));
		bind(new TypeLiteral<Store<DataFile>>(){}).toInstance(new ProtoStore<>(DataFile.class, daoProvider));

		//configureEndpoints(API_URL_PATTERN,
			//	ImmutableList.of(FileService.class, GeneralDataService.class, UserService.class));
	}

	protected abstract String getEndPointsProjectId();
}
