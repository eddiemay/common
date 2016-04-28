package com.digitald4.common.dao;

import com.digitald4.common.dao.DataAccessObject;
import com.digitald4.common.jpa.PrimaryKey;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.TransHist;
import com.digitald4.common.model.User;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.persistence.Cache;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.TypedQuery;
import org.joda.time.DateTime;

/** TODO Copy Right*/
/**Description of class, (we need to get this from somewhere, database? xml?)*/
public abstract class TransHistDAO extends DataAccessObject{
	public enum KEY_PROPERTY{ID};
	public enum PROPERTY{ID,TIMESTAMP,TYPE_ID,USER_ID,OBJECT,ROW_ID,DATA};
	private Integer id;
	private DateTime timestamp;
	private Integer typeId;
	private Integer userId;
	private String object;
	private Integer rowId;
	private String data;
	private GeneralData type;
	private User user;
	public static TransHist getInstance(EntityManager entityManager, Integer id) {
		return getInstance(entityManager, id, true);
	}
	public static TransHist getInstance(EntityManager entityManager, Integer id, boolean fetch) {
		if (isNull(id))return null;
		PrimaryKey pk = new PrimaryKey(id);
		Cache cache = entityManager.getEntityManagerFactory().getCache();
		TransHist o = null;
		if (fetch || cache != null && cache.contains(TransHist.class, pk))
			o = entityManager.find(TransHist.class, pk);
		return o;
	}
	public static List<TransHist> getAll(EntityManager entityManager) {
		return getNamedCollection(entityManager, "findAll");
	}
	public static List<TransHist> getAllActive(EntityManager entityManager) {
		return getNamedCollection(entityManager, "findAllActive");
	}
	public static List<TransHist> getCollection(EntityManager entityManager, String[] props, Object... values) {
		String qlString = "SELECT o FROM TransHist o";
		if(props != null && props.length > 0){
			qlString += " WHERE";
			int p=0;
			for(String prop:props){
				if(p > 0)
					qlString +=" AND";
				if(values[p]==null)
					qlString += " o."+prop+" IS NULL";
				else
					qlString += " o."+prop+" = ?"+(p+1);
				p++;
			}
		}
		return getCollection(entityManager, qlString, values);
	}
	public synchronized static List<TransHist> getCollection(EntityManager entityManager, String jpql, Object... values) {
		TypedQuery<TransHist> tq = entityManager.createQuery(jpql,TransHist.class);
		if(values != null && values.length > 0){
			int p=1;
			for(Object value:values)
				if(value != null)
					tq = tq.setParameter(p++, value);
		}
		return tq.getResultList();
	}
	public synchronized static List<TransHist> getNamedCollection(EntityManager entityManager, String name, Object... values) {
		TypedQuery<TransHist> tq = entityManager.createNamedQuery(name,TransHist.class);
		if(values != null && values.length > 0){
			int p=1;
			for(Object value:values)
				if(value != null)
					tq = tq.setParameter(p++, value);
		}
		return tq.getResultList();
	}
	public TransHistDAO(EntityManager entityManager) {
		super(entityManager);
	}
	public TransHistDAO(EntityManager entityManager, Integer id) {
		super(entityManager);
		this.id=id;
	}
	public TransHistDAO(EntityManager entityManager, TransHistDAO orig) {
		super(entityManager, orig);
		copyFrom(orig);
	}
	public void copyFrom(TransHistDAO orig){
		this.timestamp=orig.getTimestamp();
		this.typeId=orig.getTypeId();
		this.userId=orig.getUserId();
		this.object=orig.getObject();
		this.rowId=orig.getRowId();
		this.data=orig.getData();
	}
	@Override
	public String getHashKey(){
		return getHashKey(getKeyValues());
	}
	public Object[] getKeyValues(){
		return new Object[]{id};
	}
	@Override
	public int hashCode(){
		return PrimaryKey.hashCode(getKeyValues());
	}
	@Id
	@GeneratedValue
	@Column(name="ID",nullable=false)
	public Integer getId(){
		return id;
	}
	public TransHist setId(Integer id) throws Exception  {
		Integer oldValue = getId();
		if (!isSame(id, oldValue)) {
			this.id = id;
			setProperty("ID", id, oldValue);
		}
		return (TransHist)this;
	}
	@Column(name="TIMESTAMP",nullable=false)
	public DateTime getTimestamp(){
		return timestamp;
	}
	public TransHist setTimestamp(DateTime timestamp) throws Exception  {
		DateTime oldValue = getTimestamp();
		if (!isSame(timestamp, oldValue)) {
			this.timestamp = timestamp;
			setProperty("TIMESTAMP", timestamp, oldValue);
		}
		return (TransHist)this;
	}
	@Column(name="TYPE_ID",nullable=false)
	public Integer getTypeId(){
		return typeId;
	}
	public TransHist setTypeId(Integer typeId) throws Exception  {
		Integer oldValue = getTypeId();
		if (!isSame(typeId, oldValue)) {
			this.typeId = typeId;
			setProperty("TYPE_ID", typeId, oldValue);
			type=null;
		}
		return (TransHist)this;
	}
	@Column(name="USER_ID",nullable=false,length=64)
	public Integer getUserId(){
		return userId;
	}
	public TransHist setUserId(Integer userId) throws Exception  {
		Integer oldValue = getUserId();
		if (!isSame(userId, oldValue)) {
			this.userId = userId;
			setProperty("USER_ID", userId, oldValue);
			user=null;
		}
		return (TransHist)this;
	}
	@Column(name="OBJECT",nullable=false,length=64)
	public String getObject(){
		return object;
	}
	public TransHist setObject(String object) throws Exception  {
		String oldValue = getObject();
		if (!isSame(object, oldValue)) {
			this.object = object;
			setProperty("OBJECT", object, oldValue);
		}
		return (TransHist)this;
	}
	@Column(name="ROW_ID",nullable=false)
	public Integer getRowId(){
		return rowId;
	}
	public TransHist setRowId(Integer rowId) throws Exception  {
		Integer oldValue = getRowId();
		if (!isSame(rowId, oldValue)) {
			this.rowId = rowId;
			setProperty("ROW_ID", rowId, oldValue);
		}
		return (TransHist)this;
	}
	@Column(name="DATA",nullable=true,length=4096)
	public String getData(){
		return data;
	}
	public TransHist setData(String data) throws Exception  {
		String oldValue = getData();
		if (!isSame(data, oldValue)) {
			this.data = data;
			setProperty("DATA", data, oldValue);
		}
		return (TransHist)this;
	}
	public GeneralData getType() {
		if(type==null)
			return GeneralData.getInstance(getEntityManager(), getTypeId());
		return type;
	}
	public TransHist setType(GeneralData type) throws Exception {
		setTypeId(type==null?null:type.getId());
		this.type=type;
		return (TransHist)this;
	}
	public User getUser() {
		if(user==null)
			return User.getInstance(getEntityManager(), getUserId());
		return user;
	}
	public TransHist setUser(User user) throws Exception {
		setUserId(user==null?null:user.getId());
		this.user=user;
		return (TransHist)this;
	}
	public Map<String,Object> getPropertyValues() {
		Hashtable<String,Object> values = new Hashtable<String,Object>();
		for(PROPERTY prop:PROPERTY.values()) {
			Object value = getPropertyValue(prop);
			if(value!=null)
				values.put(""+prop,value);
		}
		return values;
	}

