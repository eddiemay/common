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
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.util.JSONUtil;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;

import java.io.FileInputStream;
import java.time.Clock;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DAOAppEngineDatastoreTest {
	private static final User ACTIVE_USER = new BasicUser().setId(1001L).setUsername("user1");
	private static final Long ID = 123L;
	private DAOAppEngineDatastore dao;
	private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
	private final Clock clock = mock(Clock.class);

	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() {
		helper.setUp();
		dao = new DAOAppEngineDatastore(() -> DAOAppEngineDatastore.Context.TEST,
				new ChangeTracker(() -> ACTIVE_USER, null, searchIndexer, clock), searchIndexer);
		fillDatabase();
	}

	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void create() {
		BasicUser user = create(new BasicUser().setUsername("another_user"));

		assertThat(user.getId()).isGreaterThan(0);
		assertThat(user.getUsername()).isEqualTo("another_user");
	}

	@Test
	public void createWithEnum() {
		Session session = create(
				new Session()
						.setId("4567")
						.setUserId(123)
						.setUsername("username")
						.setStartTime(new DateTime(1000))
						.setExpTime(new DateTime(10000))
						.setState(Session.State.ACTIVE));

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getUsername()).isEqualTo("username");
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);

		session = dao.get(Session.class, session.getId());

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getUsername()).isEqualTo("username");
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);
	}

	@Test
	public void updateWithIdString() {
		Session session = create(
				new Session()
						.setId("4567")
						.setUserId(123)
						.setUsername("username")
						.setStartTime(new DateTime(1000))
						.setExpTime(new DateTime(10000))
						.setState(Session.State.ACTIVE));

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getUsername()).isEqualTo("username");
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);

		session = dao.get(Session.class, "4567");
		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getUsername()).isEqualTo("username");
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(10000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);


		session = update(Session.class, "4567", s -> s.setExpTime(new DateTime(20000)));

		assertThat(session.getId()).isEqualTo("4567");
		assertThat(session.getUserId()).isEqualTo(123);
		assertThat(session.getUsername()).isEqualTo("username");
		assertThat(session.getStartTime().getMillis()).isEqualTo(1000);
		assertThat(session.getExpTime().getMillis()).isEqualTo(20000);
		assertThat(session.getState()).isEqualTo(Session.State.ACTIVE);
	}

	@Test
	public void deleteWithIdLong() {
		GeneralData gd = create(
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
		GeneralData gd = create(
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
		Session session = create(
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

		ComplexObj result = create(complex);

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
	public void createDataFile() throws Exception {
		try(FileInputStream fis = new FileInputStream("pom.xml")) {
			byte[] data = new byte[2048];
			int size = fis.read(data);
			DataFile dataFile = new DataFile()
					.setId("Test_File.text").setName("Test File.txt").setSize(size).setData(data);
			JSONObject json = JSONUtil.toJSON(dataFile);
			dataFile = create(dataFile);

			assertEquals(data, dataFile.getData());
			assertEquals("Test_File.text", json.get("id"));
			assertEquals("Test File.txt", json.get("name"));
		}
	}

	@Test
	public void get() {
		BasicUser user = dao.get(BasicUser.class, ID);

		assertEquals(ID, user.getId());
		assertThat(user.getUsername()).isEqualTo("user0");
		assertThat(user.getTypeId()).isEqualTo(0);
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
		create(complex);

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
		var queryResult = dao.list(BasicUser.class, Query.forList());

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		for (BasicUser user : queryResult.getItems()) {
			assertThat(user.getId()).isNotNull();
			assertThat(user.getUsername()).isNotNull();
			assertThat(user.getEmail()).isNotNull();
			assertThat(user.getFirstName()).isNotNull();
			assertThat(user.getLastName()).isNotNull();
		}
	}

	@Test
	public void list_withFields() {
		var queryResult = dao.list(BasicUser.class, Query.forList("id,username,email", null, null, null, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		for (BasicUser user : queryResult.getItems()) {
			assertThat(user.getId()).isNotNull();
			assertThat(user.getUsername()).isNotNull();
			assertThat(user.getEmail()).isNotNull();
			assertThat(user.getFirstName()).isNull();
			assertThat(user.getLastName()).isNull();
		}
	}

	@Test
	public void list_withFilter() {
		var queryResult = dao.list(BasicUser.class, Query.forList(null, "typeId>10", null, null, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(2);
		assertEquals(2, queryResult.getItems().size());
		assertThat(queryResult.getItems().get(0).getTypeId()).isGreaterThan(10);
		assertThat(queryResult.getItems().get(1).getTypeId()).isGreaterThan(10);
	}

	@Test
	public void list_withInFilter() {
		var queryResult = dao.list(BasicUser.class, Query.forList(null, "typeId IN 4|12", null, 0, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(2);
		assertEquals(2, queryResult.getItems().size());
		assertEquals(4, queryResult.getItems().get(0).getTypeId());
		assertEquals(12, queryResult.getItems().get(1).getTypeId());
	}

	@Test
	public void list_withOrderBy() {
		var queryResult = dao.list(BasicUser.class, Query.forList(null, null, "typeId", 0, 0));

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		assertThat(queryResult.getItems()).hasSize(5);

		assertThat(queryResult.getItems().stream().map(BasicUser::getTypeId).collect(toImmutableList()))
				.containsExactly(0, 4, 8, 12, 16).inOrder();


		queryResult = dao.list(BasicUser.class, Query.forList(null, null, "typeId DESC", 0, 0));
		assertThat(queryResult.getItems().stream().map(BasicUser::getTypeId).collect(toImmutableList()))
				.containsExactly(16, 12, 8, 4, 0).inOrder();
	}

	@Test
	public void list_withLimit() {
		var queryResult = dao.list(BasicUser.class, Query.forList(null, null, null, 3, 1));

		assertThat(queryResult.getTotalSize()).isEqualTo(5);
		assertEquals(3, queryResult.getItems().size());
	}

	@Test
	public void list_withOffset() {
		var queryResult = dao.list(BasicUser.class, Query.forList(null, null, null, 2, 2));

		assertEquals(2, queryResult.getItems().size());
		assertThat(queryResult.getTotalSize()).isEqualTo(5);
	}

	@Test
	public void list_advanced()  {
		var queryResult = dao.list(BasicUser.class,
				Query.forList(null, "typeId>=2", "typeId", 2, 2));

		assertEquals(2, queryResult.getItems().size());
		assertEquals(12, queryResult.getItems().get(0).getTypeId());
		assertThat(queryResult.getTotalSize()).isEqualTo(4);
	}

	@Test
	public void update() {
		BasicUser user = update(BasicUser.class, ID, u -> u.setTypeId(10));

		assertEquals("user0", user.getUsername());
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
		IntStream.range(0, 5).forEach(i -> create(new BasicUser().setId(ID + i).setTypeId(i * 4)
				.setUsername("user" + i).setEmail(String.format("user%d@email.com", i))
				.setFirstName("firstName" + i).setLastName("lastName" + i)));
	}

	public <T> T create(T t) {
		return dao.persist(Transaction.of(Op.create(t))).getOps().get(0).getEntity();
	}

	public <T, I> T update(Class<T> cls, I id, UnaryOperator<T> updater) {
		return dao.persist(Transaction.of(Op.update(cls, id, updater))).getOps().get(0).getEntity();
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
