package com.digitald4.common.storage;

import static junit.framework.TestCase.*;

import com.digitald4.common.model.BasicUser;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	public void createComplex() {
		ComplexObj complex = new ComplexObj()
				.setName("test")
				.setSubComplexes(
						ImmutableList.of(
								new ComplexObj.SubComplex().setBrand("A").setHistory(ImmutableList.of("A1", "A2", "A3")),
								new ComplexObj.SubComplex().setBrand("B").setHistory(ImmutableList.of("B1", "B2"))));

		ComplexObj result = dao.create(complex);

		assertTrue(result.getId() > 0);
		assertEquals(complex.getName(), result.getName());
		assertEquals(complex.getSubComplexes().size(), result.getSubComplexes().size());
	}

	@Test
	public void get() {
		BasicUser user = dao.get(BasicUser.class, ID);

		assertEquals(ID, user.getId());
		assertEquals("user@name", user.getUsername());
		assertEquals(10, user.getTypeId());
	}

	@Test
	public void getComplex() {
		long id = 200;
		ComplexObj complex = new ComplexObj()
				.setId(id)
				.setName("test")
				.setSubComplexes(
						ImmutableList.of(
								new ComplexObj.SubComplex().setBrand("A").setHistory(ImmutableList.of("A1", "A2", "A3")),
								new ComplexObj.SubComplex().setBrand("B").setHistory(ImmutableList.of("B1", "B2"))));
		dao.create(complex);

		// assertEquals(200, complex.getId());

		ComplexObj result = dao.get(ComplexObj.class, complex.getId());

		assertTrue(result.getId() > 0);
		assertEquals(complex.getName(), result.getName());
		assertEquals(complex.getSubComplexes().size(), result.getSubComplexes().size());

		ComplexObj.SubComplex subComplex = complex.getSubComplexes().get(0);
		assertEquals("A", subComplex.getBrand());
		assertEquals(3, subComplex.getHistory().size());
		assertEquals("A1", subComplex.getHistory().get(0));
		assertEquals("A2", subComplex.getHistory().get(1));
		assertEquals("A3", subComplex.getHistory().get(2));

		subComplex = complex.getSubComplexes().get(1);
		assertEquals("B", subComplex.getBrand());
		assertEquals(2, subComplex.getHistory().size());
		assertEquals("B1", subComplex.getHistory().get(0));
		assertEquals("B2", subComplex.getHistory().get(1));
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
		int deleted = dao.delete(BasicUser.class, ImmutableList.of(123L, 456L, 789L));

		assertEquals(3, deleted);
	}

	public void fillDatabase() {
		dao.create(new BasicUser().setId(ID).setUsername("user@name").setTypeId(10));
		dao.create(new BasicUser().setUsername("user2").setTypeId(4));
		dao.create(new BasicUser().setUsername("user3").setTypeId(18));
		dao.create(new BasicUser().setUsername("user4").setTypeId(22));
		dao.create(new BasicUser().setUsername("user5").setTypeId(2));
	}

	public static class ComplexObj {
		private long id;
		private String name;
		private ImmutableList<SubComplex> subComplexes = ImmutableList.of();

		public long getId() {
			return id;
		}

		public ComplexObj setId(long id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public ComplexObj setName(String name) {
			this.name = name;
			return this;
		}

		public ImmutableList<SubComplex> getSubComplexes() {
			return subComplexes;
		}

		public ComplexObj setSubComplexes(Iterable<SubComplex> subComplexes) {
			this.subComplexes = ImmutableList.copyOf(subComplexes);
			return this;
		}

		public static class SubComplex {
			private String brand;
			private ImmutableList<String> history;

			public String getBrand() {
				return brand;
			}

			public SubComplex setBrand(String brand) {
				this.brand = brand;
				return this;
			}

			public ImmutableList<String> getHistory() {
				return history;
			}

			public SubComplex setHistory(Iterable<String> history) {
				this.history = ImmutableList.copyOf(history);
				return this;
			}
		}
	}
}
