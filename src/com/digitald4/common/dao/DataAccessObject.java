/**
 *           | Master Data Interface Version 2.0 |
 *
 * Copyright (c) 2006, Southern California Edison, Inc.
 * 					   Distribution Staff Engineering Team.
 * 	                   All rights reserved.
 *
 * This software has been developed exclusively for internal usage.
 * Unauthorized use is prohibited.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.digitald4.common.dao;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Clob;
import java.sql.Time;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import com.digitald4.common.jpa.Change;
import com.digitald4.common.jpa.ChangeLog;
import com.digitald4.common.jpa.Entity;
import com.digitald4.common.jpa.PrimaryKey;
import com.digitald4.common.model.GenData;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.TransHist;
import com.digitald4.common.model.User;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;

/**
 * Data Access Object
 *
 * @author Eddie Mayfield
 */
public abstract class DataAccessObject extends Observable implements Comparable<Object>, ChangeLog, Entity {
	private final EntityManager entityManager;
	private HashMap<String, Change> changes;

	/**
	 * Creates a new instance of DBObject.
	 */
	public DataAccessObject(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * @param orig The original object to get data from 
	 */
	public DataAccessObject(EntityManager entityManager, DataAccessObject orig) {
		this.entityManager = entityManager;
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}

	public abstract Integer getId();

	/**
	 * Checks if is new instance.
	 *
	 * @return true, if is new instance
	 */
	public boolean isNewInstance() {
		EntityManager entityManager = getEntityManager();
		return entityManager == null || !entityManager.contains(this);
	}
	
	public <E> List<E> getCollection(Class<E> c, String[] props, Object... values) {
		return getCollection(c, getEntityManager(), props, values);
	}
	
	public static <E> List<E> getCollection(Class<E> c, EntityManager entityManager,
			String[] props, Object... values) {
		String qlString = "SELECT o FROM " + c.getSimpleName() + " o";
		if (props != null && props.length > 0) {
			qlString += " WHERE";
			int p = 0;
			for (String prop:props) {
				if (p > 0) {
					qlString += " AND";
				}
				if (values[p] == null) {
					qlString += " o." + prop + " IS NULL";
				} else {
					qlString += " o." + prop + " = ?" + (p + 1);
				}
				p++;
			}
		}
		return getCollection(c, entityManager, qlString, values);
	}
	
	public <E> List<E> getCollection(Class<E> c, String jpql, Object... values) {
		return getCollection(c, getEntityManager(), jpql, values);
	}
	
	public static <E> List<E> getCollection(Class<E> c, EntityManager entityManager,
			String jpql, Object... values) {
		TypedQuery<E> tq = entityManager.createQuery(jpql, c);
		if (values != null && values.length > 0) {
			int p = 1;
			for (Object value : values) {
				if (value != null) {
					tq = tq.setParameter(p++, value);
				}
			}
		}
		return tq.getResultList();
	}
	
	public <E> List<E> getNamedCollection(Class<E> c, String name, Object... values) {
		return getNamedCollection(c, entityManager, name, values);
	}
	
	public static <E> List<E> getNamedCollection(Class<E> c, EntityManager entityManager,
			String name, Object... values) {
		TypedQuery<E> tq = entityManager.createNamedQuery(name, c);
		if (values != null && values.length > 0) {
			int p = 1;
			for (Object value : values) {
				if (value != null) {
					tq = tq.setParameter(p++, value);
				}
			}
		}
		return tq.getResultList();
	}
	
	public static <E> List<E> getAll(Class<E> c, EntityManager entityManager) {
		return getNamedCollection(c, entityManager, "findAll");
	}
	
	public static <E> List<E> getAllActive(Class<E> c, EntityManager entityManager) {
		return getNamedCollection(c, entityManager, "findAllActive");
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 *
	 * @return the int
	 */
	public int compareTo(Object o) {
		//If this is the same exact object in memory then just say so
		if (this == o)
			return 0;
		if (o instanceof DataAccessObject)
			return (toString() + getHashKey()).compareTo(o.toString() + ((DataAccessObject)o).getHashKey());
		return 0;
	}

	/**
	 * Gets the hash key.
	 *
	 * @return the hash key
	 */
	public abstract String getHashKey();

	/**
	 * Refresh.
	 *
	 * @return true, if refresh
	 *
	 * @throws SQLException the SQL exception
	 */
	public void refresh() {
		getEntityManager().refresh(this);
	}

	public Collection<Change> getChanges(){
		return changes.values();
	}

	public void addChange(String prop, Object newValue, Object oldValue){
		if(changes == null)
			changes = new HashMap<String,Change>();
		changes.put(prop, new Change(prop, newValue, oldValue));
	}

	protected void setProperty(String prop, Object newValue, Object oldValue) {
		if (prop==null) return;
		if (isNewInstance()) return;
		addChange(prop, newValue, oldValue);
	}
	
	private void logTracking(GeneralData transType) throws Exception {
		TransHist th = new TransHist(getEntityManager()).setType(transType).setObject(getClass().getSimpleName())
				.setRowId(getId()).setTimestamp(DateTime.now()).setUser(User.getActiveUser(getEntityManager()));
		if (changes != null) {
			StringBuffer data = new StringBuffer();
			for (Change change : changes.values()) {
				if (data.length() > 0) {
					data.append(",");
				}
				data.append(change.getProperty() + "[" + change.getOldValue() + "|" + change.getNewValue() + "]");
			}
			th.setData(data.toString());
		}
		getEntityManager().persist(th);
	}

	/**
	 * @throws Exception 
	 */
	public synchronized DataAccessObject save() throws Exception {
		if (isNewInstance()) {
			insert();
		} else if (changes != null && changes.size() > 0) {
			try {
				getEntityManager().merge(this);
				logTracking(GenData.TransType_Update.get(getEntityManager()));
			} catch (Exception e) {
				e.printStackTrace();
				//refresh();
				throw e;
			} finally {
				changes.clear();
			}
		}
		return this;
	}

	public void delete() throws Exception {
		getEntityManager().remove(this);
		logTracking(GenData.TransType_Delete.get(getEntityManager()));
	}

	public void insertParents() throws Exception{
	}

	/**
	 * Insert.
	 * @throws Exception 
	 *
	 * @throws SQLException the SQL exception
	 */
	public void insert() throws Exception {
		insertPreCheck();
		insertParents();
		if (isNewInstance()) {
			getEntityManager().persist(this);
			logTracking(GenData.TransType_Insert.get(getEntityManager()));
		} else {
			save();
		}
		insertChildren();
	}

	public abstract void insertPreCheck() throws Exception;

	public void insertChildren() throws Exception { 	
	}

	public static boolean isSame(Object o, Object o2){
		return Calculate.isSame(o, o2);
	}

	/**
	 * Returns a string hash code of an object of this type with the
	 * specified parameters. The hash code would be used to find the
	 * object in a hash table.
	 *
	 * @param id - id for the object
	 * @param planyear - planYear for the object
	 * @param k1 the k1
	 *
	 * @return a string hash code of an object of this type with the specified parameters.
	 */
	public static String getHashKey(Object[] keys){
		return PrimaryKey.getHashKey(keys);
	}

	public static String getHashKey(Object key){
		return PrimaryKey.getHashKey(key);
	}

	public static boolean isNull(Object... keys){
		for(Object k:keys)
			if(Calculate.isNull(k))
				return true;
		return false;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	public String toString(){
		return getHashKey();
	}

	/**
	 * @param dao The object to compare to. 
	 */
	public Vector<String> getDifference(DataAccessObject dao){
		return new Vector<String>();
	}

	/**
	 * @param cp The object to copy children to 
	 */
	public void copyChildrenTo(DataAccessObject cp){
	}

	public String formatProperty(String colname) {
		// Convert from lowerCamel to underscored.
		if (Character.isLowerCase(colname.charAt(0))) {
			colname = FormatText.toSpaced(colname).replaceAll(" ", "_");
		}
		colname = colname.toUpperCase();
		if (colname.contains(".")) {
			colname = colname.substring(colname.lastIndexOf('.') + 1);
		}
		return colname;
	}

	public abstract Object getPropertyValue(String colName);

	public abstract DataAccessObject setPropertyValue(String colName, String value) throws Exception;

	public static DataAccessObject get(JSONObject json) throws Exception {
		String className = json.getString("className");
		Class<?> c = Class.forName(className);
		if (json.has("id")) {
			return (DataAccessObject) c.getMethod("getInstance", Integer.class).invoke(null, json.getInt("id"));
		}
		return (DataAccessObject) c.newInstance();
	}

	public static DataAccessObject updateFromJSON(DataAccessObject dao, JSONObject json) throws Exception {
		Class<? extends DataAccessObject> c = dao.getClass();
		for (String fieldName : JSONObject.getNames(json)) {
			try {
				Field field = c.getSuperclass().getDeclaredField(fieldName);
				Object value = getValue(json.get(fieldName), field.getType());
				if (value != null)
					c.getMethod("set"+fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), field.getType()).invoke(dao, value);
			} catch (NoSuchFieldException e) {
				//Ignore and just move on
				//if (!fieldName.startsWith("$$"))
				//throw e;
			}
		}
		return dao;
	}

	private static Object getValue(Object value, Class<?> javaType) throws ParseException{
		if (javaType == String.class)
			return value.toString();
		if (javaType == int.class || javaType == Integer.class)
			return Integer.valueOf(value.toString());
		if (javaType == long.class)
			return Long.valueOf(value.toString());
		if (javaType == Clob.class)
			return value.toString();
		if (javaType == double.class)
			return Double.parseDouble(value.toString());
		if (javaType == boolean.class)
			return Boolean.valueOf(value.toString());
		if (javaType == Date.class)
			return FormatText.parseDate(value.toString());
		if (javaType == Time.class)
			return FormatText.parseTime(value.toString());
		if (javaType == DateTime.class)
			return new DateTime(value.toString());
		return null;
	}

	public static JSONObject toJSON(DataAccessObject dao) throws JSONException {
		JSONObject json = new JSONObject()
				.put("className", dao.getClass().getName());
		for (Field field : dao.getClass().getSuperclass().getDeclaredFields()) {
			try {
				Method method = null;
				try {
					method = dao.getClass().getMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
				} catch (NoSuchMethodException e) {
					method = dao.getClass().getMethod("is" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
				}
				if (field.getType() != List.class) {
					json.put(field.getName(), method.invoke(dao));
				}
			} catch (Exception e1) {
			}
		}
		return json;
	}

	public JSONObject toJSON() throws JSONException {
		return toJSON(this);
	}

	public void update(JSONObject json) throws Exception {
		updateFromJSON(this, json);
	}
}
