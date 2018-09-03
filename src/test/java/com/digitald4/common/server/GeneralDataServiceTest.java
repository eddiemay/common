package com.digitald4.common.server;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.UserStore;
import com.google.protobuf.Any;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mock;

public class GeneralDataServiceTest {
	@Mock private GeneralDataStore mockStore = mock(GeneralDataStore.class);
	@Mock private UserStore mockUserStore = mock(UserStore.class);

	@Test
	public void testCreate() {
		when(mockStore.getType()).thenReturn(GeneralData.getDefaultInstance());
		when(mockStore.create(any(GeneralData.class))).thenAnswer(i -> i.getArguments()[0]);
		SingleProtoService service = new SingleProtoService<>(mockStore);

		service.create(CreateRequest.newBuilder()
				.setEntity(Any.pack(GeneralData.newBuilder()
						.setName("Test")
						.setId(1)
						.build()))
				.build());

		service.performAction("create", new JSONObject()
				.put("entity",
						new JSONObject("{\"id\":1,\"name\":\"test 1\"}")));

		service.performAction("create", new JSONObject()
				.put("entity",
						new JSONObject("{\"id\":2,\"name\":\"test 2\"}")));

		service.performAction("create", new JSONObject()
				.put("entity",
						new JSONObject("{\"id\":3,\"name\":\"test 3\"}")));
	}

	@Test
	public void testCreateUser() {
		when(mockUserStore.getType()).thenReturn(User.getDefaultInstance());
		when(mockUserStore.create(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
		UserService service = new UserService(mockUserStore, null, null);

		service.create(CreateRequest.newBuilder()
				.setEntity(Any.pack(User.newBuilder()
						.setEmail("user@test.com")
						.setFullName("Test User")
						.setId(1)
						.build()))
				.build());

		service.performAction("create", new JSONObject()
				.put("entity",
						new JSONObject("{\"id\":1,\"fullName\":\"Test User\"}")));

		service.performAction("create", new JSONObject()
				.put("entity",
						new JSONObject("{\"id\":2,\"fullName\":\"test 2\"}")));

		service.performAction("create", new JSONObject()
				.put("entity",
						new JSONObject("{\"id\":3,\"fullName\":\"test 3\"}")));
	}
}
