package com.digitald4.common.storage;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.proto.DD4Protos.User;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.*;

public class DAOProtoSQLImplTest {
	@Mock private final DBConnector connector = mock(DBConnector.class);
	@Mock private final Connection connection = mock(Connection.class);
	@Mock private final PreparedStatement ps = mock(PreparedStatement.class);
	@Mock private final ResultSet rs = mock(ResultSet.class);
	@Mock private final ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

	private DAOSQLImpl daoSql;

	@Before
	public void setUp() throws SQLException {
		daoSql = new DAOSQLImpl(connector);

		when(connector.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(ps);
		when(ps.getGeneratedKeys()).thenReturn(rs);
		when(ps.executeQuery()).thenReturn(rs);
		when(rs.getMetaData()).thenReturn(rsmd);
	}

	@Test
	public void create() throws SQLException {
		String insertSql = "INSERT INTO User(username,read_only) VALUES(?,?);";
		when(connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)).thenReturn(ps);

		daoSql.create(User.newBuilder().setUsername("user@name").setReadOnly(true).build());

		verify(connection).prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
		verify(ps).setObject(1, "user@name");
		verify(ps).setObject(2, true);
	}

	@Test
	public void get() throws SQLException {
		daoSql.get(User.class, 123);

		verify(connection).prepareStatement("SELECT * FROM User WHERE id=?;");
		verify(ps).setLong(1, 123);
	}

	@Test
	public void list() throws SQLException {
		daoSql.list(User.class, Query.forList("read_only=true,type_id>10", null, 0, 0));

		verify(connection).prepareStatement("SELECT * FROM User WHERE read_only=? AND type_id>?;");
		verify(ps).setObject(1, "true");
		verify(ps).setObject(2, "10");
	}

	@Test
	public void list_advanced() throws SQLException {
		daoSql.list(User.class, Query.forList("read_only=true,type_id>10", "username", 10, 3));

		verify(connection)
				.prepareStatement("SELECT * FROM User WHERE read_only=? AND type_id>? ORDER BY username LIMIT 20,10;");
		verify(ps, times(2)).setObject(1, "true");
		verify(ps, times(2)).setObject(2, "10");
	}

	@Test
	public void update() throws SQLException {
		when(rs.next()).thenReturn(true);

		daoSql.update(User.class, 123, user -> user.toBuilder().setTypeId(10).setReadOnly(true).build());

		verify(connection, times(2)).prepareStatement("SELECT * FROM User WHERE id=?;");
		verify(connection).prepareStatement("UPDATE User SET type_id=?, read_only=? WHERE id=?;");
		verify(ps).setObject(1, 10);
		verify(ps).setObject(2, true);
		verify(ps).setLong(3, 123);
	}

	@Test
	public void delete() throws SQLException {
		when(ps.executeUpdate()).thenReturn(1);
		daoSql.delete(User.class, 123);

		verify(connection).prepareStatement("DELETE FROM User WHERE id=?;");
		verify(ps).setLong(1, 123);
	}

	@Test
	public void batchDelete() throws SQLException {
		when(ps.executeUpdate()).thenReturn(5);
		daoSql.delete(User.class, ImmutableList.of(123L, 456L, 789L));
		verify(connection).prepareStatement("DELETE FROM User WHERE id IN (123,456,789);");
	}
}
