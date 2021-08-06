package com.digitald4.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.server.service.GeneralDataService;
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
	@Mock private final UserStore mockUserStore = mock(UserStore.class);

	@Test
	public void testCreate() {
		when(mockStore.getType()).thenReturn(new GeneralData());
		when(mockStore.create(any(GeneralData.class))).thenAnswer(i -> i.getArguments()[0]);
		GeneralDataService generalDataService = new GeneralDataService(mockStore);

		generalDataService.create(new GeneralData().setName("Test").setId(1));

		generalDataService.performAction("create",
				new JSONObject("{\"id\":1,\"name\":\"test 1\"}"));

		generalDataService.performAction("create",
				new JSONObject("{\"id\":2,\"name\":\"test 2\"}"));

		generalDataService.performAction("create",
				new JSONObject("{\"id\":3,\"name\":\"test 3\"}"));
	}

	@Test
	@Ignore
	public void testUpdate() {
		DAOTestingImpl dao = new DAOTestingImpl();
		GeneralDataService generalDataService = new GeneralDataService(new GeneralDataStore(() -> dao));

		GeneralData gd = generalDataService.create(new GeneralData().setName("Test"));
		assertTrue(gd.getId() > 0);
		assertEquals("Test", gd.getName());

		gd = generalDataService.get(gd.getId());
		assertTrue(gd.getId() > 0);
		assertEquals("Test", gd.getName());

		gd = generalDataService.update(gd.getId(), new GeneralData().setName("Test2"), "name");
		assertTrue(gd.getId() > 0);
		assertEquals("Test2", gd.getName());

		gd = JSONUtil.toObject(GeneralData.class,
				generalDataService.performAction("update",
						new JSONObject("{\"id\":" + gd.getId() + ",\"entity\":{\"name\":\"test 3\"},\"updateMask\":\"name\"}}")));
		assertTrue(gd.getId() > 0);
		assertEquals("test 3", gd.getName());
	}

	@Test
	public void testCreateUser() {
		when(mockUserStore.getType()).thenReturn(new BasicUser());
		when(mockUserStore.create(any(BasicUser.class))).thenAnswer(i -> i.getArguments()[0]);
		UserService userService = new UserService(mockUserStore, null, null, null);

		userService.create(new BasicUser().setUsername("user@test.com").setId(1));

		userService.performAction("create", new JSONObject("{\"id\":2,\"username\":\"Test User\"}"));

		userService.performAction("create", new JSONObject("{\"id\":3,\"username\":\"test 3\"}"));

		userService.performAction("create", new JSONObject("{\"id\":4,\"username\":\"test 4\"}"));
	}
}
