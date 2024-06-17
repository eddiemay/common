package com.digitald4.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.server.service.GeneralDataService;
import com.digitald4.common.server.service.JSONServiceHelper;
import com.digitald4.common.server.service.UserService;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.digitald4.common.util.JSONUtil;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

public class GeneralDataServiceTest {
	@Mock private final GeneralDataStore mockStore = mock(GeneralDataStore.class);
	@Mock private final UserStore<BasicUser> mockUserStore = mock(UserStore.class);
	@Mock private final SessionStore<BasicUser> sessionStore = mock(SessionStore.class);

	@Test
	public void testCreate() throws Exception {
		when(mockStore.getTypeClass()).thenReturn(GeneralData.class);
		when(mockStore.create(any(GeneralData.class))).thenAnswer(i -> i.getArguments()[0]);
		GeneralDataService generalDataService = new GeneralDataService(mockStore, sessionStore);
		JSONServiceHelper<GeneralData> serviceHelper = new JSONServiceHelper<>(generalDataService);

		generalDataService.create(new GeneralData().setName("Test").setId(1), null);

		serviceHelper.performAction("create", new JSONObject("{entity:{\"id\":1,\"name\":\"test 1\"}}"));
		serviceHelper.performAction("create", new JSONObject("{entity:{\"id\":2,\"name\":\"test 2\"}}"));
		serviceHelper.performAction("create", new JSONObject("{entity:{\"id\":3,\"name\":\"test 3\"}}"));
	}

	@Test
	@Ignore
	public void testUpdate() throws Exception {
		DAOTestingImpl dao = new DAOTestingImpl(new ChangeTracker(null, null, null, null, null));
		GeneralDataService generalDataService =
				new GeneralDataService(new GeneralDataStore(() -> dao), sessionStore);
		JSONServiceHelper<GeneralData> serviceHelper = new JSONServiceHelper<>(generalDataService);

		GeneralData gd = generalDataService.create(new GeneralData().setName("Test"), null);
		assertTrue(gd.getId() > 0);
		assertEquals("Test", gd.getName());

		gd = generalDataService.get(gd.getId(), null);
		assertTrue(gd.getId() > 0);
		assertEquals("Test", gd.getName());

		gd = generalDataService.update(gd.getId(), new GeneralData().setName("Test2"), "name", null);
		assertTrue(gd.getId() > 0);
		assertEquals("Test2", gd.getName());

		gd = JSONUtil.toObject(GeneralData.class,
				serviceHelper.performAction("update",
						new JSONObject("{\"id\":" + gd.getId()
								+ ",\"entity\":{\"name\":\"test 3\"},\"updateMask\":\"name\"}}")));
		assertTrue(gd.getId() > 0);
		assertEquals("test 3", gd.getName());
	}

	@Test
	public void testCreateUser() throws Exception {
		when(mockUserStore.getTypeClass()).thenReturn(BasicUser.class);
		when(mockUserStore.create(any(BasicUser.class))).thenAnswer(i -> i.getArguments()[0]);
		UserService<BasicUser> userService = new UserService<>(mockUserStore, sessionStore, null);
		UserService.UserJSONService<BasicUser> userJSONService =
				new UserService.UserJSONService<>(userService);

		BasicUser basicUser =
				userService.create(new BasicUser().setUsername("user@test.com").setId(1L), null);
		assertEquals("user@test.com", basicUser.getUsername());

		userJSONService.performAction(
				"create", new JSONObject("{entity:{\"id\":2,\"username\":\"Test User\"}}"));

		userJSONService.performAction(
				"create", new JSONObject("{entity:{\"id\":3,\"username\":\"test 3\"}}"));

		userJSONService.performAction(
				"create", new JSONObject("{entity:{\"id\":4,\"username\":\"test 4\"}}"));
	}
}
