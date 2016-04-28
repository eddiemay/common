package com.digitald4.common.dao;

import com.digitald4.common.dao.DataAccessObject;
import com.digitald4.common.jpa.PrimaryKey;
import com.digitald4.common.model.GeneralData;
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

/** TODO Copy Right*/
/**Description of class, (we need to get this from somewhere, database? xml?)*/
public abstract class GeneralDataDAO extends DataAccessObject{
	public enum KEY_PROPERTY{ID};
	public enum PROPERTY{ID,GROUP_ID,IN_GROUP_ID,NAME,RANK,ACTIVE,DESCRIPTION,DATA};
	private Integer id;
	private Integer groupId;
	private Integer inGroupId;
	private String name;
	private double rank;
	private boolean active = true;
	private String description;
	private String data;
	private List<GeneralData> generalDatas;
	private GeneralData group;
	public static GeneralData getInstance(EntityManager entityManager, Integer id) {
		return getInstance(entityManager, id, true);
	}
	public static GeneralData getInstance(EntityManager entityManager, Integer id, boolean fetch) {
		if (isNull(id))return null;
		PrimaryKey pk = new PrimaryKey(id);
		Cache cache = entityManager.getEntityManagerFactory().getCache();
		GeneralData o = null;
		if (fetch || cache != null && cache.contains(GeneralData.class, pk))
			o = entityManager.find(GeneralData.class, pk);
		return o;
	}
	public static List<GeneralData> getAll(EntityManager entityManager) {
		return getNamedCollection(entityManager, "findAll");
	}
	public static List<GeneralData> getAllActive(EntityManager entityManager) {
		return getNamedCollection(entityManager, "findAllActive");
	}
	public static List<GeneralData> getCollection(EntityManager entityManager, String[] props, Object... values) {
		String qlString = "SELECT o FROM GeneralData o";
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
	public synchronized static List<GeneralData> getCollection(EntityManager entityManager, String jpql, Object... values) {
		TypedQuery<GeneralData> tq = entityManager.createQuery(jpql, GeneralData.class);
		if (values != null && values.length > 0) {
			int p=1;
			for(Object value:values)
				if(value != null)
					tq = tq.setParameter(p++, value);
		}
		return tq.getResultList();
	}
	public synchronized static List<GeneralData> getNamedCollection(EntityManager entityManager, String name, Object... values) {
		TypedQuery<GeneralData> tq = entityManager.createNamedQuery(name,GeneralData.class);
		if(values != null && values.length > 0){
			int p=1;
			for(Object value:values)
				if(value != null)
					tq = tq.setParameter(p++, value);
		}
		return tq.getResultList();
	}
	public GeneralDataDAO(EntityManager entityManager) {
		super(entityManager);
	}
	public GeneralDataDAO(EntityManager entityManager, Integer id) {
		super(entityManager);
		this.id=id;
	}
	public GeneralDataDAO(EntityManager entityManager, GeneralDataDAO orig) {
		super(entityManager, orig);
		copyFrom(orig);
	}
	public void copyFrom(GeneralDataDAO orig){
		this.groupId=orig.getGroupId();
		this.inGroupId=orig.getInGroupId();
		this.name=orig.getName();
		this.rank=orig.getRank();
		this.active=orig.isActive();
		this.description=orig.getDescription();
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
	public GeneralData setId(Integer id) throws Exception  {
		Integer oldValue = getId();
		if (!isSame(id, oldValue)) {
			this.id = id;
			setProperty("ID", id, oldValue);
		}
		return (GeneralData)this;
	}
	@Column(name="GROUP_ID",nullable=true)
	public Integer getGroupId(){
		return groupId;
	}
	public GeneralData setGroupId(Integer groupId) throws Exception  {
		Integer oldValue = getGroupId();
		if (!isSame(groupId, oldValue)) {
			this.groupId = groupId;
			setProperty("GROUP_ID", groupId, oldValue);
			group=null;
		}
		return (GeneralData)this;
	}
	@Column(name="IN_GROUP_ID",nullable=false)
	public Integer getInGroupId(){
		return inGroupId;
	}
	public GeneralData setInGroupId(Integer inGroupId) throws Exception  {
		Integer oldValue = getInGroupId();
		if (!isSame(inGroupId, oldValue)) {
			this.inGroupId = inGroupId;
			setProperty("IN_GROUP_ID", inGroupId, oldValue);
		}
		return (GeneralData)this;
	}
	@Column(name="NAME",nullable=false,length=64)
	public String getName(){
		return name;
	}
	public GeneralData setName(String name) throws Exception  {
		String oldValue = getName();
		if (!isSame(name, oldValue)) {
			this.name = name;
			setProperty("NAME", name, oldValue);
		}
		return (GeneralData)this;
	}
	@Column(name="RANK",nullable=true)
	public double getRank(){
		return rank;
	}
	public GeneralData setRank(double rank) throws Exception  {
		double oldValue = getRank();
		if (!isSame(rank, oldValue)) {
			this.rank = rank;
			setProperty("RANK", rank, oldValue);
		}
		return (GeneralData)this;
	}
	@Column(name="ACTIVE",nullable=true)
	public boolean isActive(){
		return active;
	}
	public GeneralData setActive(boolean active) throws Exception  {
		boolean oldValue = isActive();
		if (!isSame(active, oldValue)) {
			this.active = active;
			setProperty("ACTIVE", active, oldValue);
		}
		return (GeneralData)this;
	}
	@Column(name="DESCRIPTION",nullable=true,length=256)
	public String getDescription(){
		return description;
	}
	public GeneralData setDescription(String description) throws Exception  {
		String oldValue = getDescription();
		if (!isSame(description, oldValue)) {
			this.description = description;
			setProperty("DESCRIPTION", description, oldValue);
		}
		return (GeneralData)this;
	}
	@Column(name="DATA",nullable=true,length=128)
	public String getData(){
		return data;
	}
	public GeneralData setData(String data) throws Exception  {
		String oldValue = getData();
		if (!isSame(data, oldValue)) {
			this.data = data;
			setProperty("DATA", data, oldValue);
		}
		return (GeneralData)this;
	}
	public GeneralData getGroup() {
		if(group==null)
			return GeneralData.getInstance(getEntityManager(), getGroupId());
		return group;
	}
	public GeneralData setGroup(GeneralData group) throws Exception {
		setGroupId(group==null?null:group.getId());
		this.group=group;
		return (GeneralData)this;
	}
	public List<GeneralData> getGeneralDatas() {
		if(isNewInstance() || generalDatas != null){
			if(generalDatas == null)
				generalDatas = new SortedList<GeneralData>();
			return generalDatas;
		}
		return GeneralData.getNamedCollection(getEntityManager(), "findByGroup",getId());
	}
	public GeneralData addGeneralData(GeneralData generalData) throws Exception {
		generalData.setGroup((GeneralData)this);
		if(isNewInstance() || generalDatas != null)
			getGeneralDatas().add(generalData);
		else
			generalData.insert();
		return (GeneralData)this;
	}
	public GeneralData removeGeneralData(GeneralData generalData) throws Exception {
		if(isNewInstance() || generalDatas != null)
			getGeneralDatas().remove(generalData);
		else
			generalData.delete();
		return (GeneralData)this;
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

	public GeneralData setPropertyValues(Map<String,Object> data) throws Exception  {
		for(String key:data.keySet())
			setPropertyValue(key, data.get(key).toString());
		return (GeneralData)this;
	}

	@Override
	public Object getPropertyValue(String property) {
		return getPropertyValue(PROPERTY.valueOf(formatProperty(property)));
	}
	public Object getPropertyValue(PROPERTY property) {
		switch (property) {
			case ID: return getId();
			case GROUP_ID: return getGroupId();
			case IN_GROUP_ID: return getInGroupId();
			case NAME: return getName();
			case RANK: return getRank();
			case ACTIVE: return isActive();
			case DESCRIPTION: return getDescription();
			case DATA: return getData();
		}
		return null;
	}

	@Override
	public GeneralData setPropertyValue(String property, String value) throws Exception  {
		if(property == null) return (GeneralData)this;
		return setPropertyValue(PROPERTY.valueOf(formatProperty(property)),value);
	}

	public GeneralData setPropertyValue(PROPERTY property, String value) throws Exception  {
		switch (property) {
			case ID:setId(Integer.valueOf(value)); break;
			case GROUP_ID:setGroupId(Integer.valueOf(value)); break;
			case IN_GROUP_ID:setInGroupId(Integer.valueOf(value)); break;
			case NAME:setName(String.valueOf(value)); break;
			case RANK:setRank(Double.valueOf(value)); break;
			case ACTIVE:setActive(Boolean.valueOf(value)); break;
			case DESCRIPTION:setDescription(String.valueOf(value)); break;
			case DATA:setData(String.valueOf(value)); break;
		}
		return (GeneralData)this;
	}

	public GeneralData copy() throws Exception {
		GeneralData cp = new GeneralData(getEntityManager(), (GeneralData)this);
		copyChildrenTo(cp);
		return cp;
	}
	public void copyChildrenTo(GeneralDataDAO cp) throws Exception {
		super.copyChildrenTo(cp);
		for(GeneralData child:getGeneralDatas())
			cp.addGeneralData(child.copy());
	}
	public Vector<String> getDifference(GeneralDataDAO o){
		Vector<String> diffs = super.getDifference(o);
		if(!isSame(getId(),o.getId())) diffs.add("ID");
		if(!isSame(getGroupId(),o.getGroupId())) diffs.add("GROUP_ID");
		if(!isSame(getInGroupId(),o.getInGroupId())) diffs.add("IN_GROUP_ID");
		if(!isSame(getName(),o.getName())) diffs.add("NAME");
		if(!isSame(getRank(),o.getRank())) diffs.add("RANK");
		if(!isSame(isActive(),o.isActive())) diffs.add("ACTIVE");
		if(!isSame(getDescription(),o.getDescription())) diffs.add("DESCRIPTION");
		if(!isSame(getData(),o.getData())) diffs.add("DATA");
		return diffs;
	}
	@Override
	public void insertParents() throws Exception {
		if(group != null && group.isNewInstance())
				group.insert();
	}
	@Override
	public void insertPreCheck() throws Exception {
		if (isNull(getInGroupId()))
			 throw new Exception("IN_GROUP_ID is required.");
		if (isNull(getName()))
			 throw new Exception("NAME is required.");
	}
	@Override
	public void insertChildren() throws Exception {
		if (generalDatas != null) {
			for (GeneralData generalData : getGeneralDatas()) {
				generalData.setGroup((GeneralData)this);
			}
		}
		if (generalDatas != null) {
			for (GeneralData generalData : getGeneralDatas()) {
				generalData.insert();
			}
			generalDatas = null;
		}
	}
}
