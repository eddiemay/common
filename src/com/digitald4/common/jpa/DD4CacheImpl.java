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
import java.util.Vector;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.joda.time.DateTime;

import com.digitald4.common.jdbc.DD4Hashtable;
import com.digitald4.common.log.EspLogger;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Retryable;

public class DD4CacheImpl implements DD4Cache {
	private DD4EntityManagerFactory emf;
	private DD4Hashtable<Class<?>,DD4Hashtable<String,Object>> hashById = new DD4Hashtable<Class<?>,DD4Hashtable<String,Object>>(199);
	private Hashtable<Class<?>,PropertyCollectionFactory<?>> propFactories = new Hashtable<Class<?>,PropertyCollectionFactory<?>>();

	public DD4CacheImpl(DD4EntityManagerFactory emf){
		this.emf = emf;
	}
	
	@SuppressWarnings("rawtypes")
	public boolean contains(Class c, Object o) {
		DD4Hashtable<String, Object> classHash = hashById.get(c);
		if (classHash != null)
			return classHash.containsKey(((Entity)o).getHashKey());
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public void evict(Class c) {
		hashById.remove(c);
		propFactories.remove(c);
	}
	
	@SuppressWarnings("rawtypes")
	public void evict(Class c, Object o) {
		DD4Hashtable<String,Object> classHash = hashById.get(c);
		if(classHash != null)
			classHash.remove(((Entity)o).getHashKey());
		@SuppressWarnings("unchecked")
		PropertyCollectionFactory<Object> pcf = getPropertyCollectionFactory(false, c);
		if(pcf!=null)
			pcf.evict(o);
	}
	
	public void evictAll() {
		hashById.clear();
		propFactories.clear();
	}
	
	@Override
	public <T> T find(Class<T> c, PrimaryKey pk) throws Exception {
		T o = getCachedObj(c, pk);
		if (o == null) {
			fetch(c, pk);
			o = getCachedObj(c, pk);
		}
		return o;
	}
	
	@Override
	public <T> void reCache(T o) {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>)o.getClass();
		PropertyCollectionFactory<T> pcf = getPropertyCollectionFactory(false, c);
		if (pcf != null) {
			pcf.evict(o);
			pcf.cache(o);
		}
	}
	
