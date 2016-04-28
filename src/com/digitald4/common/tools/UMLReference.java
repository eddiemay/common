package com.digitald4.common.tools;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jdom.Element;

public class UMLReference implements Comparable<UMLReference>{
	public static enum DeleteRule{NO_ACTION,CASCADE,SET_NULL};
	private UMLClass umlClass;
	private String refClass;
	private boolean indexed;
	private String dbPrefix;
	private boolean required;
	private String name;
	private String refName;
	private String desc;
	private DeleteRule deleteRule = DeleteRule.CASCADE;
	private ArrayList<UMLConnector> connectors = new ArrayList<UMLConnector>();
	private boolean standard;
	
	public UMLReference(UMLClass umlClass, String refClass, String dbPrefix, boolean indexed) {
		setUmlClass(umlClass);
		setRefClass(refClass);
		setIndexed(indexed);
		setDbPrefix(dbPrefix);
	}
	public UMLReference(UMLClass umlClass, String refClass, String dbPrefix, boolean indexed, String attribute, String refAttr) {
		setUmlClass(umlClass);
		setRefClass(refClass);
		setIndexed(indexed);
		setDbPrefix(dbPrefix);
		addConnector(new UMLConnector(this,attribute,refAttr));
		deleteRule = DeleteRule.SET_NULL;
		standard=true;
	}
	public UMLReference(UMLClass umlClass, Element e) {
		setUmlClass(umlClass);
		setRefClass(e.getAttributeValue("refclass"));
		setIndexed(e.getAttributeValue("indexed")!=null && e.getAttributeValue("indexed").equals("true"));
		setDbPrefix(e.getAttributeValue("dbprefix"));
		setRequired(e.getAttributeValue("required")==null || e.getAttributeValue("required").equals("false"));
		setName(e.getAttributeValue("name"));
		setRefName(e.getAttributeValue("refname"));
		setDesc(e.getText());
		String dr = e.getAttributeValue("deleterule");
		if(dr==null || dr.equalsIgnoreCase(""+DeleteRule.CASCADE))
			deleteRule = DeleteRule.CASCADE;
		else if(dr.equalsIgnoreCase(""+DeleteRule.NO_ACTION))
			deleteRule = DeleteRule.NO_ACTION;
		else if(dr.equalsIgnoreCase(""+DeleteRule.SET_NULL))
			deleteRule = DeleteRule.SET_NULL;
		for(Object o:e.getChildren("CONNECTOR"))
			addConnector(new UMLConnector(this,(Element)o));
	}
	public void addConnector(UMLConnector umlConnector) {
		connectors.add(umlConnector);
	}
	public UMLClass getUmlClass() {
		return umlClass;
	}
	public void setUmlClass(UMLClass umlClass) {
		this.umlClass = umlClass;
	}
	public String getRefClass() {
		return refClass;
	}
	public void setRefClass(String refClass) {
		this.refClass = refClass;
	}
	public boolean isIndexed() {
		return indexed;
	}
	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}
	public String getDbPrefix() {
		return dbPrefix;
	}
	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}
	public boolean isRequired() {
		return required;
	}
	public void setRequired(boolean required) {
		this.required = required;
	}
	public String getName() {
		if(name == null)
			return getRefClass();
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRefName() {
		if(refName == null)
			return getUmlClass().getName();
		return refName;
	}
	public void setRefName(String refName) {
		this.refName = refName;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public Collection<UMLConnector> getConnectors(){
		return connectors;
	}
	public String toString(){
		return getName();
	}
	public String getDBName() {
		return getUmlClass().getTablePrefixStr()+getDbPrefix();
	}
	public String getDBFKName(){
		String out1 = "";
		for(UMLConnector conn:getConnectors()){
			out1 += "_"+conn.getAttrDBName().replaceAll("_", "");
		}
		return umlClass.getTablePrefixStr()+"FK"+out1;
	}
	public DeleteRule getDeleteRule(){
		return deleteRule;
	}
	public void setDeleteRule(DeleteRule deleteRule){
		this.deleteRule = deleteRule;
	}
	public String getDBCreation() {
		String out1 = "";
		String out2 = "";
		for(UMLConnector conn:getConnectors()){
			if(out1.length()>0){
				out1+=",";
				out2+=",";
			}
			out1 += conn.getAttrDBName();
			out2 += conn.getRefAttrDBName();
		}
		return "CONSTRAINT " + getDBFKName() + " FOREIGN KEY (" + out1 + ") REFERENCES " + getRefDBTable() + " (" + out2 + ") ON DELETE " + (getDeleteRule().toString().replace("_", " "));
	}
	public UMLClass getRefUMLClass() {
		return UMLClass.findClass(getRefClass());
	}
	public String getRefDBTable(){
		UMLClass refClass = getRefUMLClass();
		if(refClass!=null)
			return refClass.getDBTable();
		return getRefClass().toUpperCase().replaceAll(" ", "_")+"_TABLE";
	}
	@Override
	public int compareTo(UMLReference ref) {
		if(this==ref)return 0;
		int ret = getDBName().compareTo(ref.getDBName());
		return ret;
	}
	public Element getXMLElement() {
		Element e = new Element("REFERENCE");
		e.setAttribute("refclass", getRefClass());
		e.setAttribute("indexed",isIndexed()?"true":"false");
		e.setAttribute("dbprefix",getDbPrefix());
		e.setAttribute("required",isRequired()?"true":"false");
		if(getName()!=null)
			e.setAttribute("name",getName());
		if(getRefName()!=null)
			e.setAttribute("refname",getRefName());
		e.setText(getDesc());
		for(UMLConnector conn:getConnectors())
			e.addContent(conn.getXMLElement());
		return e;
	}
	public String getDBChange(DatabaseMetaData dbmd, String schema) throws SQLException {
		String out = "";
		// We have to just check for an index since the MySQL foreign key code doesn't work.
		boolean needCreate = !umlClass.getDBIndexes(dbmd).contains(getDBFKName());
		/*DBForiegnKey fk = umlClass.getDBReference(dbmd, getDBFKName());
		if (fk != null) {
			if (!getDBCreation().equalsIgnoreCase(fk.getDBCreation())) {
				out = "ALTER TABLE " + umlClass.getDBTable() + " DROP CONSTRAINT " + getDBFKName() + ";\n";
			}
			else
				needCreate=false;
		}*/
		if (needCreate)
			out += "ALTER TABLE " + umlClass.getDBTable() + " ADD " + getDBCreation() + ";\n";
		return out;
	}
	public UMLIndex getUMLIndex() {
		ArrayList<String> columns = new ArrayList<String>();
		for (UMLConnector conn : getConnectors())
			columns.add(conn.getAttrDBName());
		return new UMLIndex(umlClass, columns);
	}
	public boolean isStandard() {
		return standard;
	}
}
