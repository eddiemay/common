package com.digitald4.common.model;
import java.util.ArrayList;
import java.util.Hashtable;

import com.digitald4.common.dao.GeneralDataDAO;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
@Entity
@Table(schema="common",name="general_data")
@NamedQueries({
	@NamedQuery(name = "findByID", query="SELECT o FROM GeneralData o WHERE o.ID=?1"),//AUTO-GENERATED
	@NamedQuery(name = "findAll", query="SELECT o FROM GeneralData o"),//AUTO-GENERATED
	@NamedQuery(name = "findAllActive", query="SELECT o FROM GeneralData o"),//AUTO-GENERATED
	@NamedQuery(name = "findByGroup", query="SELECT o FROM GeneralData o WHERE o.GROUP_ID=?1"),//AUTO-GENERATED
})
@NamedNativeQueries({
	@NamedNativeQuery(name = "refresh", query="SELECT o.* FROM general_data o WHERE o.ID=?"),//AUTO-GENERATED
})
public class GeneralData extends GeneralDataDAO{
	public GeneralData(EntityManager entityManager) {
		super(entityManager);
	}
	public GeneralData(EntityManager entityManager, Integer id){
		super(entityManager, id);
	}
	public GeneralData(EntityManager entityManager, GeneralData orig){
		super(entityManager, orig);
	}
	public static GeneralData getInstance(EntityManager entityManager, GeneralData group, int inGroupId) {
		for (GeneralData gd : getCollection(entityManager, new String[]{""+PROPERTY.GROUP_ID}, group == null ? null : group.getId())) {
			if (gd.getInGroupId() == inGroupId) {
				return gd;
			}
		}
		System.err.println("Missing GeneralData ("+group+","+inGroupId+")");
		return null;
	}
	
	private Hashtable<String, Object> attributes = null;
	public Object getDataAttribute(String attribute) {
		if (attributes == null) {
			parseAttributes();
		}
		return attributes.get(attribute);
	}
	public void parseAttributes() {
		attributes = new Hashtable<String, Object>();
		String data = getData();
		if (data != null && data.charAt(0) == '{') {
			data = data.trim();
			for (String attr : data.substring(1, data.length() - 1).split(",")) {
				String name = attr.trim();
				name = name.substring(0, name.indexOf(':'));
				if (name.charAt(0) == '\'') {
					name = name.substring(1, name.length() - 1);
				}
				String value = attr.substring(attr.indexOf(':') + 1).trim();
				if (value.charAt(0) == '\'') {
					attributes.put(name, value.substring(1, value.length() - 1));
				}
				else if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")) {
					attributes.put(name, Boolean.parseBoolean(value));
				} else {
					attributes.put(name, Integer.parseInt(value));
				}
			}
		}
	}
	
	public String toString(){
		return getName();
	}
	
	@Override
	public int compareTo(Object o) {
		if(o instanceof GeneralData) {
			GeneralData gd = (GeneralData)o;
			if (getRank() < gd.getRank()) {
				return -1;
			}
			if (getRank() > gd.getRank()) {
				return 1;
			}
		}
		return super.compareTo(o);
	}
	
	@Override
	public void delete() throws Exception {
		for (GeneralData gd : new ArrayList<GeneralData>(getGeneralDatas())) {
			gd.delete();
		}
		super.delete();
	}
}