	public <T> List<T> find(DD4TypedQueryImpl<T> tq) throws Exception {
		List<T> list = getCachedList(false,tq.getTypeClass(),tq);
		if(list == null){
			fetch(tq);
			list = getCachedList(true,tq.getTypeClass(),tq);
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getCachedObj(Class<T> c, Object pk){
		DD4Hashtable<String, Object> classHash = hashById.get(c);
		if (classHash == null) 
			return null;
		return (T)classHash.get(((Entity)pk).getHashKey());
	}
	
	public <T> List<T> getCachedList(boolean create, Class<T> c, DD4TypedQueryImpl<T> tq) throws Exception{
		PropertyCollectionFactory<T> pcf = getPropertyCollectionFactory(create, c);
		if(pcf!=null)
			return pcf.getList(create,tq);
		return null;
	}
	
	public <T> PropertyCollectionFactory<T> getPropertyCollectionFactory(boolean create, Class<T> c){
		@SuppressWarnings("unchecked")
		PropertyCollectionFactory<T> pcf = (PropertyCollectionFactory<T>)propFactories.get(c);
		if(pcf == null && create){
			pcf = new PropertyCollectionFactory<T>();
			propFactories.put(c, pcf);
		}
		return pcf;
	}
	
	private <T> void fetch(Class<T> c, PrimaryKey pk) throws Exception{
		String query = getQuery(c);
		if (query == null)
			EspLogger.error(this, "Query findById is null for "+c.getSimpleName());
		if (emf == null)
			EspLogger.error(this, "emf is null");
		Connection con = emf.getConnection();
		if(con == null)
			EspLogger.error(this, "connection is null");
		//EspLogger.debug(this, query);
		PreparedStatement ps = null;
		ResultSet rs = null;
		long sTime = System.currentTimeMillis();
		try {
			ps = con.prepareStatement(query);
			setPSKeys(ps, query, pk.getKeys());
			rs = ps.executeQuery();
			while (rs.next()) {
				// T o = c.newInstance();
				T o = c.getConstructor(EntityManager.class).newInstance(emf.createEntityManager());
				refresh(o, rs);
				if (!contains(c, o))
					put(o);
			}
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs!=null)
				rs.close();
			if (ps != null)
				ps.close();
			EspLogger.message(this, query + pk + " " + (System.currentTimeMillis() - sTime) + "ms");
			con.close();
		}
	}
	private <T> void fetch(DD4TypedQueryImpl<T> tq) throws Exception {
		new Retryable<Boolean, DD4TypedQueryImpl<T>>() {
			public Boolean execute(DD4TypedQueryImpl<T> tq) throws Exception {
				Class<T> c = tq.getTypeClass();
				String query = null;
				String jpql = null;
				if (tq.getName()!=null){
					query = getNamedNativeQuery(tq.getName()+"_FETCH", c);
					if (query == null)
						jpql = getNamedQuery(tq.getName()+"_FETCH",c);
				}
				if (query == null){
					if (jpql==null)
						jpql = tq.getQuery();
					query = convertJPQL2SQL(tq.getTypeClass(), jpql);
				}
				long sTime = System.currentTimeMillis();
				//EspLogger.debug(this, query);
				Connection con = emf.getConnection();
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					ps = con.prepareStatement(query);
					setPSKeys(ps, query, tq.getParameterValues());
					rs = ps.executeQuery();
					while (rs.next()) {
						// T o = c.newInstance();
						T o = c.getConstructor(EntityManager.class).newInstance(emf.createEntityManager());
						refresh(o, rs);
						if (contains(c, o)) {
							o = getCachedObj(c, o);
						} else {
							put(o);
						}
						put(o, tq);
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
					EspLogger.message(this, query + (tq.getParameterValues().length > 0 ? tq.getParameterValues()[0] : "") +
							" " + (System.currentTimeMillis() - sTime) + "ms");
					con.close();
				}
				return true;
			}
		}.run(tq);
	}
	
	public <T> void refresh(T o) throws Exception{
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>)o.getClass();
		String query = "SELECT * FROM "+c.getAnnotation(Table.class).name()+" WHERE ";
		ArrayList<Object> values = new ArrayList<Object>();
		for(Method m:c.getMethods()){
			Id id = m.getAnnotation(Id.class);
			if(id!=null){
				if(values.size()!=0)
					query+=" AND ";
				if(m.getReturnType()==Calendar.class)
					query+="TO_CHAR("+m.getAnnotation(Column.class).name()+",'YYYY-MM-DD')=?";
				else
					query+=m.getAnnotation(Column.class).name()+"=?";
				values.add(m.invoke(o));
			}
		}
		EspLogger.message(this, query);
		Connection con = emf.getConnection();
		PreparedStatement ps = con.prepareStatement(query);
		setPSKeys(ps,query,values.toArray());
		ResultSet rs = null;
		try{
			rs = ps.executeQuery();
			if(rs.next()){
				refresh(o,rs);
				if(!contains(c, o))
					put(o);
				PropertyCollectionFactory<T> pcf = getPropertyCollectionFactory(false, c);
				if(pcf!=null){
					pcf.evict(o);
					pcf.cache(o);
				}
			}
		}catch(Exception e){
			throw e;
		}finally{
			if(rs!=null)
				rs.close();
			if (ps != null)
				ps.close();
			con.close();
		}
	}
	
	public <T >void put(T o){
		DD4Hashtable<String, Object> classHash = hashById.get(o.getClass());
		if (classHash == null) {
			classHash = new DD4Hashtable<String,Object>(199);
			hashById.put(o.getClass(), classHash);
		}
		classHash.put(((Entity)o).getHashKey(), o);
	}
	
	private <T> void put(T o, DD4TypedQueryImpl<T> tq) throws Exception{
		PropertyCollectionFactory<T> pcf = getPropertyCollectionFactory(true, tq.getTypeClass());
		pcf.cache(o, tq);
	}
	
	private <T> String getQuery(Class<T> c){
		String query = getNamedNativeQuery("findByID_FETCH",c);
		if(query != null)
			return query;
		String jpql = getNamedQuery("findByID_FETCH",c);
		if(jpql == null)
			jpql = getNamedQuery("findByID",c);
		return convertJPQL2SQL(c,jpql);
	}
	
	public <T> String getNamedQuery(String namedQuery, Class<T> c){
		NamedQueries namedQueries = c.getAnnotation(NamedQueries.class);
		if(namedQueries==null)
			return null;
		for(NamedQuery nq :namedQueries.value())
			if(nq.name().equalsIgnoreCase(namedQuery))
				return nq.query();
		return null;
	}
	
	public <T> String getNamedNativeQuery(String name, Class<T> c){
		NamedNativeQueries namedQueries = c.getAnnotation(NamedNativeQueries.class);
		if(namedQueries==null)
			return null;
		for(NamedNativeQuery nq :namedQueries.value())
			if(nq.name().equalsIgnoreCase(name))
				return nq.query();
		return null;
	}

