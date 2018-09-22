package com.digitald4.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.server.UserService.UserJSONService;
import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
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
		SingleProtoService<GeneralData> protoService = new SingleProtoService<>(mockStore);
		JSONService jsonService = new JSONServiceImpl<>(GeneralData.class, protoService, false);

		protoService.create(GeneralData.newBuilder()
				.setName("Test")
				.setId(1)
				.build());

		jsonService.performAction("create",
				new JSONObject("{\"id\":1,\"name\":\"test 1\"}"));

		jsonService.performAction("create",
				new JSONObject("{\"id\":2,\"name\":\"test 2\"}"));

		jsonService.performAction("create",
				new JSONObject("{\"id\":3,\"name\":\"test 3\"}"));
	}

	@Test
	public void testUpdate() {
		DAOTestingImpl dao = new DAOTestingImpl();
		GeneralDataService protoService = new GeneralDataService(new GeneralDataStore(() -> dao));
		JSONService jsonService = new JSONServiceImpl<>(GeneralData.class, protoService, false);

		GeneralData gd = protoService.create(GeneralData.newBuilder()
				.setName("Test")
				.build());
		assertTrue(gd.getId() > 0);
		assertEquals("Test", gd.getName());

		gd = protoService.get(gd.getId());
		assertTrue(gd.getId() > 0);
		assertEquals("Test", gd.getName());

		gd = protoService.update(
				gd.getId(),
				UpdateRequest.newBuilder()
						.setEntity(Any.pack(GeneralData.newBuilder().setName("Test2").build()))
						.setUpdateMask(FieldMask.newBuilder().addPaths("name"))
						.build());
		assertTrue(gd.getId() > 0);
		assertEquals("Test2", gd.getName());

		gd = ProtoUtil.toProto(GeneralData.getDefaultInstance(), jsonService.performAction("update",
				new JSONObject("{\"id\":" + gd.getId() + ",\"entity\":{\"name\":\"test 3\"},\"updateMask\":\"name\"}}")));

		assertTrue(gd.getId() > 0);
		assertEquals("test 3", gd.getName());
	}

	@Test
	public void testCreateUser() {
		when(mockUserStore.getType()).thenReturn(User.getDefaultInstance());
		when(mockUserStore.create(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
		UserService userService = new UserService(mockUserStore, null, null);
		UserJSONService jsonService = new UserJSONService(userService);

		userService.create(User.newBuilder()
				.setEmail("user@test.com")
				.setFullName("Test User")
				.setId(1)
				.build());

		jsonService.performAction("create",
				new JSONObject("{\"id\":1,\"fullName\":\"Test User\"}"));

		jsonService.performAction("create",
				new JSONObject("{\"id\":2,\"fullName\":\"test 2\"}"));

		jsonService.performAction("create",
				new JSONObject("{\"id\":3,\"fullName\":\"test 3\"}"));
	}
}
