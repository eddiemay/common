package com.digitald4.common.dao;

import com.digitald4.common.dao.DataAccessObject;
import com.digitald4.common.jpa.PrimaryKey;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.TransHist;
import com.digitald4.common.model.User;
import com.digitald4.common.util.SortedList;
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
public abstract class UserDAO extends DataAccessObject{
	public enum KEY_PROPERTY{ID};
	public enum PROPERTY{ID,TYPE_ID,USER_NAME,EMAIL,FIRST_NAME,LAST_NAME,DISABLED,READ_ONLY,PASSWORD,NOTES,LAST_LOGIN};
	private Integer id;
	private Integer typeId = 1128;
	private String userName;
	private String email;
	private String firstName;
	private String lastName;
	private boolean disabled;
	private boolean readOnly;
	private String password;
	private String notes;
	private DateTime lastLogin;
	private List<TransHist> transHists;
	private GeneralData type;
	public static User getInstance(EntityManager entityManager, Integer id) {
		return getInstance(entityManager, id, true);
	}
	public static User getInstance(EntityManager entityManager, Integer id, boolean fetch) {
		if (isNull(id))return null;
		PrimaryKey pk = new PrimaryKey(id);
		Cache cache = entityManager.getEntityManagerFactory().getCache();
		User o = null;
		if (fetch || cache != null && cache.contains(User.class, pk))
			o = entityManager.find(User.class, pk);
		return o;
	}
	public static List<User> getAll(EntityManager entityManager) {
		return getNamedCollection(entityManager, "findAll");
	}
	public static List<User> getAllActive(EntityManager entityManager) {
		return getNamedCollection(entityManager, "findAllActive");
	}
	public static List<User> getCollection(EntityManager entityManager, String[] props, Object... values) {
		String qlString = "SELECT o FROM User o";
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
	public synchronized static List<User> getCollection(EntityManager entityManager, String jpql, Object... values) {
		TypedQuery<User> tq = entityManager.createQuery(jpql,User.class);
		if(values != null && values.length > 0){
			int p=1;
			for(Object value:values)
				if(value != null)
					tq = tq.setParameter(p++, value);
		}
		return tq.getResultList();
	}
	public synchronized static List<User> getNamedCollection(EntityManager entityManager, String name, Object... values) {
		TypedQuery<User> tq = entityManager.createNamedQuery(name,User.class);
		if(values != null && values.length > 0){
			int p=1;
			for(Object value:values)
				if(value != null)
					tq = tq.setParameter(p++, value);
		}
		return tq.getResultList();
	}
	public UserDAO(EntityManager entityManager) {
		super(entityManager);
	}
	public UserDAO(EntityManager entityManager, Integer id) {
		super(entityManager);
		this.id=id;
	}
	public UserDAO(EntityManager entityManager, UserDAO orig) {
		super(entityManager, orig);
		copyFrom(orig);
	}
	public void copyFrom(UserDAO orig){
		this.typeId=orig.getTypeId();
		this.userName=orig.getUserName();
		this.email=orig.getEmail();
		this.firstName=orig.getFirstName();
		this.lastName=orig.getLastName();
		this.disabled=orig.isDisabled();
		this.readOnly=orig.isReadOnly();
		this.password=orig.getPassword();
		this.notes=orig.getNotes();
		this.lastLogin=orig.getLastLogin();
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
	public User setId(Integer id) throws Exception  {
		Integer oldValue = getId();
		if (!isSame(id, oldValue)) {
			this.id = id;
			setProperty("ID", id, oldValue);
		}
		return (User)this;
	}
	@Column(name="TYPE_ID",nullable=false)
	public Integer getTypeId(){
		return typeId;
	}
	public User setTypeId(Integer typeId) throws Exception  {
		Integer oldValue = getTypeId();
		if (!isSame(typeId, oldValue)) {
			this.typeId = typeId;
			setProperty("TYPE_ID", typeId, oldValue);
			type=null;
		}
		return (User)this;
	}
	@Column(name="USER_NAME",nullable=false,length=20)
	public String getUserName(){
		return userName;
	}
	public User setUserName(String userName) throws Exception  {
		String oldValue = getUserName();
		if (!isSame(userName, oldValue)) {
			this.userName = userName;
			setProperty("USER_NAME", userName, oldValue);
		}
		return (User)this;
	}
	@Column(name="EMAIL",nullable=false,length=64)
	public String getEmail(){
		return email;
	}
	public User setEmail(String email) throws Exception  {
		String oldValue = getEmail();
		if (!isSame(email, oldValue)) {
			this.email = email;
			setProperty("EMAIL", email, oldValue);
		}
		return (User)this;
	}
	@Column(name="FIRST_NAME",nullable=false,length=20)
	public String getFirstName(){
		return firstName;
	}
	public User setFirstName(String firstName) throws Exception  {
		String oldValue = getFirstName();
		if (!isSame(firstName, oldValue)) {
			this.firstName = firstName;
			setProperty("FIRST_NAME", firstName, oldValue);
		}
		return (User)this;
	}
	@Column(name="LAST_NAME",nullable=false,length=20)
	public String getLastName(){
		return lastName;
	}
	public User setLastName(String lastName) throws Exception  {
		String oldValue = getLastName();
		if (!isSame(lastName, oldValue)) {
			this.lastName = lastName;
			setProperty("LAST_NAME", lastName, oldValue);
		}
		return (User)this;
	}
	@Column(name="DISABLED",nullable=true)
	public boolean isDisabled(){
		return disabled;
	}
	public User setDisabled(boolean disabled) throws Exception  {
		boolean oldValue = isDisabled();
		if (!isSame(disabled, oldValue)) {
			this.disabled = disabled;
			setProperty("DISABLED", disabled, oldValue);
		}
		return (User)this;
	}
	@Column(name="READ_ONLY",nullable=true)
	public boolean isReadOnly(){
		return readOnly;
	}
	public User setReadOnly(boolean readOnly) throws Exception  {
		boolean oldValue = isReadOnly();
		if (!isSame(readOnly, oldValue)) {
			this.readOnly = readOnly;
			setProperty("READ_ONLY", readOnly, oldValue);
		}
		return (User)this;
	}
	@Column(name="PASSWORD",nullable=true,length=128)
	public String getPassword(){
		return password;
	}
	public User setPassword(String password) throws Exception  {
		String oldValue = getPassword();
		if (!isSame(password, oldValue)) {
			this.password = password;
			setProperty("PASSWORD", password, oldValue);
		}
		return (User)this;
	}
	@Column(name="NOTES",nullable=true,length=4096)
	public String getNotes(){
		return notes;
	}
	public User setNotes(String notes) throws Exception  {
		String oldValue = getNotes();
		if (!isSame(notes, oldValue)) {
			this.notes = notes;
			setProperty("NOTES", notes, oldValue);
		}
		return (User)this;
	}
	@Column(name="LAST_LOGIN",nullable=true)
	public DateTime getLastLogin(){
		return lastLogin;
	}
	public User setLastLogin(DateTime lastLogin) throws Exception  {
		DateTime oldValue = getLastLogin();
		if (!isSame(lastLogin, oldValue)) {
			this.lastLogin = lastLogin;
			setProperty("LAST_LOGIN", lastLogin, oldValue);
		}
		return (User)this;
	}
	public GeneralData getType() {
		if(type==null)
			return GeneralData.getInstance(getEntityManager(), getTypeId());
		return type;
	}
	public User setType(GeneralData type) throws Exception {
		setTypeId(type==null?null:type.getId());
		this.type=type;
		return (User)this;
	}
	public List<TransHist> getTransHists() {
		if(isNewInstance() || transHists != null){
			if(transHists == null)
				transHists = new SortedList<TransHist>();
			return transHists;
		}
		return TransHist.getNamedCollection(getEntityManager(), "findByUser",getId());
	}
	public User addTransHist(TransHist transHist) throws Exception {
		transHist.setUser((User)this);
		if(isNewInstance() || transHists != null)
			getTransHists().add(transHist);
		else
			transHist.insert();
		return (User)this;
	}
	public User removeTransHist(TransHist transHist) throws Exception {
		if(isNewInstance() || transHists != null)
			getTransHists().remove(transHist);
		else
			transHist.delete();
		return (User)this;
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

	public User setPropertyValues(Map<String,Object> data) throws Exception  {
		for(String key:data.keySet())
			setPropertyValue(key, data.get(key).toString());
		return (User)this;
	}

	@Override
	public Object getPropertyValue(String property) {
		return getPropertyValue(PROPERTY.valueOf(formatProperty(property)));
	}
	public Object getPropertyValue(PROPERTY property) {
		switch (property) {
			case ID: return getId();
			case TYPE_ID: return getTypeId();
			case USER_NAME: return getUserName();
			case EMAIL: return getEmail();
			case FIRST_NAME: return getFirstName();
			case LAST_NAME: return getLastName();
			case DISABLED: return isDisabled();
			case READ_ONLY: return isReadOnly();
			case PASSWORD: return getPassword();
			case NOTES: return getNotes();
			case LAST_LOGIN: return getLastLogin();
		}
		return null;
	}

	@Override
	public User setPropertyValue(String property, String value) throws Exception  {
		if(property == null) return (User)this;
		return setPropertyValue(PROPERTY.valueOf(formatProperty(property)),value);
	}

	public User setPropertyValue(PROPERTY property, String value) throws Exception  {
		switch (property) {
			case ID:setId(Integer.valueOf(value)); break;
			case TYPE_ID:setTypeId(Integer.valueOf(value)); break;
			case USER_NAME:setUserName(String.valueOf(value)); break;
			case EMAIL:setEmail(String.valueOf(value)); break;
			case FIRST_NAME:setFirstName(String.valueOf(value)); break;
			case LAST_NAME:setLastName(String.valueOf(value)); break;
			case DISABLED:setDisabled(Boolean.valueOf(value)); break;
			case READ_ONLY:setReadOnly(Boolean.valueOf(value)); break;
			case PASSWORD:setPassword(String.valueOf(value)); break;
			case NOTES:setNotes(String.valueOf(value)); break;
			case LAST_LOGIN:setLastLogin(new DateTime(value)); break;
		}
		return (User)this;
	}

	public User copy() throws Exception {
		User cp = new User(getEntityManager(), (User)this);
		copyChildrenTo(cp);
		return cp;
	}
	public void copyChildrenTo(UserDAO cp) throws Exception {
		super.copyChildrenTo(cp);
		for(TransHist child:getTransHists())
			cp.addTransHist(child.copy());
	}
	public Vector<String> getDifference(UserDAO o){
		Vector<String> diffs = super.getDifference(o);
		if(!isSame(getId(),o.getId())) diffs.add("ID");
		if(!isSame(getTypeId(),o.getTypeId())) diffs.add("TYPE_ID");
		if(!isSame(getUserName(),o.getUserName())) diffs.add("USER_NAME");
		if(!isSame(getEmail(),o.getEmail())) diffs.add("EMAIL");
		if(!isSame(getFirstName(),o.getFirstName())) diffs.add("FIRST_NAME");
		if(!isSame(getLastName(),o.getLastName())) diffs.add("LAST_NAME");
		if(!isSame(isDisabled(),o.isDisabled())) diffs.add("DISABLED");
		if(!isSame(isReadOnly(),o.isReadOnly())) diffs.add("READ_ONLY");
		if(!isSame(getPassword(),o.getPassword())) diffs.add("PASSWORD");
		if(!isSame(getNotes(),o.getNotes())) diffs.add("NOTES");
		if(!isSame(getLastLogin(),o.getLastLogin())) diffs.add("LAST_LOGIN");
		return diffs;
	}
	@Override
	public void insertParents() throws Exception {
	}
	@Override
	public void insertPreCheck() throws Exception {
		if (isNull(getTypeId()))
			 throw new Exception("TYPE_ID is required.");
		if (isNull(getUserName()))
			 throw new Exception("USER_NAME is required.");
		if (isNull(getEmail()))
			 throw new Exception("EMAIL is required.");
		if (isNull(getFirstName()))
			 throw new Exception("FIRST_NAME is required.");
		if (isNull(getLastName()))
			 throw new Exception("LAST_NAME is required.");
	}
	@Override
	public void insertChildren() throws Exception {
		if (transHists != null) {
			for (TransHist transHist : getTransHists()) {
				transHist.setUser((User)this);
			}
		}
		if (transHists != null) {
			for (TransHist transHist : getTransHists()) {
				transHist.insert();
			}
			transHists = null;
		}
	}
}
