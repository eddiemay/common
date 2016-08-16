package com.digitald4.common.storage;

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

import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnector;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.QueryParam;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

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
		System.out.println(sql);
		try (Connection con = connector.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			int index = 1;
			for (Map.Entry<FieldDescriptor, Object> entry : valueMap.entrySet()) {
				setObject(ps, index++, entry.getKey(), entry.getValue());
			}
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				t = (T) t.toBuilder()
						.setField(t.getDescriptorForType().findFieldByName("id"), rs.getInt(1))
						.build();
			}
			rs.close();
			return t;
		} catch (SQLException e) {
			throw new DD4StorageException("Error creating record", e);
		}
	}
	
	@Override
	public T get(int id) throws DD4StorageException {
		String sql = SELECT_SQL.replaceAll("\\{TABLE\\}", getView());
		System.out.println(sql);
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
			throw new DD4StorageException("Error reading record: " + e.getMessage(), e);
		}
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
	public List<T> get(List<QueryParam> params) throws DD4StorageException {
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
		System.out.println(sql);
		try (Connection con = connector.getConnection();
				PreparedStatement ps = con.prepareStatement(sql)) {
			int p = 1;
			for (QueryParam param : params) {
				setObject(ps, p++, descriptor.findFieldByName(param.getColumn()), param.getValue());
			}
			ResultSet rs = ps.executeQuery();
			List<T> results = process(rs);
			rs.close();
			return results;
		} catch (SQLException e) {
			throw new DD4StorageException("Error reading record: " + e.getMessage(), e);
		}
	}
	
	@Override
	public List<T> getAll() throws DD4StorageException {
		String sql = GET_ALL_SQL.replaceAll("\\{TABLE\\}", getView());
		System.out.println(sql);
		try (Connection con = connector.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();) {
			return process(rs);
		} catch (SQLException e) {
			throw new DD4StorageException("Error reading record: " + e.getMessage(), e);
		}
	}
	
	@Override
	public T update(int id, Function<T, T> updater) throws DD4StorageException {
		T orig = get(id);
		T updated = updater.execute(orig);
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
			System.out.println(sql);
			try (Connection con = connector.getConnection();
					PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				int index = 1;
				for (Map.Entry<FieldDescriptor, Object> entry : modified) {
					System.out.println("Setting: " + entry.getKey().getName());
					setObject(ps, index++, entry.getKey(), entry.getValue());
				}
				ps.setInt(index, id);
				ps.executeUpdate();
			} catch (SQLException e) {
				throw new DD4StorageException("Error updating record", e);
			}
		}
		return updated;
	}
	
	@Override
	public void delete(int id) throws DD4StorageException {
		String sql = DELETE_SQL.replaceAll("\\{TABLE\\}", getTable());
		System.out.println(sql);
		try (Connection con = connector.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DD4StorageException("Error deleting record", e);
		}
	}
	
	static void setObject(PreparedStatement ps, int index, FieldDescriptor field, Object value)
			throws SQLException {
		if ("".equals(value)) {
			value = null;
		}
		if (field != null) {
			switch (field.getJavaType()) {
				case ENUM: if (value instanceof EnumValueDescriptor) {
					ps.setObject(index, ((EnumValueDescriptor) value).getNumber());
				} else {
					ps.setObject(index, Integer.valueOf(value.toString()));
				} break;
				case LONG: ps.setTimestamp(index, new Timestamp((Long.valueOf(value.toString())))); break;
				case MESSAGE: {
					if (field.isRepeated()) {
						StringBuffer json = new StringBuffer()
							.append("{\"" + field.getName() + "\": [");
						boolean first = true;
						for (Message message : (List<Message>) value) {
							if (!first) {
								json.append(",");
							} else {
								first = false;
							}
							json.append(JsonFormat.printToString(message));
						}
						json.append("]}");
						System.out.println("JSON: " + json.toString());
						ps.setString(index, json.toString());
					} else {
						ps.setString(index, JsonFormat.printToString((Message) value));
					}
				} break;
				default: ps.setObject(index, value);
			}
		} else {
			System.out.println("******************************************Field is null for: " + value);
			ps.setObject(index, value);
		}
	}
	
	protected List<T> process(ResultSet rs) throws SQLException {
		List<T> results = new ArrayList<T>();
		while (rs.next()) {
			results.add(parseFromRS(rs));
		}
		return results;
	}
	
	private T parseFromRS(ResultSet rs) throws SQLException {
		StringBuffer data = new StringBuffer("");
		ResultSetMetaData rsmd = rs.getMetaData();
		Message.Builder builder = type.newBuilderForType();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String columnName = rsmd.getColumnName(i).toLowerCase();
			Object value = rs.getObject(i);
			if (value != null) {
				data.append(columnName + ": " + value + "\n");
				try {
					FieldDescriptor field = descriptor.findFieldByName(columnName);
					if (field == null) {
						field = descriptor.findFieldByName(columnName.substring(0, columnName.length() - 2));
					}
					if (field.getJavaType() == JavaType.ENUM) {
						value = field.getEnumType().findValueByNumber(rs.getInt(i));
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.LONG) {
						value = rs.getTimestamp(i).getTime();
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.MESSAGE) {
						JsonFormat.merge(rs.getString(i), builder);
					} else {
						builder.setField(field, value);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					System.out.println(e.getMessage() + " for column: " + columnName);
				}
			}
		}
		System.out.println(data);
		return (T) builder.build();
	}
}
