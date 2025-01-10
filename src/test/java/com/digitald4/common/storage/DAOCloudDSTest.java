package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.DataFile;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.Session;
import com.digitald4.common.model.User;
import com.digitald4.common.util.JSONUtil;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;

import java.io.FileInputStream;
import java.time.Clock;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DAOCloudDSTest {
	private static final User ACTIVE_USER = new BasicUser().setId(1001L).setUsername("user1");
	private static final Long ID = 123L;
	private static final String USERNAME = "user1";
	private static final int TYPE_ID = 10;
	private static final String EMAIL = "user@company.com";
	private static final String FIRST_NAME = "Ricky";
	private static final String LAST_NAME = "Bobby";
	private static final BasicUser BASIC_USER = new BasicUser().setId(ID).setUsername(USERNAME)
			.setTypeId(TYPE_ID).setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME);
	private DAOCloudDS dao;
	private ChangeTracker changeTracker;
	private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
	private final Clock clock = mock(Clock.class);

	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() {
		helper.setUp();
		changeTracker = new ChangeTracker(() -> dao, () -> ACTIVE_USER, null, searchIndexer, clock);
		dao = new DAOCloudDS(
				DatastoreServiceFactory.getDatastoreService(), changeTracker, searchIndexer, () -> DAOCloudDS.Context.TEST);
		fillDatabase();
	}

	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void create() {
		BasicUser user = dao.create(new BasicUser().setUsername("anotheruser"));

		assertThat(user.getId()).isGreaterThan(0);
		assertThat(user.getUsername()).isEqualTo("anotheruser");
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

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);

		session = dao.get(Session.class, session.getId());

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);
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

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);

		session = dao.update(Session.class, "4567", s -> s.setExpTime(new DateTime(20000)));

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(20000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);
	}

	@Test
	public void deleteWithIdLong() {
		GeneralData gd = dao.create(
				new GeneralData()
						.setId(4567)
						.setName("test")
						.setDescription("test data"));

		assertThat(gd.getId()).isEqualTo(4567);
		assertThat(gd.getName()).isEqualTo("test");
		assertThat(gd.getDescription()).isEqualTo("test data");

		int deleted = dao.delete(GeneralData.class, ImmutableList.of(4567));

		try {
			dao.get(GeneralData.class, gd.getId());
			fail("Should not have got here");
		} catch (Exception e) {
			// Expected.
		}
		assertThat(deleted).isEqualTo(1);
	}

	@Test
	public void deleteWithIdLongValueAsString() {
		GeneralData gd = dao.create(
				new GeneralData()
						.setId(4567)
						.setName("test")
						.setDescription("test data"));

		assertThat(gd.getId()).isEqualTo(4567);
		assertThat(gd.getName()).isEqualTo("test");
		assertThat(gd.getDescription()).isEqualTo("test data");

		int deleted = dao.delete(GeneralData.class, ImmutableList.of("4567"));

		try {
			dao.get(GeneralData.class, gd.getId());
			fail("Should not have got here");
		} catch (Exception e) {
			// Expected.
		}
		assertThat(deleted).isEqualTo(1);
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

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);

		dao.delete(Session.class, "4567");

		try {
			dao.get(Session.class, session.getId());
			fail("Should not have got here");
		} catch (Exception e) {
			// Expected.
		}
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

		assertThat(result.getId()).isGreaterThan(0L);
		assertEquals(complex.getName(), result.getName());
		assertEquals(complex.getTextData(), result.getTextData());
		assertEquals(complex.getSubComplexes().size(), result.getSubComplexes().size());

		ComplexObj read = dao.get(ComplexObj.class, result.getId());
		assertEquals(complex.getName(), read.getName());
		assertEquals(complex.getTextData().toString(), read.getTextData().toString());
		assertEquals(complex.getSubComplexes().size(), read.getSubComplexes().size());
	}

	@Test
	public void createEmptyList() {
		dao.create(ImmutableList.of());
	}

	@Test
	public void createDataFile() throws Exception {
		try(FileInputStream fis = new FileInputStream("pom.xml")) {
			byte[] data = new byte[2048];
			int size = fis.read(data);
			DataFile dataFile = new DataFile().setName("Test File.txt").setSize(size).setData(data);
			JSONObject json = JSONUtil.toJSON(dataFile);
			dataFile = dao.create(dataFile);

			assertEquals(data, dataFile.getData());
		}
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

		assertThat(result.getId()).isGreaterThan(0L);
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

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		assertEquals(5, queryResult.getItems().size());
	}

	@Test
	public void list_withFilter() {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList("typeId>10", null, null, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(2);
		assertEquals(2, queryResult.getItems().size());
		assertThat(queryResult.getItems().get(0).getTypeId()).isGreaterThan(10);
		assertThat(queryResult.getItems().get(1).getTypeId()).isGreaterThan(10);
	}

	@Test
	public void list_withInFilter() {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList("typeId IN 4|10", null, 0, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(2);
		assertEquals(2, queryResult.getItems().size());
		assertEquals(4, queryResult.getItems().get(0).getTypeId());
		assertEquals(10, queryResult.getItems().get(1).getTypeId());
	}

	@Test
	public void list_withOrderBy() {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList(null, "typeId", 0, 0));

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		assertEquals(5, queryResult.getItems().size());

		assertThat(queryResult.getItems().stream().map(BasicUser::getTypeId).collect(toImmutableList()))
				.containsExactly(2, 4, 10, 18, 22).inOrder();
	}

	@Test
	public void list_withLimit() {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList(null, null, 3, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		assertEquals(3, queryResult.getItems().size());
	}

	@Test
	public void list_withOffset() {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList(null, null, 2, 2));

		assertEquals(2, queryResult.getItems().size());
		assertThat(queryResult.getTotalSize()).isEqualTo(5);
	}

	@Test
	public void list_advanced()  {
		QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, Query.forList("typeId>=2", "typeId", 2, 2));

		assertEquals(2, queryResult.getItems().size());
		assertEquals(10, queryResult.getItems().get(0).getTypeId());
		assertThat(queryResult.getTotalSize()).isEqualTo(5);
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
