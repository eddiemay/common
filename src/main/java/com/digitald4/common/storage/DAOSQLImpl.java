package com.digitald4.common.storage;

import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.joining;

import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.UnaryOperator;
import org.json.JSONObject;

public class DAOSQLImpl implements TypedDAO<Message> {
	private static final String INSERT_SQL = "INSERT INTO %s(%s) VALUES(%s);";
	private static final String SELECT_SQL = "SELECT * FROM %s WHERE id=?;";
	private static final String SEARCH_SQL = "SELECT * FROM %s%s%s%s;";
	private static final String UPDATE_SQL = "UPDATE %s SET %s WHERE id=?;";
	private static final String DELETE_SQL = "DELETE FROM %s%s;";
	private static final String BATCH_DELETE_SQL = "DELETE FROM %s WHERE id IN (%s);";
	private static final String LIMIT_SQL = " LIMIT %s%d";
	private static final String COUNT_SQL = "SELECT COUNT(*) FROM %s%s;";

	private final DBConnector connector;
	private final boolean useViews;

	public DAOSQLImpl(DBConnector connector) {
		this(connector, false);
	}

	public DAOSQLImpl(DBConnector connector, boolean useViews) {
		this.connector = connector;
		this.useViews = useViews;
	}

	@Override
	public <T extends Message> T create(T t) {
		return Calculate.executeWithRetries(2, () -> {
			String columns = "";
			String values = "";
			Map<FieldDescriptor, Object> valueMap = t.getAllFields();
			for (FieldDescriptor field : valueMap.keySet()) {
				if (columns.length() > 0) {
					columns += ",";
					values += ",";
				}
				columns += field.getName();
				values += "?";
			}
			String sql = String.format(INSERT_SQL, t.getClass().getSimpleName(), columns, values);
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				int index = 1;
				for (Map.Entry<FieldDescriptor, Object> entry : valueMap.entrySet()) {
					setObject(ps, index++, t, entry.getKey(), entry.getValue());
				}
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					return get((Class<T>) t.getClass(), rs.getInt(1));
				}
				return t;
			} catch (SQLException e) {
				throw new RuntimeException("Error creating record: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <T extends Message> T get(Class<T> c, long id) {
		return Calculate.executeWithRetries(2, () -> {
			String sql = String.format(SELECT_SQL, getView(c));
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setLong(1, id);
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
	public <T extends Message> QueryResult<T> list(Class<T> c, Query query) {
		return Calculate.executeWithRetries(2, () -> {
			String where = query.getFilters().isEmpty() ? "" :
					query.getFilters().stream()
							.map(filter -> filter.getColumn() + filter.getOperator() + "?")
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
			Descriptor descriptor = ProtoUtil.getDefaultInstance(c).getDescriptorForType();
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				int p = 1;
				for (Query.Filter filter : query.getFilters()) {
					setObject(ps, p++, null, descriptor.findFieldByName(filter.getColumn()), filter.getValue());
				}
				ResultSet rs = ps.executeQuery();
				List<T> results = process(c, rs);
				int totalSize = results.size();
				rs.close();
				if (countSql != null) {
					PreparedStatement ps2 = con.prepareStatement(countSql);
					p = 1;
					for (Query.Filter filter : query.getFilters()) {
						setObject(ps2, p++, null, descriptor.findFieldByName(filter.getColumn()), filter.getValue());
					}
					rs = ps2.executeQuery();
					if (rs.next()) {
						totalSize = rs.getInt(1);
					}
					rs.close();
					ps2.close();
				}
				return new QueryResult<>(results, totalSize);
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public <T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return Calculate.executeWithRetries(2, () -> {
			T orig = get(c, id);
			T updated = updater.apply(orig);

			// Find all the fields that were modified in the updated proto.
			Map<FieldDescriptor, Object> valueMap = updated.getAllFields();
			List<Map.Entry<FieldDescriptor, Object>> modified = new ArrayList<>();
			String sets = valueMap.entrySet().stream()
					.map(entry -> {
						FieldDescriptor field = entry.getKey();
						if (!valueMap.get(field).equals(orig.getField(field))) {
							modified.add(entry);
							return field.getName() + "=?";
						}

						return null;
					})
					.filter(Objects::nonNull)
					.collect(joining(", "));

			// Find all the fields that have been removed from the update set them to null.
			for (FieldDescriptor field : orig.getAllFields().keySet()) {
				if (!valueMap.containsKey(field)) {
					if (sets.length() > 0) {
						sets += ", ";
					}
					sets += field.getName() + "=NULL";
				}
			}

			if (sets.isEmpty()) {
				throw new RuntimeException("Nothing changed, returning");
			} else {
				String sql = String.format(UPDATE_SQL, getTable(c), sets);
				try (Connection con = connector.getConnection();
						 PreparedStatement ps = con.prepareStatement(sql)) {
					int index = 1;
					for (Map.Entry<FieldDescriptor, Object> entry : modified) {
						setObject(ps, index++, updated, entry.getKey(), entry.getValue());
					}
					ps.setLong(index, id);
					ps.executeUpdate();
				} catch (Exception e) {
					throw new RuntimeException("Error updating record " + updated + ": " + e.getMessage(), e);
				}
			}
			return get(c, id);
		});
	}

	@Override
	public <T extends Message> void delete(Class<T> c, long id) {
		if (!Calculate.executeWithRetries(2, () -> {
			String sql = String.format(DELETE_SQL, getTable(c),  " WHERE id=?");
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setLong(1, id);

				return ps.executeUpdate() > 0;
			} catch (SQLException e) {
				throw new RuntimeException("Error deleting record: " + e.getMessage(), e);
			}
		})) {
			throw new RuntimeException("Error deleting record: " + c.getSimpleName() + " " + id);
		}
	}

	@Override
	public <T extends Message> int delete(Class<T> c, Iterable<Long> ids) {
		return Calculate.executeWithRetries(2, () -> {
			String sql = String.format(BATCH_DELETE_SQL, getView(c), stream(ids).map(String::valueOf).collect(joining(",")));
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				return ps.executeUpdate();
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

	private <T extends Message> void setObject(
			PreparedStatement ps, int index, T t, FieldDescriptor field, Object value) throws SQLException {
		if ("".equals(value)) {
			value = null;
		}
		if (field != null) {
			if (field.isRepeated() || field.isMapField()) {
				JSONObject json = new JSONObject(ProtoUtil.print(t));
				ps.setString(index, json.get(FormatText.toLowerCamel(field.getName())).toString());
			} else {
				switch (field.getJavaType()) {
					case ENUM:
						if (value instanceof EnumValueDescriptor) {
							ps.setObject(index, ((EnumValueDescriptor) value).getNumber());
						} else {
							ps.setObject(index, Integer.valueOf(value.toString()));
						}
						break;
					case LONG:
						if (field.getName().endsWith("id")) {
							ps.setObject(index, value);
						} else {
							ps.setTimestamp(index, new Timestamp((Long.parseLong(value.toString()))));
						}
						break;
					case MESSAGE:
						ps.setString(index, ProtoUtil.print((Message) value));
						break;
					default:
						ps.setObject(index, value);
				}
			}
		} else {
			System.out.println("******************************************Field is null for: " + value);
			ps.setObject(index, value);
		}
	}

	private <T extends Message> List<T> process(Class<T> c, ResultSet rs) throws SQLException {
		List<T> results = new ArrayList<>();
		while (rs.next()) {
			results.add(parseFromResultSet(c, rs));
		}
		return results;
	}

	private <T extends Message> T parseFromResultSet(Class<T> c, ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		Message.Builder builder = ProtoUtil.getDefaultInstance(c).toBuilder();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String columnName = rsmd.getColumnName(i).toLowerCase();
			Object value = rs.getObject(i);
			if (value != null) {
				try {
					FieldDescriptor field = builder.getDescriptorForType().findFieldByName(columnName);
					if (field == null) {
						field = builder.getDescriptorForType().findFieldByName(columnName.substring(0, columnName.length() - 2));
					}
					if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
						JsonFormat.parser().ignoringUnknownFields()
								.merge("{\"" + field.getName() + "\": " + rs.getString(i) + "}", builder);
					} else if (field.getJavaType() == JavaType.ENUM) {
						value = field.getEnumType().findValueByNumber(rs.getInt(i));
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.LONG) {
						if (!columnName.endsWith("id")) {
							value = rs.getTimestamp(i).getTime();
						}
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.BYTE_STRING) {
						builder.setField(field, ByteString.copyFrom(rs.getBytes(i)));
					} else {
						builder.setField(field, value);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					System.out.println(e.getMessage() + " for column: " + columnName + ". value: " + value);
				}
			}
		}
		return (T) builder.build();
	}
}
