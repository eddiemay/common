package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.joining;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.JSONUtil;
import com.digitald4.common.util.JSONUtil.Field;
import com.google.appengine.api.datastore.Text;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class DAOSQLImpl implements DAO {
	private static final String INSERT_SQL = "INSERT INTO %s(%s) VALUES(%s);";
	private static final String SELECT_SQL = "SELECT * FROM %s WHERE id=?;";
	private static final String BATCH_SELECT_SQL = "SELECT * FROM %s WHERE id IN (%s);";
	private static final String SEARCH_SQL = "SELECT * FROM %s%s%s%s;";
	private static final String UPDATE_SQL = "UPDATE %s SET %s%s%s WHERE id=?;";
	private static final String BATCH_DELETE_SQL = "DELETE FROM %s WHERE id IN (%s);";
	private static final String LIMIT_SQL = " LIMIT %s%d";
	private static final String COUNT_SQL = "SELECT COUNT(*) FROM %s%s;";

	private final DBConnector connector;
	private final ChangeTracker changeTracker;
	private final boolean useViews;

	public DAOSQLImpl(DBConnector connector, ChangeTracker changeTracker) {
		this(connector, changeTracker, false);
	}

	public DAOSQLImpl(DBConnector connector, ChangeTracker changeTracker, boolean useViews) {
		this.connector = connector;
		this.changeTracker = changeTracker;
		this.useViews = useViews;
	}

	@Override
	public <T> Transaction<T> persist(Transaction<T> transaction) {
		changeTracker.prePersist(this, transaction);
		transaction.getOps().forEach(op -> {
			if (op.getUpdater() == null) {
				op.setEntity(create(op.getEntity()));
			} else {
				op.setEntity(update(op));
			}
		});
		changeTracker.postPersist(this, transaction);
		return transaction;
	}

	@Override
	public <T> T create(T t) {
		return Calculate.executeWithRetries(2, () -> {
			JSONObject json = JSONUtil.toJSON(t);
			ImmutableList<String> keys = stream(json.keys())
					.filter(key -> {
						Object value = json.get(key);
						return !(value instanceof Long && value.equals(0L) || value instanceof Integer && value.equals(0));
					})
					.collect(toImmutableList());
			String sql = String.format(INSERT_SQL, t.getClass().getSimpleName(),
					String.join(",", keys),
					keys.stream().map(key -> "?").collect(joining(","))); // ? value placeholder for each column.
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				int index = 1;
				for (String key : keys) {
					setObject(ps, index++, json.get(key));
				}
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					return get((Class<T>) t.getClass(), rs.getObject(1));
				}
				return t;
			} catch (SQLException e) {
				throw new RuntimeException("Error creating record: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <T, I> T get(Class<T> c, I id) {
		return Calculate.executeWithRetries(2, () -> {
			String sql = String.format(SELECT_SQL, getView(c));
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setObject(1, id);
				T result = null;
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					result = parseFromResultSet(c, rs);
				}
				rs.close();
				return result;
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <T, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids) {
		return Calculate.executeWithRetries(2, () -> {
			String sql = String.format(BATCH_SELECT_SQL, getView(c), stream(ids).map(String::valueOf).collect(joining(",")));
			try (Connection con = connector.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
				ImmutableList.Builder<T> results = ImmutableList.builder();
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					results.add(parseFromResultSet(c, rs));
				}
				rs.close();
				return BulkGetable.MultiListResult.of(results.build(), ids);
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query.List query) {
		return Calculate.executeWithRetries(2, () -> {
			String where = query.getFilters().isEmpty() ? "" :
					query.getFilters().stream()
							.map(
									filter -> String.format("%s%s",
											filter.getColumn(),
											filter.getOperator().equals("IN") ? " IN (?)" : filter.getOperator() + "?"))
							.collect(joining(" AND ", " WHERE ", ""));
			String orderBy = query.getOrderBys().isEmpty() ? "" :
					query.getOrderBys().stream()
							.map(ob -> ob.getColumn() + (ob.getDesc() ? " DESC" : ""))
							.collect(joining(", ", " ORDER BY ", ""));
			String limit = "";
			String countSql = null;
			if (query.getLimit() > 0) {
				limit = String.format(LIMIT_SQL, query.getOffset() > 0 ? query.getOffset() + "," : "", query.getLimit());
				countSql = String.format(COUNT_SQL, getView(c), where);
			}

			String sql = String.format(SEARCH_SQL, getView(c), where, orderBy, limit);
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				int p = 1;
				for (Query.Filter filter : query.getFilters()) {
					Object value = filter.getValue();
					if (value instanceof Collection) {
						value = value.toString().substring(1, value.toString().length() - 1).replace(" ", "");
					}
					setObject(ps, p++, value);
				}
				ResultSet rs = ps.executeQuery();
				List<T> results = process(c, rs);
				int totalSize = results.size();
				rs.close();
				if (countSql != null) {
					PreparedStatement ps2 = con.prepareStatement(countSql);
					p = 1;
					for (Query.Filter filter : query.getFilters()) {
						setObject(ps2, p++, filter.getValue());
					}
					rs = ps2.executeQuery();
					if (rs.next()) {
						totalSize = rs.getInt(1);
					}
					rs.close();
					ps2.close();
				}
				return QueryResult.of(c, results, totalSize, query);
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
		throw new DD4StorageException("Unimplemented method", ErrorCode.BAD_REQUEST);
	}

	private <T, I> T update(Op<T> op) {
		return Calculate.executeWithRetries(2, () -> {
			JSONObject origJson = JSONUtil.toJSON(op.getCurrent());
			JSONObject updated = JSONUtil.toJSON(op.getEntity());

			// Find all the fields that were modified in the updated object.
			ImmutableList<String> modified = updated.keySet().stream()
					.filter(key -> !updated.get(key).equals(origJson.opt(key)))
					.collect(toImmutableList());

			// Find all the fields that have been removed from the update set them to null.
			ImmutableList<String> cleared = origJson.keySet().stream()
					.filter(key -> !updated.has(key))
					.collect(toImmutableList());

			if (modified.isEmpty() && cleared.isEmpty()) {
				throw new RuntimeException("Nothing changed, returning");
			} else {
				String sql = String.format(UPDATE_SQL, getTable(op.getTypeClass()),
						modified.stream().map(key -> key + "=?").collect(joining(", ")),
						!modified.isEmpty() && !cleared.isEmpty() ? ", " : "",
						cleared.stream().map(key -> key + "=NULL").collect(joining(",")));
				try (Connection con = connector.getConnection();
						 PreparedStatement ps = con.prepareStatement(sql)) {
					int index = 1;
					for (String key : modified) {
						setObject(ps, index++, updated.get(key));
					}
					ps.setObject(index, origJson.get("id"));
					ps.executeUpdate();
				} catch (Exception e) {
					throw new RuntimeException("Error updating record " + updated + ": " + e.getMessage(), e);
				}
			}
			return op.getEntity();
		});
	}

	@Override
	public <T, I> boolean delete(Class<T> c, I id) {
		return delete(c, ImmutableList.of(id)) == 1;
	}

	@Override
	public <T, I> int delete(Class<T> c, Iterable<I> ids) {
		return Calculate.executeWithRetries(2, () -> {
			changeTracker.preDelete(this, c, ids);
			String sql = String.format(
					BATCH_DELETE_SQL, getView(c), stream(ids).map(String::valueOf).collect(joining(",")));
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				int deleted = ps.executeUpdate();
				changeTracker.postDelete(c, ids);
				return deleted;
			} catch (SQLException e) {
				throw new RuntimeException("Error deleting record: " + e.getMessage(), e);
			}
		});
	}

	private String getTable(Class<?> c) {
		return c.getSimpleName();
	}

	private String getView(Class<?> c) {
		return getTable(c) + (useViews ? "View" : "");
	}

	private void setObject(PreparedStatement ps, int index, Object value) throws SQLException {
		if ("".equals(value)) {
			value = null;
		}

		ps.setObject(index, value);
	}

	private <T> List<T> process(Class<T> c, ResultSet rs) throws SQLException {
		List<T> results = new ArrayList<>();
		while (rs.next()) {
			results.add(parseFromResultSet(c, rs));
		}
		return results;
	}

	private <T> T parseFromResultSet(Class<T> c, ResultSet rs) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();
		ImmutableMap<String, Field> fieldMap = JSONUtil.getFields(c);
		JSONObject jsonObject = new JSONObject();
		for (int i = 1; i <= metaData.getColumnCount(); i++) {
			String colName = metaData.getColumnName(i).toLowerCase();
			Object value = rs.getObject(i);
			if (value != null) {
				try {
					String javaName = FormatText.toLowerCamel(colName);
					Field field = fieldMap.get(javaName);
					if (field == null) {
						throw new DD4StorageException("Unknown field: " + colName + " for Object: " + c.getSimpleName());
					}

					if (field.getSetMethod() == null) {
						continue;
					}

					switch (field.getType().getSimpleName()) {
						case "ByteArray":
							jsonObject.put(javaName, value.toString().getBytes());
							break;
						case "DateTime":
							if (value instanceof Date) {
								jsonObject.put(javaName, ((Date) value).getTime());
							} else {
								jsonObject.put(javaName, (value instanceof Long) ? value : DateTime.parse((String) value).getMillis());
							}
							break;
						case "Instant":
							if (value instanceof Date) {
								jsonObject.put(javaName, ((Date) value).getTime());
							} else {
								jsonObject.put(javaName, (value instanceof Long) ? value : Instant.parse((String) value).toEpochMilli());
							}
							break;
						case "Integer":
						case "int":
							jsonObject.put(javaName, ((Long) value).intValue());
							break;
						case "Long":
						case "long":
							if (colName.endsWith("id")) {
								jsonObject.put(javaName, value);
							} else {
								jsonObject.put(javaName, value);
								// field.invokeSet(t, new java.sql.Timestamp((Long.parseLong(value.toString()))));
							}
							break;
						case "StringBuilder":
							jsonObject.put(javaName, (value instanceof Text) ? ((Text) value).getValue() : value);
							break;
						case "String":
						default:
							if (field.isCollection()) {
								jsonObject.put(javaName, new JSONArray((String) value));
							} else if (field.getType().isEnum()) {
								jsonObject.put(javaName, Enum.valueOf((Class<? extends Enum>) field.getType(), (String) value));
							} else {
								jsonObject.put(javaName, field.isObject() ? new JSONObject((String) value) : value);
							}
							break;
					}
				} catch (Exception e) {
					// e.printStackTrace();
					System.out.println(e.getMessage() + " for column: " + colName + ". value: " + value);
				}
			}
		}

		return JSONUtil.toObject(c, jsonObject);
	}
}
