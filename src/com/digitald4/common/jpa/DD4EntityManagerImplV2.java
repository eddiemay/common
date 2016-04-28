package com.digitald4.common.jpa;

import java.lang.reflect.Method;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.joda.time.DateTime;

import com.digitald4.common.log.EspLogger;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Retryable;

public class DD4EntityManagerImplV2 implements DD4EntityManager {
	
	private DD4EntityManagerFactory emf;
	private DD4Cache cache;

	public DD4EntityManagerImplV2(DD4EntityManagerFactory emf, DD4Cache cache) {
		this.emf = emf;
		this.cache = cache;
	}

	@Override	
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object entity) {
		return emf.getCache().contains(entity.getClass(), entity);
	}

	@Override
	public Query createNamedQuery(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> c) { 
		return emf.getCache().createNamedQuery(name, c);
	}

	@Override
	public Query createNativeQuery(String sqlString) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Query createNativeQuery(String arg0, Class c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Query createNativeQuery(String arg0, String arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Query createQuery(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> TypedQuery<T> createQuery(String jpql, Class<T> c) {
		return cache.createQuery(jpql, c);
	}

	@Override
	public void detach(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T find(Class<T> c, Object pk) {
		if (pk == null) {
			return null;
		}
		if (!(pk instanceof PrimaryKey)) {
			return find(c, new PrimaryKey<Object>(pk)); 
		} else {
			return find(c, (PrimaryKey<?>)pk);
		}
	}

	public <T> T find(Class<T> c, PrimaryKey<?> pk) {
		try {
			T o = emf.getCache().find(c, pk);
			if (o == null) {
				fetch(c, pk);
				o = emf.getCache().find(c, pk);
			}
			return o;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public <T> T find(Class<T> c, Object pk, Map<String, Object> arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T find(Class<T> c, Object pk, LockModeType mode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T find(Class<T> c, Object pk, LockModeType mode, Map<String, Object> arg3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() {
		throw new UnsupportedOperationException();
	}

	@Override	
	public CriteriaBuilder getCriteriaBuilder() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getDelegate() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DD4EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	@Override
	public FlushModeType getFlushMode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LockModeType getLockMode(Object arg0) {
		throw new UnsupportedOperationException();
	}
	
  // 266912: Criteria API and Metamodel API (See Ch 5 of the JPA 2.0 Specification)
  /** Reference to the Metamodel for this deployment and session. 
   * Please use the accessor and not the instance variable directly*/
	private Metamodel metaModel;
	
	public Metamodel getMetamodel() {
		 return metaModel;
	}

	@Override	
	public Map<String, Object> getProperties() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T getReference(Class<T> arg0, Object arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityTransaction getTransaction() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void joinTransaction() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void lock(Object arg0, LockModeType arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T merge(T o) {
		try {
			return new Retryable<T, T>() {
				@Override
				public T execute(T o) throws Exception {
					if (o instanceof ChangeLog) {
						return merge(o,((ChangeLog)o).getChanges());
					}
					Collection<Change> changes = new Vector<Change>();
					@SuppressWarnings("unchecked")
					Class<T> c = (Class<T>)o.getClass();
					for (Method m : c.getMethods()) {
						Column col = m.getAnnotation(Column.class);
						if (col != null) {
							changes.add(new Change(col.name(), m.invoke(o),null));
						}
					}
					return merge(o, changes);
				}
			}.run(o);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}
	
	@Override
	public void persist(Object o) {
		try {
			new Retryable<Boolean, Object>() {
				@Override
				public Boolean execute(Object o) throws Exception {
					Class<?> c = o.getClass();
					String table = c.getAnnotation(Table.class).name();
					persist(o, c, table);
					return true;
				}
			}.run(o);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private void persist(Object o, Class<?> c, String table) throws Exception {
		String query = "INSERT INTO " + table + "(";
		String values = "";
		ArrayList<KeyValue> propVals = new ArrayList<KeyValue>();
		HashMap<String, Method> gKeys = new HashMap<String, Method>();
		for (Method m : c.getMethods()) {
			Column col = m.getAnnotation(Column.class);
			if (col != null) {
				Object value = m.invoke(o);
				if (!Calculate.isNull(value)) {
					if (values.length() > 0) {
						query+=",";
						values+=",";
					}
					query += col.name();
					values += "?";
					propVals.add(new KeyValue(col.name(), value));
				} else if (m.getAnnotation(SequenceGenerator.class) != null) {
					SequenceGenerator sq = m.getAnnotation(SequenceGenerator.class);
					if (values.length() > 0) {
						query+=",";
						values+=",";
					}
					query += col.name();
					values += sq.sequenceName() + ".NEXTVAL";
					gKeys.put(col.name(), c.getMethod("set" + FormatText.toUpperCamel(col.name()), m.getReturnType()));
				} else if (m.getAnnotation(GeneratedValue.class) != null) {
					gKeys.put(col.name(), c.getMethod("set" + FormatText.toUpperCamel(col.name()), m.getReturnType()));
				}
			}
		}
		query += ") VALUES(" + values + ")";
		String printQ = query + "\n(";
		Connection con = emf.getConnection();
		PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		int i = 1;
		for (KeyValue kv:propVals) {
			setPSValue(ps,i++,kv.getName(),kv.getValue());
			if (kv.getValue() instanceof Calendar) {
				printQ += FormatText.formatDate((Calendar)kv.getValue()) + ",";
			} else {
				printQ+=kv.getValue()+",";
			}
		}
		printQ += ")";
		EspLogger.message(this, printQ);
		try {
			ps.executeUpdate();
			processGenKeysMySQL(gKeys, ps, o);
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (ps != null)
				ps.close();
			con.close();
		}
		cache.put(o);
		cache.reCache(o);
	}
	
	private void processGenKeysMySQL(HashMap<String,Method> gKeys, PreparedStatement ps, Object o) throws Exception {
		if (gKeys.size() > 0) {
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				int i = 1;
				for (String gk : gKeys.keySet()) {
					gKeys.get(gk).invoke(o, rs.getInt(i++));
				}
				rs.close();
			}
		}
	}

	@Override
	public void refresh(Object o) {
		Class<?> c = o.getClass();
		String query = "SELECT * FROM " + c.getAnnotation(Table.class).name() + " WHERE ";
		ArrayList<Object> values = new ArrayList<Object>();
		for (Method m : c.getMethods()) {
			Id id = m.getAnnotation(Id.class);
			if (id != null) {
				if (values.size() != 0) {
					query+=" AND ";
				}
				if (m.getReturnType() == Calendar.class) {
					query += "TO_CHAR(" + m.getAnnotation(Column.class).name() + ",'YYYY-MM-DD')=?";
				} else {
					query += m.getAnnotation(Column.class).name() + "=?";
				}
				try {
					values.add(m.invoke(o));
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		EspLogger.message(this, query);
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = emf.getConnection();
			ps = con.prepareStatement(query);
			setPSKeys(ps, query, values.toArray());
			rs = ps.executeQuery();
			if (rs.next()) {
				refresh(o,rs);
				if (!cache.contains(c, o)) {
					cache.put(o);
				}
				cache.reCache(o);
			}
		} catch(Exception e) {
			throw new IllegalArgumentException(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
				con.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void refresh(Object arg0, Map<String, Object> arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void refresh(Object arg0, LockModeType arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void remove(Object o) {
		Class<?> c = o.getClass();
		String table = c.getAnnotation(Table.class).name();
		String query = "DELETE FROM "+table+" WHERE ";
		String where="";
		ArrayList<KeyValue> propVals = new ArrayList<KeyValue>();
		for (Method m : c.getMethods()) {
			Id id = m.getAnnotation(Id.class);
			if (id != null) {
				if (where.length() > 0) {
					where += "AND ";
				}
				where += m.getAnnotation(Column.class).name()+"=?";
				try {
					propVals.add(new KeyValue(m.getAnnotation(Column.class).name(), m.invoke(o)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		query += where;
		String printQ = query+"\n(";
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = emf.getConnection();
			ps = con.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
			int i=1;
			for (KeyValue kv:propVals) {
				setPSValue(ps,i++,kv.getName(),kv.getValue());
				if (kv.getValue() instanceof Calendar)
					printQ+=FormatText.formatDate((Calendar)kv.getValue())+",";
				else
					printQ+=kv.getValue()+",";
			}
			EspLogger.message(this, printQ+")");
			ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();;
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				con.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cache.evict(c, o);
	}

	@Override
	public void setFlushMode(FlushModeType arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProperty(String arg0, Object arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> c) {
		try {
			// return c.newInstance();
			return c.getConstructor(EntityManager.class).newInstance(this);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	private <T> String getQuery(Class<T> c) {
		String query = EntityManagerHelper.getNamedNativeQuery("findByID_FETCH",c);
		if (query != null) {
			return query;
		}
		String jpql = EntityManagerHelper.getNamedQuery("findByID_FETCH",c);
		if (jpql == null) {
			jpql = EntityManagerHelper.getNamedQuery("findByID",c);
		}
		return EntityManagerHelper.convertJPQL2SQL(c, jpql);
	}
	
	private void setPSKeys(PreparedStatement ps, String query, Object... keys) throws SQLException{
		if (query.toUpperCase().contains("WHERE")) {
			int p = 1;
			for (Object key:keys) {
				if (key instanceof Calendar) {
					if (query.toUpperCase().contains("DATE")) {
						ps.setObject(p++, FormatText.formatDate((Calendar)key, FormatText.MYSQL_DATE));
					} else {
						ps.setObject(p++, new Timestamp(((Calendar)key).getTimeInMillis()));
					}
				} else if (key != null) {
					ps.setObject(p++, key);
				}
			}
		}
	}
	
	private <T> void fetch(Class<T> c, PrimaryKey<?> pk) throws Exception {
		String query = getQuery(c);
		if (query == null) {
			EspLogger.error(this, "Query findById is null for " + c.getSimpleName());
		}
		Connection con = emf.getConnection();
		if(con == null) {
			EspLogger.error(this, "connection is null");
		}
		//EspLogger.debug(this, query);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(query);
			setPSKeys(ps, query, pk.getKeys());
			rs = ps.executeQuery();
			while (rs.next()) {
				// T o = c.newInstance();
				T o = c.getConstructor(EntityManager.class).newInstance(this);
				refresh(o, rs);
				if (!emf.getCache().contains(c, o)) {
					emf.getCache().put(o);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			con.close();
		}
	}
	
	@Override
	public <T> List<T> fetchResults(DD4TypedQuery<T> tq) throws Exception {
		final EntityManager entityManager = this;
		return new Retryable<List<T>, DD4TypedQuery<T>>() {
			public List<T> execute(DD4TypedQuery<T> tq) throws Exception {
				List<T> results = new DD4SortedList<T>();
				Class<T> c = tq.getTypeClass();
				String sql = tq.getSql();
				
				EspLogger.message(this, sql + (tq.getParameters().size() > 0 ? tq.getParameterValue(1) : ""));
				//EspLogger.debug(this, query);
				Connection con = emf.getConnection();
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					ps = con.prepareStatement(sql);
					setPSKeys(ps, sql, tq.getParameterValues());
					rs = ps.executeQuery();
					while (rs.next()) {
						// T o = c.newInstance();
						T o = c.getConstructor(EntityManager.class).newInstance(entityManager);
						refresh(o, rs);
						if (cache.contains(c, o)) {
							o = cache.getCachedObj(c, o);
						} else {
							cache.put(o);
						}
						results.add(o);
					}
				} catch(Exception e) {
					throw e;
				} finally {
					if (rs != null) {
						rs.close();
					}
					if (ps != null) {
						ps.close();
					}
					con.close();
				}
				return results;
			}
		}.run(tq);
	}
	
	private void refresh(Object o, ResultSet rs) throws Exception {
		ResultSetMetaData md = rs.getMetaData();
		for (int c = 1; c <= md.getColumnCount(); c++) {
			PropCPU pc = getPropCPU(o, md.getColumnName(c));
			if (pc != null && pc.setMethod != null) {
				try {
					//EspLogger.debug(this, ""+setMethod);
					pc.setMethod.invoke(o, getValue(rs, c, md.getColumnName(c), pc.javaType));
				} catch(SQLException e) {
					EspLogger.error(this, "for: " + pc.javaType + " " + pc.setMethod);
					System.out.println("Error: " + pc.javaType + " " + pc.setMethod);
					throw e;
				}
			}
		}
	}
	
	private Hashtable<String,PropCPU> propCPUs = new Hashtable<String,PropCPU>(); 
	private PropCPU getPropCPU(Object o, String prop){
		String ss = o.getClass()+"."+prop;
		PropCPU pc = propCPUs.get(ss);
		if (pc == null) {
			pc = new PropCPU();
			propCPUs.put(ss, pc);
			Method getMethod = null;
			String upperCamel = FormatText.toUpperCamel(prop);
			try {
				getMethod = o.getClass().getMethod("get" + upperCamel);
			} catch(Exception e) {
			}
			if (getMethod == null) {
				try {
					getMethod = o.getClass().getMethod("is" + upperCamel);
				} catch(Exception e2) {
				}
			}
			if (getMethod != null) {
				Method setMethod = null;
				try {
					pc.javaType = getMethod.getReturnType();
					setMethod = o.getClass().getMethod("set" + upperCamel, pc.javaType);
					pc.setMethod = setMethod;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		return pc;
	}
	
	private class PropCPU {
		Class<?> javaType;
		Method setMethod;
	}
	
	private Object getValue(ResultSet rs, int col, String colName, Class<?> javaType) throws SQLException{
		if(javaType == String.class)
			return rs.getString(col);
		if(javaType == int.class || javaType == Integer.class)
			return rs.getInt(col);
		if (javaType == short.class)
			return rs.getShort(col);
		if(javaType == long.class)
			return rs.getLong(col);
		if(javaType == Clob.class)
			return rs.getClob(col);
		if(javaType == double.class)
			return rs.getDouble(col);
		if(javaType == boolean.class)
			return rs.getBoolean(col);
		if(javaType == Calendar.class){
			if(colName.toUpperCase().contains("DATE"))
				return getCalendar(rs.getDate(col));
			return getCalendar(rs.getTimestamp(col));
		}
		if(javaType == Time.class)
			return rs.getTime(col);
		if(javaType == Date.class)
			return rs.getDate(col);
		if (javaType == DateTime.class) {
			if (rs.getObject(col) == null) {
				return null;
			}
			return new DateTime(rs.getTimestamp(col));
		}
		return rs.getObject(col);
	}
	
	public static Calendar getCalendar(Date date){
		if(date == null)
			return null;
		Calendar cal = Calculate.getCal(2002, Calendar.APRIL, 8);
		cal.setTime(date);
		return cal;
	}
	
	public static Calendar getCalendar(Timestamp ts){
		if(ts == null)
			return null;
		Calendar cal = Calculate.getCal(2002, Calendar.APRIL, 8);
		cal.setTime(ts);
		return cal;
	}
	
	private <T> T merge(T o, Collection<Change> changes) throws Exception {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>)o.getClass();
		String table = c.getAnnotation(Table.class).name();
		String query = "UPDATE "+table+" SET ";
		String set="";
		for(Change change:changes){
			if(set.length() > 0)
				set+=", ";
			set+=change.getProperty()+"=?";
		}
		query += set;
		String where="";
		ArrayList<KeyValue> propVals = new ArrayList<KeyValue>();
		for(Method m:c.getMethods()){
			Id id = m.getAnnotation(Id.class);
			if(id != null){
				if(where.length() > 0)
					where += " AND ";
				where += m.getAnnotation(Column.class).name()+"=?";
				propVals.add(new KeyValue(m.getAnnotation(Column.class).name(),m.invoke(o)));
			}
		}
		query += " WHERE "+where;
		String printQ = query+"\n(";
		Connection con = emf.getConnection();
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(query);
			int i=1;
			for (Change change:changes) {
				setPSValue(ps, i++, change.getProperty(), change.getNewValue());
			}
			for (KeyValue kv:propVals) {
				setPSValue(ps, i++, kv.getName(), kv.getValue());
				if (kv.getValue() instanceof Calendar) {
					printQ+=FormatText.formatDate((Calendar)kv.getValue())+",";
				} else {
					printQ+=kv.getValue()+",";
				}
			}
			printQ+=")";
			EspLogger.message(this,printQ);
			ps.executeUpdate();
		} catch(Exception e) {
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
			con.close();
		}
		cache.reCache(o);
		return o;
	}
	
	private static void setPSValue(PreparedStatement ps, int index, String colName, Object value)throws SQLException{
		if(value instanceof Integer){
			if((Integer)value!=0)
				ps.setInt(index,(Integer)value);
			else
				ps.setObject(index, null);
		}
		else if(value instanceof Double)
			ps.setDouble(index,(Double)value);
		else if(value instanceof Boolean)
			ps.setBoolean(index,(Boolean)value);
		else if(value instanceof Calendar){
			if(colName.toUpperCase().contains("DATE"))
				ps.setDate(index, new Date(((Calendar)value).getTimeInMillis()));
			else 
				ps.setTimestamp(index, new Timestamp(((Calendar)value).getTimeInMillis()));
		}
		else if(value instanceof Time)
			ps.setTime(index, (Time)value);
		else if(value instanceof DateTime)
			ps.setTimestamp(index, new Timestamp(((DateTime)value).getMillis()));
		else if(value instanceof String)
			ps.setString(index,(String)value);
		else if(value instanceof byte[])
			ps.setBinaryStream(index,new java.io.ByteArrayInputStream((byte[])value),((byte[])value).length);
		else
			ps.setObject(index,value);
	}
	
	private class KeyValue {
		private String name;
		private Object value;
		
		public KeyValue(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public Object getValue() {
			return value;
		}
	}
}