	@Override
	public <T> TypedQuery<T> createQuery(String query, Class<T> c) { 
		return new DD4TypedQueryImpl<T>(this, query, query, c);
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> c) { 
		String query = getNamedQuery(name, c);
		if(query == null)
			query = getNamedQuery(name, c.getSuperclass());
		return new DD4TypedQueryImpl<T>(this, name, query, c);
	}
	
	private <T> String convertJPQL2SQL(Class<T> c, String query){
		String cq = query.replaceFirst("o", "o.*");
		cq = cq.replaceFirst(c.getSimpleName(), c.getAnnotation(Table.class).name());
		for(int x=1; x<10; x++)
			cq = cq.replace("?"+x, "?");
		return cq;
	}
	private void setPSKeys(PreparedStatement ps, String query, Object... keys) throws SQLException{
		if(query.toUpperCase().contains("WHERE")){
			int p=1;
			for (Object key:keys){ 
				if (key instanceof Calendar) {
					if (query.toUpperCase().contains("DATE"))
						ps.setObject(p++, FormatText.formatDate((Calendar)key, FormatText.MYSQL_DATE));
					else
						ps.setObject(p++, new Timestamp(((Calendar)key).getTimeInMillis()));
				} else if (key instanceof DateTime) {
					ps.setObject(p++, FormatText.formatDate((DateTime)key, FormatText.MYSQL_DATE));
				}else if (key != null)
					ps.setObject(p++, key);
			}
		}
	}
	public void refresh(Object o, ResultSet rs) throws Exception {
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
	Hashtable<String,PropCPU> propCPUs = new Hashtable<String,PropCPU>(); 
	public PropCPU getPropCPU(Object o, String prop){
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
					setMethod = o.getClass().getMethod("set" + upperCamel,getMethod.getReturnType());
					pc.javaType = getMethod.getReturnType();
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
	public static boolean isNull(Object value){
		if(value == null) return true;
		if(value instanceof Number)
			return ((Number)value).doubleValue()==0.0;
		return false;
	}
	
	public <T> void persist(T o) throws Exception {
		new Retryable<Boolean, T>() {
			@Override
			public Boolean execute(T o) throws Exception {
				@SuppressWarnings("unchecked")
				Class<T> c = (Class<T>)o.getClass();
				String table = c.getAnnotation(Table.class).name();
				persist(o, c, table);
				return true;
			}
		}.run(o);
	}
	
	private <T> void persist(T o, Class<T> c, String table) throws Exception {
		String query = "INSERT INTO "+table+"(";
		String values = "";
		ArrayList<KeyValue> propVals = new ArrayList<KeyValue>();
		HashMap<String,Method> gKeys = new HashMap<String,Method>();
		for(Method m:c.getMethods()){
			Column col = m.getAnnotation(Column.class);
			if(col != null){
				Object value = m.invoke(o);
				if(!isNull(value)){
					if(values.length() > 0){
						query+=",";
						values+=",";
					}
					query+=col.name();
					values+="?";
					propVals.add(new KeyValue(col.name(),value));
				}
				else if(m.getAnnotation(SequenceGenerator.class)!=null){
					SequenceGenerator sq = m.getAnnotation(SequenceGenerator.class);
					if(values.length() > 0){
						query+=",";
						values+=",";
					}
					query+=col.name();
					values+=sq.sequenceName()+".NEXTVAL";
					gKeys.put(col.name(),c.getMethod("set"+FormatText.toUpperCamel(col.name()),m.getReturnType()));
				}
				else if(m.getAnnotation(GeneratedValue.class)!=null)
					gKeys.put(col.name(),c.getMethod("set"+FormatText.toUpperCamel(col.name()),m.getReturnType()));
			}
		}
		query+=") VALUES("+values+")";
		String printQ = query+"\n(";
		Connection con = emf.getConnection();
		PreparedStatement ps = con.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
		int i=1;
		for(KeyValue kv:propVals){
			setPSValue(ps,i++,kv.getName(),kv.getValue());
			if(kv.getValue() instanceof Calendar)
				printQ+=FormatText.formatDate((Calendar)kv.getValue())+",";
			else
				printQ+=kv.getValue()+",";
		}
		printQ+=")";
		EspLogger.message(this,printQ);
		try{
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
		put(o);
		PropertyCollectionFactory<T> pcf = getPropertyCollectionFactory(false, c);
		if(pcf!=null){
			pcf.cache(o);
		}
	}
	
	protected void processGenKeysOracle(HashMap<String,Method> gKeys, PreparedStatement ps, String table, Object o) throws Exception {
		if (gKeys.size() > 0) {
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				String gCols = "";
				for (String gk:gKeys.keySet()) {
					if(gCols.length()>0)
						gCols+=",";
					gCols+=gk;
				}
				Connection con = emf.getConnection();
				PreparedStatement ps2 = null;
				try {
					ps2 = con.prepareStatement("SELECT "+gCols+" FROM "+table+" WHERE ROWID=?");
					ps2.setString(1,rs.getString(1));
					rs.close();
					rs = ps2.executeQuery();
					if (rs.next()) {
						for (String gk:gKeys.keySet()) {
							gKeys.get(gk).invoke(o, rs.getInt(gk));
						}
					}
				} catch (Exception e) {
					throw e;
				} finally {
					if (rs != null) {
						rs.close();
					}
					if (ps2 != null) {
						ps2.close();
					}
					con.close();
				}
			}
		}
	}
	
	protected void processGenKeysMySQL(HashMap<String,Method> gKeys, PreparedStatement ps, Object o) throws Exception {
		if(gKeys.size()>0){
			ResultSet rs = ps.getGeneratedKeys();
			if(rs.next()){
				int i=1;
				for(String gk:gKeys.keySet())
					gKeys.get(gk).invoke(o, rs.getInt(i++));
				rs.close();
			}
		}
	}
	
	public <T> void remove(T o) throws Exception {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>)o.getClass();
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
				propVals.add(new KeyValue(m.getAnnotation(Column.class).name(),m.invoke(o)));
			}
		}
		query += where;
		String printQ = query+"\n(";
		Connection con = emf.getConnection();
		PreparedStatement ps = null;
		try {
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
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
			con.close();
		}
		evict(c, o);
	}
	
	public <T> T merge(T o) throws Exception {
		return new Retryable<T, T>() {
			@Override
			public T execute(T o) throws Exception {
				if (o instanceof ChangeLog) {
					return merge(o, ((ChangeLog)o).getChanges());
				}
				Collection<Change> changes = new Vector<Change>();
				@SuppressWarnings("unchecked")
				Class<T> c = (Class<T>)o.getClass();
				for (Method m : c.getMethods()) {
					Column col = m.getAnnotation(Column.class);
					if (col != null) {
						changes.add(new Change(col.name(), m.invoke(o), null));
					}
				}
				return merge(o, changes);
			}
		}.run(o);
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
			for(Change change:changes)
				setPSValue(ps,i++,change.getProperty(),change.getNewValue());
			for(KeyValue kv:propVals){
				setPSValue(ps,i++,kv.getName(),kv.getValue());
				if(kv.getValue() instanceof Calendar)
					printQ+=FormatText.formatDate((Calendar)kv.getValue())+",";
				else
					printQ+=kv.getValue()+",";
			}
			printQ+=")";
			EspLogger.message(this,printQ);
			ps.executeUpdate();
		} catch (Exception e) {
			throw e;
		}
		finally {
			if (ps != null) {
				ps.close();
			}
			con.close();
		}
		PropertyCollectionFactory<T> pcf = getPropertyCollectionFactory(false, c);
		if (pcf!=null) {
			pcf.evict(o);
			pcf.cache(o);
		}
		return o;
	}
	
	public static void setPSValue(PreparedStatement ps, int index, String colName, Object value)throws SQLException{
		if (value instanceof Integer) {
			if((Integer)value!=0)
				ps.setInt(index,(Integer)value);
			else
				ps.setObject(index, null);
		} else if (value instanceof Double)
			ps.setDouble(index,(Double)value);
		else if(value instanceof Boolean)
			ps.setBoolean(index,(Boolean)value);
		else if(value instanceof Calendar) {
			if(colName.toUpperCase().contains("DATE"))
				ps.setDate(index, new Date(((Calendar)value).getTimeInMillis()));
			else 
				ps.setTimestamp(index, new Timestamp(((Calendar)value).getTimeInMillis()));
		} else if (value instanceof Time)
			ps.setTime(index, (Time)value);
		else if (value instanceof DateTime)
			ps.setTimestamp(index, new Timestamp(((DateTime)value).getMillis()));
		else if (value instanceof String)
			ps.setString(index,(String)value);
		else if (value instanceof byte[])
			ps.setBinaryStream(index,new java.io.ByteArrayInputStream((byte[])value),((byte[])value).length);
		else
			ps.setObject(index,value);
	}
	private class KeyValue{
		private String name;
		private Object value;
		public KeyValue(String name, Object value){
			this.name = name;
			this.value = value;
		}
		public String getName(){
			return name;
		}
		public Object getValue(){
			return value;
		}
	}
}
