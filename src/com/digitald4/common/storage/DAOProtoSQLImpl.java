package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.QueryParam;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.RetryableFunction;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

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
import org.json.JSONObject;

public class DAOProtoSQLImpl<T extends GeneratedMessage> implements DAO<T> {
	private static final String INSERT_SQL = "INSERT INTO {TABLE}({COLUMNS}) VALUES({VALUES});";
	private static final String SELECT_SQL = "SELECT * FROM {TABLE} WHERE id=?;";
	private static final String UPDATE_SQL = "UPDATE {TABLE} SET {SETS} WHERE id=?;";
	private static final String DELETE_SQL = "DELETE FROM {TABLE} WHERE id=?;";
	private static final String QUERY_SQL = "SELECT * FROM {TABLE} WHERE {COLUMNS};";
	private static final String GET_ALL_SQL = "SELECT * FROM {TABLE};";
	
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
	public List<T> get(QueryParam... params) throws DD4StorageException {
		List<QueryParam> list = new ArrayList<>();
		for (QueryParam param : params) {
			list.add(param);
		}
		return get(list);
	}
	
	@Override
	public List<T> get(List<QueryParam> params) {
		return GET_COLL_FUNC.applyWithRetries(params);
	}
	
	@Override
	public List<T> getAll() throws DD4StorageException {
		return GET_ALL_FUNC.applyWithRetries(true);
	}
	
	@Override
	public T update(int id, UnaryOperator<T> updater) {
		return UPDATE_FUNC.applyWithRetries(Pair.of(id, updater));
	}
	
	@Override
	public boolean delete(int id) {
		return DELETE_FUNC.applyWithRetries(id);
	}
	
	private void setObject(PreparedStatement ps, int index, T t, FieldDescriptor field, Object value)
			throws SQLException {
		if ("".equals(value)) {
			value = null;
		}
		if (field != null) {
			if (field.isRepeated() || field.isMapField()) {
				JSONObject json = new JSONObject(JsonFormat.printToString(t));
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
						ps.setString(index, JsonFormat.printToString((Message) value));
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
						JsonFormat.merge("{\"" + field.getName() + "\": " + rs.getString(i) + "}", builder);
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

	private final RetryableFunction<T, T> CREATE_FUNC =  new RetryableFunction<T, T>() {
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
			String sql = SELECT_SQL.replaceAll("\\{TABLE\\}", getView());
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql);) {
				ps.setInt(1, id);
				T result = null;
				ResultSet rs = ps.executeQuery();
				List<T> results = process(rs);
				if (!results.isEmpty()) {
					result = results.get(0);
				}
				rs.close();
				return result;
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		}
	};

	private final RetryableFunction<List<QueryParam>, List<T>> GET_COLL_FUNC =
			new RetryableFunction<List<QueryParam>, List<T>>() {
		@Override
		public List<T> apply(List<QueryParam> params) {
			String columns = "";
			for (QueryParam param : params) {
				if (columns.length() > 0) {
					columns += " AND ";
				}
				columns += param.getColumn() + " " + param.getOperan() + " ?";
			}
			String sql = QUERY_SQL.replaceAll("\\{TABLE\\}", getView())
					.replaceAll("\\{COLUMNS\\}", columns);
			if (params.size() == 0) {
				sql = sql.replaceAll(" WHERE ", "");
			}
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql)) {
				int p = 1;
				for (QueryParam param : params) {
					setObject(ps, p++, null, descriptor.findFieldByName(param.getColumn()), param.getValue());
				}
				ResultSet rs = ps.executeQuery();
				List<T> results = process(rs);
				rs.close();
				return results;
			} catch (SQLException e) {
				throw new RuntimeException("Error reading record: " + e.getMessage(), e);
			}
		}
	};

	private final RetryableFunction<Boolean, List<T>> GET_ALL_FUNC = new RetryableFunction<Boolean, List<T>>() {
		@Override
		public List<T> apply(Boolean Void) {
			String sql = GET_ALL_SQL.replaceAll("\\{TABLE\\}", getView());
			try (Connection con = connector.getConnection();
					 PreparedStatement ps = con.prepareStatement(sql);
					 ResultSet rs = ps.executeQuery();) {
				return process(rs);
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
				String sql = UPDATE_SQL.replaceAll("\\{TABLE\\}", getView())
						.replaceAll("\\{SETS\\}", sets);
				try (Connection con = connector.getConnection();
						 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
					int index = 1;
					for (Map.Entry<FieldDescriptor, Object> entry : modified) {
						setObject(ps, index++, updated, entry.getKey(), entry.getValue());
					}
					ps.setInt(index, id);
					ps.executeUpdate();
				} catch (SQLException e) {
					throw new RuntimeException("Error updating record: " + e.getMessage(), e);
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
