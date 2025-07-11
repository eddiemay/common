package com.digitald4.common.storage;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Transaction.Op;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.*;

public class DAOSQLImplTest {
	private static final User ACTIVE_USER = new BasicUser().setId(1001L).setUsername("user1");
	@Mock private final DBConnector connector = mock(DBConnector.class);
	@Mock private final Connection connection = mock(Connection.class);
	@Mock private final PreparedStatement ps = mock(PreparedStatement.class);
	@Mock private final ResultSet rs = mock(ResultSet.class);
	@Mock private final ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

	private DAOSQLImpl daoSql;
	private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
	private final Clock clock = mock(Clock.class);

	@Before
	public void setUp() throws SQLException {
		ChangeTracker changeTracker = new ChangeTracker(() -> ACTIVE_USER, null, searchIndexer, clock);
		daoSql = new DAOSQLImpl(connector, changeTracker);

		when(connector.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(ps);
		when(ps.getGeneratedKeys()).thenReturn(rs);
		when(ps.executeQuery()).thenReturn(rs);
		when(rs.getMetaData()).thenReturn(rsmd);
	}

	@Test
	public void create() throws SQLException {
		when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
				.thenReturn(ps);

		daoSql.persist(Transaction.of(
				Op.create(new BasicUser().setUsername("user@name").setFirstName("FirstName"))));

		verify(connection).prepareStatement(
				"INSERT INTO BasicUser(firstName,username) VALUES(?,?);", Statement.RETURN_GENERATED_KEYS);
		verify(ps).setObject(1, "FirstName");
		verify(ps).setObject(2, "user@name");
	}

	@Test
	public void get() throws SQLException {
		daoSql.get(BasicUser.class, 123);

		verify(connection).prepareStatement("SELECT * FROM BasicUser WHERE id=?;");
		verify(ps).setObject(1, 123);
	}

	@Test
	public void batch_get() throws SQLException {
		daoSql.get(BasicUser.class, ImmutableList.of(123));

		verify(connection).prepareStatement("SELECT * FROM BasicUser WHERE id IN (123);");
	}

	@Test
	public void list() throws SQLException {
		daoSql.list(BasicUser.class, Query.forList(null, "read_only=true,type_id>10", null, 0, 0));

		verify(connection).prepareStatement("SELECT * FROM BasicUser WHERE read_only=? AND type_id>?;");
		verify(ps).setObject(1, "true");
		verify(ps).setObject(2, "10");
	}

	@Test
	public void list_withIn() throws SQLException {
		daoSql.list(BasicUser.class, Query.forList(null, "read_only=true,type_id IN 5|10", null, 0, 0));

		verify(connection).prepareStatement(
				"SELECT * FROM BasicUser WHERE read_only=? AND type_id IN (?);");
		verify(ps).setObject(1, "true");
		verify(ps).setObject(2, "5,10");
	}

	@Test
	public void list_advanced() throws SQLException {
		daoSql.list(BasicUser.class, Query.forList(null, "read_only=true,type_id>10", "username", 10, 3));

		verify(connection).prepareStatement(
				"SELECT * FROM BasicUser WHERE read_only=? AND type_id>? ORDER BY username LIMIT 20,10;");
		verify(ps, times(2)).setObject(1, "true");
		verify(ps, times(2)).setObject(2, "10");
	}

	@Test
	public void update() throws SQLException {
		when(rs.next()).thenReturn(true).thenReturn(false);
		when(rsmd.getColumnCount()).thenReturn(3);
		when(rsmd.getColumnName(anyInt())).thenReturn("id").thenReturn("first_name").thenReturn("last_name");
		when(rs.getObject(anyInt())).thenReturn(123L).thenReturn("fName").thenReturn("lName");

		daoSql.persist(Transaction.of(Op.update(
				BasicUser.class, 123L, user -> user.setTypeId(10).setLastName("LastName"))));

		verify(connection).prepareStatement("SELECT * FROM BasicUser WHERE id IN (123);");
		verify(connection).prepareStatement("UPDATE BasicUser SET lastName=?, typeId=? WHERE id=?;");
		verify(ps).setObject(1, "LastName");
		verify(ps).setObject(2, 10);
		verify(ps).setObject(3, 123L);
	}

	@Test
	public void delete() throws SQLException {
		when(ps.executeUpdate()).thenReturn(1);
		daoSql.delete(BasicUser.class, 123);

		verify(connection).prepareStatement("DELETE FROM BasicUser WHERE id IN (123);");
	}

	@Test
	public void batchDelete() throws SQLException {
		when(ps.executeUpdate()).thenReturn(5);
		daoSql.delete(BasicUser.class, ImmutableList.of(123L, 456L, 789L));
		verify(connection).prepareStatement("DELETE FROM BasicUser WHERE id IN (123,456,789);");
	}
}