	public TransHist setPropertyValues(Map<String,Object> data) throws Exception  {
		for(String key:data.keySet())
			setPropertyValue(key, data.get(key).toString());
		return (TransHist)this;
	}

	@Override
	public Object getPropertyValue(String property) {
		return getPropertyValue(PROPERTY.valueOf(formatProperty(property)));
	}
	public Object getPropertyValue(PROPERTY property) {
		switch (property) {
			case ID: return getId();
			case TIMESTAMP: return getTimestamp();
			case TYPE_ID: return getTypeId();
			case USER_ID: return getUserId();
			case OBJECT: return getObject();
			case ROW_ID: return getRowId();
			case DATA: return getData();
		}
		return null;
	}

	@Override
	public TransHist setPropertyValue(String property, String value) throws Exception  {
		if(property == null) return (TransHist)this;
		return setPropertyValue(PROPERTY.valueOf(formatProperty(property)),value);
	}

	public TransHist setPropertyValue(PROPERTY property, String value) throws Exception  {
		switch (property) {
			case ID:setId(Integer.valueOf(value)); break;
			case TIMESTAMP:setTimestamp(new DateTime(value)); break;
			case TYPE_ID:setTypeId(Integer.valueOf(value)); break;
			case USER_ID:setUserId(Integer.valueOf(value)); break;
			case OBJECT:setObject(String.valueOf(value)); break;
			case ROW_ID:setRowId(Integer.valueOf(value)); break;
			case DATA:setData(String.valueOf(value)); break;
		}
		return (TransHist)this;
	}

	public TransHist copy() throws Exception {
		TransHist cp = new TransHist(getEntityManager(), (TransHist)this);
		copyChildrenTo(cp);
		return cp;
	}
	public void copyChildrenTo(TransHistDAO cp) throws Exception {
		super.copyChildrenTo(cp);
	}
	public Vector<String> getDifference(TransHistDAO o){
		Vector<String> diffs = super.getDifference(o);
		if(!isSame(getId(),o.getId())) diffs.add("ID");
		if(!isSame(getTimestamp(),o.getTimestamp())) diffs.add("TIMESTAMP");
		if(!isSame(getTypeId(),o.getTypeId())) diffs.add("TYPE_ID");
		if(!isSame(getUserId(),o.getUserId())) diffs.add("USER_ID");
		if(!isSame(getObject(),o.getObject())) diffs.add("OBJECT");
		if(!isSame(getRowId(),o.getRowId())) diffs.add("ROW_ID");
		if(!isSame(getData(),o.getData())) diffs.add("DATA");
		return diffs;
	}
	@Override
	public void insertParents() throws Exception {
		if(user != null && user.isNewInstance())
				user.insert();
	}
	@Override
	public void insertPreCheck() throws Exception {
		if (isNull(getTimestamp()))
			 throw new Exception("TIMESTAMP is required.");
		if (isNull(getTypeId()))
			 throw new Exception("TYPE_ID is required.");
		if (isNull(getUserId()))
			 throw new Exception("USER_ID is required.");
		if (isNull(getObject()))
			 throw new Exception("OBJECT is required.");
		if (isNull(getRowId()))
			 throw new Exception("ROW_ID is required.");
	}
	@Override
	public void insertChildren() throws Exception {
	}
}
