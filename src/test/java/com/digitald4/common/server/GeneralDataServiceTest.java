package com.digitald4.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.UpdateRequest;
import com.digitald4.common.model.User;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.server.service.GeneralDataService;
import com.digitald4.common.server.service.JSONService;
import com.digitald4.common.server.service.JSONServiceImpl;
import com.digitald4.common.server.service.SingleProtoService;
import com.digitald4.common.server.service.UserService;
import com.digitald4.common.server.service.UserService.UserJSONService;
import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.digitald4.common.util.ProtoUtil;
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
		JSONService jsonService = new JSONServiceImpl<>(protoService, false);

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
		JSONService jsonService = new JSONServiceImpl<>(protoService, false);

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
				new UpdateRequest<>(
						GeneralData.newBuilder().setName("Test2").build(),
						FieldMask.newBuilder().addPaths("name").build()));
		assertTrue(gd.getId() > 0);
		assertEquals("Test2", gd.getName());

		gd = ProtoUtil.toProto(GeneralData.getDefaultInstance(), jsonService.performAction("update",
				new JSONObject("{\"id\":" + gd.getId() + ",\"entity\":{\"name\":\"test 3\"},\"updateMask\":\"name\"}}")));

		assertTrue(gd.getId() > 0);
		assertEquals("test 3", gd.getName());
	}

	@Test
	public void testCreateUser() {
		when(mockUserStore.getType()).thenReturn(new BasicUser());
		when(mockUserStore.create(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
		UserService userService = new UserService(mockUserStore, null, null, null);
		UserJSONService jsonService = new UserJSONService(userService);

		userService.create(new BasicUser(DD4Protos.User.newBuilder()
				.setUsername("user@test.com")
				.setId(1)
				.build()));

		jsonService.performAction("create",
				new JSONObject("{\"id\":1,\"username\":\"Test User\"}"));

		jsonService.performAction("create",
				new JSONObject("{\"id\":2,\"username\":\"test 2\"}"));

		jsonService.performAction("create",
				new JSONObject("{\"id\":3,\"username\":\"test 3\"}"));
	}
}
