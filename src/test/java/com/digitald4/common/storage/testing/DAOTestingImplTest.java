package com.digitald4.common.storage.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.QueryResult;
import javax.inject.Provider;
import org.junit.Test;

public class DAOTestingImplTest {
	private final DAO dao = new DAOTestingImpl();
	private final Provider<DAO> daoProvider = () -> dao;
	private final GenericStore<User> userStore = new GenericStore<>(User.class, daoProvider);

	@Test
	public void testCreate() {
		User user = userStore.create(User.newBuilder()
				.setFirstName("Eddie")
				.setLastName("Mayfield")
				.setEmail("eddiemay@gmail.com")
				.setTypeId(56)
				.build());
		assertTrue(user.getId() > 0);
		assertEquals("Eddie", user.getFirstName());
		assertEquals("Mayfield", user.getLastName());
		assertEquals("eddiemay@gmail.com", user.getEmail());
		assertEquals(56, user.getTypeId());

		assertEquals(user, userStore.get(user.getId()));

		QueryResult<User> users = userStore.list(Query.getDefaultInstance());
		assertEquals(1, users.getTotalSize());
		assertEquals(1, users.getResults().size());
		assertEquals(user, users.getResults().get(0));

		user = userStore.update(user.getId(), user_ -> user_.toBuilder()
				.setEmail("eddiemay1999@yahoo.com")
				.build());
		assertEquals("eddiemay1999@yahoo.com", user.getEmail());

		User user2 = userStore.create(User.newBuilder()
				.setFirstName("Ben")
				.setLastName("Frank")
				.setEmail("benfrank@gmail.com")
				.setTypeId(34)
				.build());
		assertTrue(user2.getId() > 0);
		assertEquals("Ben", user2.getFirstName());
		assertEquals("Frank", user2.getLastName());
		assertEquals("benfrank@gmail.com", user2.getEmail());
		assertEquals(34, user2.getTypeId());

		users = userStore.list(Query.getDefaultInstance());
		assertEquals(2, users.getTotalSize());
		assertEquals(2, users.getResults().size());
		assertEquals(user, users.getResults().get(0));
		assertEquals(user2, users.getResults().get(1));

		users = userStore.list(Query.newBuilder()
				.addFilter(Filter.newBuilder().setColumn("type_id").setValue("34"))
				.build());
		assertEquals(1, users.getTotalSize());
		assertEquals(1, users.getResults().size());
		assertEquals(user2, users.getResults().get(0));
	}
}
