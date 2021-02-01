package com.digitald4.common.storage.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import javax.inject.Provider;

import com.google.protobuf.Message;
import org.junit.Test;

public class DAOTestingImplTest {
	private final DAO<Message> dao = new DAOTestingImpl();
	private final Provider<DAO<Message>> daoProvider = () -> dao;
	private final GenericStore<Message, User> userStore = new GenericStore<>(User.class, daoProvider);

	@Test
	public void testCreate() {
		User user = userStore.create(User.newBuilder()
				.setUsername("eddiemay@gmail.com")
				.setTypeId(56)
				.build());
		assertTrue(user.getId() > 0);
		assertEquals("eddiemay@gmail.com", user.getUsername());
		assertEquals(56, user.getTypeId());

		assertEquals(user, userStore.get(user.getId()));

		QueryResult<User> users = userStore.list(new Query());
		assertEquals(1, users.getTotalSize());
		assertEquals(1, users.getResults().size());
		assertEquals(user, users.getResults().get(0));

		user = userStore.update(user.getId(), user_ -> user_.toBuilder()
				.setUsername("eddiemay1999@yahoo.com")
				.build());
		assertEquals("eddiemay1999@yahoo.com", user.getUsername());

		User user2 = userStore.create(User.newBuilder()
				.setUsername("benfrank@gmail.com")
				.setTypeId(34)
				.build());
		assertTrue(user2.getId() > 0);
		assertEquals("benfrank@gmail.com", user2.getUsername());
		assertEquals(34, user2.getTypeId());

		users = userStore.list(new Query());
		assertEquals(2, users.getTotalSize());
		assertEquals(2, users.getResults().size());
		assertEquals(user, users.getResults().get(0));
		assertEquals(user2, users.getResults().get(1));

		users = userStore.list(new Query().setFilters(new Query.Filter().setColumn("type_id").setValue("34")));
		assertEquals(1, users.getTotalSize());
		assertEquals(1, users.getResults().size());
		assertEquals(user2, users.getResults().get(0));
	}
}
