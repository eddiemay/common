package com.digitald4.common.dao.sql;

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

import com.digitald4.common.dao.DAO;
import com.digitald4.common.dao.QueryParam;
import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnector;
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
	
	private final T t;
	private final Descriptor descriptor; 
	private final DBConnector connector;
	private final String table;
	
	public DAOProtoSQLImpl(T t, DBConnector connector) {
		this.t = t;
		this.descriptor = t.getDescriptorForType();
		this.connector = connector;
		this.table = t.getDescriptorForType().getName();
	}

	public String getTable() {
		return table;
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
	public T read(int id) throws DD4StorageException {
		String sql = SELECT_SQL.replaceAll("\\{TABLE\\}", getTable());
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
			throw new DD4StorageException("Error reading record", e);
		}
	}
	
	@Override
	public List<T> query(QueryParam... params) throws DD4StorageException {
		List<QueryParam> list = new ArrayList<>();
		for (QueryParam param : params) {
			list.add(param);
		}
		return query(list);
	}
	
	@Override
	public List<T> query(List<QueryParam> params) throws DD4StorageException {
		String columns = "";
		for (QueryParam param : params) {
			if (columns.length() > 0) {
				columns += " AND ";
			}
			columns += param.getColumn() + " " + param.getOperan() + " ?";
		}
		String sql = QUERY_SQL.replaceAll("\\{TABLE\\}", getTable())
				.replaceAll("\\{COLUMNS\\}", columns);
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
			throw new DD4StorageException("Error reading record", e);
		}
	}
	
	@Override
	public List<T> getAll() throws DD4StorageException {
		String sql = GET_ALL_SQL.replaceAll("\\{TABLE\\}", getTable());
		System.out.println(sql);
		try (Connection con = connector.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();) {
			return process(rs);
		} catch (SQLException e) {
			throw new DD4StorageException("Error reading record", e);
		}
	}
	
	@Override
	public T update(int id, Function<T, T> updater) throws DD4StorageException {
		T orig = read(id);
		T updated = updater.execute(orig);
		String sets = "";
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
			return updated;
		} catch (SQLException e) {
			throw new DD4StorageException("Error updating record", e);
		}
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
		if (field != null) {
			switch (field.getJavaType()) {
				case ENUM: ps.setObject(index, ((EnumValueDescriptor) value).getNumber()); break;
				case LONG: ps.setTimestamp(index, new Timestamp((Long) value)); break;
				case MESSAGE: {
					if (field.isRepeated()) {
						StringBuffer json = new StringBuffer()
							.append("{\"" + field.getName() + "\": [");
						for (Message message : (List<Message>) value) {
							json.append(JsonFormat.printToString(message));
							json.append(",");
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
		Message.Builder builder = t.newBuilderForType();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String columnName = rsmd.getColumnName(i).toLowerCase();
			Object value = rs.getObject(i);
			if (value != null) {
				data.append(columnName + ": " + value + "\n");
				try {
					FieldDescriptor field = descriptor.findFieldByName(columnName); 
					if (field.getJavaType() == JavaType.ENUM) {
						value = field.getEnumType().findValueByNumber(rs.getInt(i));
					} else if (field.getJavaType() == JavaType.LONG) {
						value = rs.getTimestamp(i).getTime();
					} else if (field.getJavaType() == JavaType.MESSAGE) {
						Message.Builder subBuilder = field.getMessageType().toProto().newBuilderForType();
						JsonFormat.merge(rs.getString(i), subBuilder);
						value = subBuilder;
					}
					builder.setField(field, value);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(e.getMessage() + " for column: " + columnName);
				}
			}
		}
		System.out.println(data);
		return (T) builder.build();
	}
}
