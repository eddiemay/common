package com.digitald4.common.storage.testing;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.storage.*;
import org.junit.Test;

public class DAOTestingImplTest {
	private final DAOTestingImpl dao = new DAOTestingImpl(new ChangeTracker(null, null, null, null, null));
	private final GenericStore<BasicUser, Long> userStore = new GenericStore<>(BasicUser.class, () -> dao);

	@Test
	public void testCreate() {
		BasicUser user = userStore.create(new BasicUser().setUsername("eddiemay@gmail.com").setTypeId(56));
		assertThat(user.getId()).isGreaterThan(0);
		assertThat(user.getUsername()).isEqualTo("eddiemay@gmail.com");
		assertThat(user.getTypeId()).isEqualTo(56);

		assertThat(userStore.get(user.getId())).isEqualTo(user);

		QueryResult<BasicUser> users = userStore.list(Query.forList());
		assertThat(users.getItems()).containsExactly(user);

		user = userStore.update(user.getId(), user_ -> user_.setUsername("eddiemay1999@yahoo.com"));
		assertThat(user.getUsername()).isEqualTo("eddiemay1999@yahoo.com");

		BasicUser user2 = userStore.create(new BasicUser().setUsername("benfrank@gmail.com").setTypeId(34));
		assertThat(user2.getId()).isGreaterThan(0);
		assertThat(user2.getUsername()).isEqualTo("benfrank@gmail.com");
		assertThat(user2.getTypeId()).isEqualTo(34);

		users = userStore.list(Query.forList());
		assertThat(users.getItems()).containsExactly(user, user2);

		users = userStore.list(Query.forList().setFilters(Query.Filter.of("typeId", 34)));
		assertThat(users.getItems()).containsExactly(user2);
	}
}
