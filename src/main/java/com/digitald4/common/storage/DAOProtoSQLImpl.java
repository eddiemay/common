package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.RetryableFunction;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class DAOProtoSQLImpl<T extends GeneratedMessageV3> implements DAO<T> {
	private static final String INSERT_SQL = "INSERT INTO {TABLE}({COLUMNS}) VALUES({VALUES});";
	private static final String SELECT_SQL = "SELECT * FROM ";
	private static final String WHERE_ID = " WHERE id=?;";
	private static final String UPDATE_SQL = "UPDATE {TABLE} SET {SETS}" + WHERE_ID;
	private static final String DELETE_SQL = "DELETE FROM {TABLE}" + WHERE_ID;
	private static final String WHERE_SQL = " WHERE ";
	private static final String ORDER_BY_SQL = " ORDER BY ";
	private static final String LIMIT_SQL = " LIMIT ";
	private static final String COUNT_SQL = "SELECT COUNT(*) FROM ";

	private final T type;
	private final Descriptor descriptor; 
	private final DBConnector connector;
	private final String table;
	private final String view;
	
	public DAOProtoSQLImpl(Class<T> c, DBConnector connector) {
		this(c, connector, null, null);
	}

	public DAOProtoSQLImpl(Class<T> c, DBConnector connector, String view) {
		this(c, connector, view, null);
	}

	public DAOProtoSQLImpl(Class<T> c, DBConnector connector, String view, String table) {
		try {
			this.type = (T) c.getMethod("getDefaultInstance").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		this.descriptor = type.getDescriptorForType();
		this.connector = connector;
		this.table = table != null ? table : descriptor.getName();
		this.view = view != null ? view : this.table;
	}
	
	@Override
	public T getType() {
		return type;
	}

	public String getTable() {
		return table;
	}
	
	public String getView() {
		return view;
	}
	
	public DBConnector getConnector() {
		return connector;
	}
	
	@Override
	public T create(T t) throws DD4StorageException {
		return CREATE_FUNC.applyWithRetries(t);
	}
	
	@Override
	public T get(int id) {
		return GET_BY_ID_FUNC.applyWithRetries(id);
	}
	
	@Override
	public ListResponse<T> list(ListRequest listRequest) {
		return GET_COLL_FUNC.applyWithRetries(listRequest);
	}
	
	@Override
	public T update(int id, UnaryOperator<T> updater) {
		return UPDATE_FUNC.applyWithRetries(Pair.of(id, updater));
	}
	
	@Override
	public void delete(int id) {
		DELETE_FUNC.applyWithRetries(id);
	}
	
	private void setObject(PreparedStatement ps, int index, T t, FieldDescriptor field, Object value)
			throws SQLException {
		if ("".equals(value)) {
			value = null;
		}
		if (field != null) {
			if (field.isRepeated() || field.isMapField()) {
				JSONObject json = new JSONObject(JsonFormat.printer().print(t));
				ps.setString(index, json.get(field.getName()).toString());
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
						ps.setTimestamp(index, new Timestamp((Long.valueOf(value.toString()))));
						break;
					case MESSAGE:
						ps.setString(index, JsonFormat.printer().print((Message) value));
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
	
	protected List<T> process(ResultSet rs) throws SQLException {
		List<T> results = new ArrayList<T>();
		while (rs.next()) {
			results.add(parseFromResultSet(rs));
		}
		return results;
	}
	
	private T parseFromResultSet(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		Message.Builder builder = type.newBuilderForType();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String columnName = rsmd.getColumnName(i).toLowerCase();
			Object value = rs.getObject(i);
			if (value != null) {
				try {
					FieldDescriptor field = descriptor.findFieldByName(columnName);
					if (field == null) {
						field = descriptor.findFieldByName(columnName.substring(0, columnName.length() - 2));
					}
					if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
						JsonFormat.parser().ignoringUnknownFields()
								.merge("{\"" + field.getName() + "\": " + rs.getString(i) + "}", builder);
					} else if (field.getJavaType() == JavaType.ENUM) {
						value = field.getEnumType().findValueByNumber(rs.getInt(i));
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.LONG) {
						value = rs.getTimestamp(i).getTime();
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

	private final RetryableFunction<T, T> CREATE_FUNC = new RetryableFunction<T, T>() {
		@Override
		public T apply(T t) {
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
			String sql = INSERT_SQL.replaceAll("\\{TABLE\\}", getTable())
					.replaceAll("\\{COLUMNS\\}", columns)
					.replaceAll("\\{VALUES\\}", values);
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				int index = 1;
				for (Map.Entry<FieldDescriptor, Object> entry : valueMap.entrySet()) {
					setObject(ps, index++, t, entry.getKey(), entry.getValue());
				}
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					return get(rs.getInt(1));
				}
				return t;
			} catch (SQLException e) {
				throw new RuntimeException("Error creating record: " + e.getMessage(), e);
			}
		}
	};

	private final RetryableFunction<Integer, T> GET_BY_ID_FUNC = new RetryableFunction<Integer, T>() {
		@Override
		public T apply(Integer id) {
			String sql = SELECT_SQL + getView() + WHERE_ID;
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql);) {
				ps.setInt(1, id);
				T result = null;
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					result = parseFromResultSet(rs);
				}
				rs.close();
				return result;
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		}
	};

	private final RetryableFunction<ListRequest, ListResponse<T>> GET_COLL_FUNC =
			new RetryableFunction<ListRequest, ListResponse<T>>() {
		@Override
		public ListResponse<T> apply(ListRequest listRequest) {
			StringBuffer sql = new StringBuffer(SELECT_SQL).append(getView());
			String where = "";
			String countSql = null;
			if (listRequest.getFilterCount() > 0) {
				sql.append(where = WHERE_SQL + String.join(" AND ", listRequest.getFilterList().stream()
						.map(filter -> filter.getColumn() + " " + (filter.getOperan().isEmpty() ? "=" : filter.getOperan()) + " ?")
						.collect(Collectors.toList())));
			}
			if (listRequest.getOrderByCount() > 0) {
				sql.append(ORDER_BY_SQL).append(String.join(",", listRequest.getOrderByList().stream()
						.map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
						.collect(Collectors.toList())));
			}
			if (listRequest.getPageSize() > 0) {
				sql.append(LIMIT_SQL)
						.append((listRequest.getPageToken() > 0 ? listRequest.getPageSize() + "," : "")
						+ String.valueOf(listRequest.getPageSize()));
				countSql = COUNT_SQL + getView() + where + ";";
			}
			sql.append(";");
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql.toString())) {
				int p = 1;
				for (Filter filter : listRequest.getFilterList()) {
					setObject(ps, p++, null, descriptor.findFieldByName(filter.getColumn()), filter.getValue());
				}
				ResultSet rs = ps.executeQuery();
				List<T> results = process(rs);
				rs.close();
				int count = results.size();
				if (countSql != null) {
					PreparedStatement ps2 = con.prepareStatement(countSql);
					p = 1;
					for (Filter filter : listRequest.getFilterList()) {
						setObject(ps2, p++, null, descriptor.findFieldByName(filter.getColumn()), filter.getValue());
					}
					rs = ps2.executeQuery();
					if (rs.next()) {
						count = rs.getInt(1);
					}
					rs.close();
					ps2.close();
				}
				return ListResponse.<T>newBuilder()
						.addAllResult(results)
						.setTotalSize(count)
						.build();
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		}
	};

	private final RetryableFunction<Pair<Integer, UnaryOperator<T>>, T> UPDATE_FUNC =
			new RetryableFunction<Pair<Integer, UnaryOperator<T>>, T>() {
		@Override
		public T apply(Pair<Integer, UnaryOperator<T>> pair) {
			int id = pair.getLeft();
			UnaryOperator<T> updater = pair.getRight();
			T orig = get(id);
			T updated = updater.apply(orig);
			String sets = "";

			// Find all the fields that were modified in the updated proto.
			Map<FieldDescriptor, Object> valueMap = updated.getAllFields();
			List<Map.Entry<FieldDescriptor, Object>> modified = new ArrayList<>();
			for (Map.Entry<FieldDescriptor, Object> entry : valueMap.entrySet()) {
				FieldDescriptor field = entry.getKey();
				if (!valueMap.get(field).equals(orig.getField(field))) {
					if (sets.length() > 0) {
						sets += ", ";
					}
					modified.add(entry);
					sets += field.getName() + " = ?";
				}
			}

			// Find all the fields that have been removed from the update set them to null.
			for (FieldDescriptor field : orig.getAllFields().keySet()) {
				if (!valueMap.containsKey(field)) {
					if (sets.length() > 0) {
						sets += ", ";
					}
					sets += field.getName() + " = NULL";
				}
			}
			if (sets.isEmpty()) {
				System.out.println("Nothing changed, returning");
			} else {
				String sql = UPDATE_SQL.replaceAll("\\{TABLE\\}", getTable())
						.replaceAll("\\{SETS\\}", sets);
				try (Connection con = connector.getConnection();
						 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
					int index = 1;
					for (Map.Entry<FieldDescriptor, Object> entry : modified) {
						setObject(ps, index++, updated, entry.getKey(), entry.getValue());
					}
					ps.setInt(index, id);
					ps.executeUpdate();
				} catch (Exception e) {
					throw new RuntimeException("Error updating record " + updated + ": " + e.getMessage(), e);
				}
			}
			return get(id);
		}
	};

	private final RetryableFunction<Integer, Boolean> DELETE_FUNC = new RetryableFunction<Integer, Boolean>() {
		@Override
		public Boolean apply(Integer id) {
			String sql = DELETE_SQL.replaceAll("\\{TABLE\\}", getTable());
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql);) {
				ps.setInt(1, id);
				return ps.executeUpdate() > 0;
			} catch (SQLException e) {
				throw new RuntimeException("Error deleting record: " + e.getMessage(), e);
			}
		}
	};
}
