package com.digitald4.common.storage;

import static junit.framework.TestCase.*;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.Session;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DAOCloudDSTest {
	private static final Long ID = 123L;
	private DAOCloudDS dao;

	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() {
		helper.setUp();
		dao = new DAOCloudDS(DatastoreServiceFactory.getDatastoreService(), null);
		fillDatabase();
	}

	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void create() {
		BasicUser user = dao.create(new BasicUser().setUsername("anotheruser"));

		assertTrue(user.getId() > 0);
		assertEquals("anotheruser", user.getUsername());
	}

	@Test
	public void createWithEnum() {
		Session session = dao.create(
				new Session()
						.setId("4567")
						.setUserId(123)
						.setStartTime(new DateTime(1000))
						.setExpTime(new DateTime(10000))
						.setState(Session.State.ACTIVE));

		assertEquals("4567",session.getId());
		assertEquals(123, session.getUserId());
		assertEquals(1000, session.getStartTime().getMillis());
		assertEquals(10000, session.getExpTime().getMillis());
		assertEquals(Session.State.ACTIVE, Session.State.ACTIVE);

		session = dao.get(Session.class, session.getId());
		assertEquals("4567", session.getId());
		assertEquals(123, session.getUserId());
		assertEquals(1000, session.getStartTime().getMillis());
		assertEquals(10000, session.getExpTime().getMillis());
		assertEquals(Session.State.ACTIVE, Session.State.ACTIVE);
	}

	@Test
	public void updateWithIdString() {
		Session session = dao.create(
				new Session()
						.setId("4567")
						.setUserId(123)
						.setStartTime(new DateTime(1000))
						.setExpTime(new DateTime(10000))
						.setState(Session.State.ACTIVE));

		assertEquals("4567",session.getId());
		assertEquals(123, session.getUserId());
		assertEquals(1000, session.getStartTime().getMillis());
		assertEquals(10000, session.getExpTime().getMillis());
		assertEquals(Session.State.ACTIVE, Session.State.ACTIVE);

		session = dao.update(Session.class, "4567", s -> s.setExpTime(new DateTime(20000)));
		assertEquals("4567", session.getId());
		assertEquals(123, session.getUserId());
		assertEquals(1000, session.getStartTime().getMillis());
		assertEquals(20000, session.getExpTime().getMillis());
		assertEquals(Session.State.ACTIVE, Session.State.ACTIVE);
	}

	@Test
	public void deleteWithIdString() {
		Session session = dao.create(
				new Session()
						.setId("4567")
						.setUserId(123)
						.setStartTime(new DateTime(1000))
						.setExpTime(new DateTime(10000))
						.setState(Session.State.ACTIVE));

		assertEquals("4567",session.getId());
		assertEquals(123, session.getUserId());
		assertEquals(1000, session.getStartTime().getMillis());
		assertEquals(10000, session.getExpTime().getMillis());
		assertEquals(Session.State.ACTIVE, Session.State.ACTIVE);

		dao.delete(Session.class, "4567");
	}

	@Test
	public void createComplex() {
		ComplexObj complex = new ComplexObj()
				.setName("test")
				.setTextData(new StringBuilder("Start of string"))
				.setSubComplexes(
						ImmutableList.of(
								new ComplexObj.SubComplex().setBrand("A").setHistory(ImmutableList.of("A1", "A2", "A3")),
								new ComplexObj.SubComplex().setBrand("B").setHistory(ImmutableList.of("B1", "B2"))));

		ComplexObj result = dao.create(complex);

		assertTrue(result.getId() > 0);
		assertEquals(complex.getName(), result.getName());
		assertEquals(complex.getTextData(), result.getTextData());
		assertEquals(complex.getSubComplexes().size(), result.getSubComplexes().size());

		ComplexObj read = dao.get(ComplexObj.class, result.getId());
		assertEquals(complex.getName(), read.getName());
		assertEquals(complex.getTextData().toString(), read.getTextData().toString());
		assertEquals(complex.getSubComplexes().size(), read.getSubComplexes().size());

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
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList());

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(5, queryResult.getItems().size());
	}

	@Test
	public void list_withFilter() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forList("type_id>10", null, 0, 1));

		assertEquals(2, queryResult.getTotalSize());
		assertEquals(2, queryResult.getItems().size());
		assertTrue(queryResult.getItems().get(0).getTypeId() > 10);
		assertTrue(queryResult.getItems().get(1).getTypeId() > 10);
	}

	@Test
	public void list_withOrderBy() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forList(null, "type_id",0, 0));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(5, queryResult.getItems().size());

		int prevTypeId = 0;
		for (BasicUser user : queryResult.getItems()) {
			assertTrue(prevTypeId < user.getTypeId());
			prevTypeId = user.getTypeId();
		}
	}

	@Test
	public void list_withLimit() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forList(null, null, 3, 1));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(3, queryResult.getItems().size());
	}

	@Test
	public void list_withOffset() {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forList(null, null, 2, 2));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(2, queryResult.getItems().size());
	}

	@Test
	public void list_advanced()  {
		QueryResult<BasicUser> queryResult =
				dao.list(BasicUser.class, Query.forList("type_id>=2", "type_id", 2, 2));

		assertEquals(5, queryResult.getTotalSize());
		assertEquals(2, queryResult.getItems().size());
		assertEquals(10, queryResult.getItems().get(0).getTypeId());
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
		dao.delete(BasicUser.class, ImmutableList.of(123L, 456L, 789L));
	}

	public void fillDatabase() {
		dao.create(new BasicUser().setId(ID).setUsername("user@name").setTypeId(10));
		dao.create(new BasicUser().setUsername("user2").setTypeId(4));
		dao.create(new BasicUser().setId(456L).setUsername("user3").setTypeId(18));
		dao.create(new BasicUser().setUsername("user4").setTypeId(22));
		dao.create(new BasicUser().setId(789L).setUsername("user5").setTypeId(2));
	}

	public static class ComplexObj {
		private long id;
		private String name;
		private StringBuilder textData;
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

		public StringBuilder getTextData() {
			return textData;
		}

		public ComplexObj setTextData(StringBuilder textData) {
			this.textData = textData;
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
