package com.digitald4.common.storage;

import com.digitald4.common.model.BasicUser;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.time.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class DAOCloudDSTest {
	private static final long ID = 123;
	private DAOCloudDS dao;

	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() {
		helper.setUp();
		dao = new DAOCloudDS(DatastoreServiceFactory.getDatastoreService());
		fillDatabase();
	}

	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void create() {
		BasicUser user = dao.create(new BasicUser().setUsername("anotheruser").updateLastLogin(Clock.systemUTC()));

		assertTrue(user.getId() > 0);
		assertEquals("anotheruser", user.getUsername());
	}

	@Test
	public void get() {
		dao.create(new BasicUser().setUsername("user@name").updateLastLogin(Clock.systemUTC()));

		BasicUser user = dao.get(BasicUser.class, ID);

		assertEquals(ID, user.getId());
		assertEquals("user@name", user.getUsername());
		assertEquals(10, user.getTypeId());
	}

	@Test
	public void list() {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, new Query());

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(5, queryResult.getResults().size());
	}

	@Test
	public void list_withFilter() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forValues("type_id>10", null, 0, 0));

		assertEquals(2, queryResult.getTotalSize());
		assertEquals(2, queryResult.getResults().size());
		assertTrue(queryResult.getResults().get(0).getTypeId() > 10);
		assertTrue(queryResult.getResults().get(1).getTypeId() > 10);
	}

	@Test
	public void list_withOrderBy() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forValues(null, "type_id",0, 0));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(5, queryResult.getResults().size());

		int prevTypeId = 0;
		for (BasicUser user : queryResult.getResults()) {
			assertTrue(prevTypeId < user.getTypeId());
			prevTypeId = user.getTypeId();
		}
	}

	@Test
	public void list_withLimit() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forValues(null, null, 3, 0));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(3, queryResult.getResults().size());
	}

	@Test
	public void list_withOffset() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forValues(null, null, 0, 2));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(3, queryResult.getResults().size());
	}

	@Test
	public void list_advanced()  {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forValues("type_id>=2", "type_id", 3, 2));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(3, queryResult.getResults().size());
		assertEquals(10, queryResult.getResults().get(0).getTypeId());
	}

	@Test
	public void update() {
		BasicUser user = dao.update(BasicUser.class, ID, u -> u.setTypeId(10));

		assertEquals("user@name", user.getUsername());
		assertEquals(10, user.getTypeId());
	}

	@Test
	public void delete() {
		dao.delete(BasicUser.class, ID);
	}

	@Test
	public void batchDelete() {
		int deleted = dao.delete(BasicUser.class, Query.forValues("type_id>=10", null, 0, 0));

		assertEquals(3, deleted);
	}

	public void fillDatabase() {
		dao.create(new BasicUser().setId(ID).setUsername("user@name").setTypeId(10));
		dao.create(new BasicUser().setUsername("user2").setTypeId(4));
		dao.create(new BasicUser().setUsername("user3").setTypeId(18));
		dao.create(new BasicUser().setUsername("user4").setTypeId(22));
		dao.create(new BasicUser().setUsername("user5").setTypeId(2));
	}
}
