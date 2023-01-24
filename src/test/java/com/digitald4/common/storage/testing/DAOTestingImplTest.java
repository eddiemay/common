package com.digitald4.common.storage.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.storage.*;
import org.junit.Test;

public class DAOTestingImplTest {
	private final DAOTestingImpl dao = new DAOTestingImpl();
	private final GenericStore<BasicUser, Long> userStore = new GenericStore<>(BasicUser.class, () -> dao);

	@Test
	public void testCreate() {
		BasicUser user = userStore.create(new BasicUser().setUsername("eddiemay@gmail.com").setTypeId(56));
		assertTrue(user.getId() > 0);
		assertEquals("eddiemay@gmail.com", user.getUsername());
		assertEquals(56, user.getTypeId());

		assertEquals(user, userStore.get(user.getId()));

		QueryResult<BasicUser> users = userStore.list(Query.forList());
		assertEquals(1, users.getTotalSize());
		assertEquals(1, users.getItems().size());
		assertEquals(user, users.getItems().get(0));

		user = userStore.update(user.getId(), user_ -> user_.setUsername("eddiemay1999@yahoo.com"));
		assertEquals("eddiemay1999@yahoo.com", user.getUsername());

		BasicUser user2 = userStore.create(new BasicUser().setUsername("benfrank@gmail.com").setTypeId(34));
		assertTrue(user2.getId() > 0);
		assertEquals("benfrank@gmail.com", user2.getUsername());
		assertEquals(34, user2.getTypeId());

		users = userStore.list(Query.forList());
		assertEquals(2, users.getTotalSize());
		assertEquals(2, users.getItems().size());
		assertEquals(user, users.getItems().get(0));
		assertEquals(user2, users.getItems().get(1));

		users = userStore.list(Query.forList().setFilters(Query.Filter.of("typeId", 34)));
		assertEquals(1, users.getTotalSize());
		assertEquals(1, users.getItems().size());
		assertEquals(user2, users.getItems().get(0));
	}
}
