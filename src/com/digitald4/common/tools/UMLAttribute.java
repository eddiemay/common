package com.digitald4.common.tools;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdom.Element;

import com.digitald4.common.tools.FieldType.DataStore;

public class UMLAttribute {
	private UMLClass umlClass;
	private String name;
	private String formerName;
	private FieldType type;
	private String size;
	private String def;
	private boolean nullable;
	private boolean id;
	private boolean generated;
	private String sequence;
	private String desc;
	public UMLAttribute(UMLClass umlClass, String name){
		setUmlClass(umlClass);
		setName(name);
	}
	public UMLAttribute(UMLClass umlClass, Element e){
		setUmlClass(umlClass);
		setName(e.getAttributeValue("name"));
		setFormerName(e.getAttributeValue("formername"));
		setType(e.getAttributeValue("type"));
		setSize(e.getAttributeValue("size"));
		setDefault(e.getAttributeValue("default"));
		setNullable(e.getAttributeValue("nullable")==null || e.getAttributeValue("nullable").equals("true"));
		setId(e.getAttributeValue("id")!=null && e.getAttributeValue("id").equals("true"));
		setGenerated(e.getAttributeValue("generated")!=null && e.getAttributeValue("generated").equals("true"));
		setSequence(e.getAttributeValue("sequence"));
		setDesc(e.getText());
	}
	public UMLClass getUmlClass() {
		return umlClass;
	}
	public void setUmlClass(UMLClass umlClass) {
		this.umlClass = umlClass;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		setFormerName(getName());
		this.name = name;
	}
	public String getFormerName() {
		return formerName;
	}
	public void setFormerName(String formerName) {
		this.formerName = formerName;
	}
	public String getDBName(){
		return getName().toUpperCase().replaceAll(" ", "_");
	}
	public String getDBFormerName(){
		if(getFormerName()==null) return null;
		return getFormerName().toUpperCase().replaceAll(" ", "_");
	}
	public FieldType getType() {
		return type;
	}
	public void setType(String type) {
		this.type = FieldType.valueOf(type.toUpperCase());
	}
	public String getDBType(){
		return type.getDataStoreType(DataStore.MYSQL);
	}
	public String getDBTypeDeclare(){
		return getDBType().replaceAll("%s", "" + getSize());
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	public String getDefault(){
		return def;
	}
	public void setDefault(String def){
		this.def = def;
	}
	public String getDim(){
		if(getType() == FieldType.STRING)
			return "'";
		return "";
	}
	public String getDefaultWDim(){
		if(def==null)
			return def;
		return getDim()+def+getDim();
	}
	public boolean isNullable() {
		return nullable;
	}
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	public boolean isGenerated() {
		return generated;
	}
	public void setGenerated(boolean generated) {
		this.generated = generated;
	}
	public boolean isId() {
		return id;
	}
	public void setId(boolean id) {
		this.id = id;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String toString(){
		return getName();
	}
	public Element getXMLElement() {
		Element e = new Element("ATTRIBUTE");
		e.setAttribute("name", getName());
		e.setAttribute("type", ""+getType());
		e.setAttribute("size",""+getSize());
		if(getDefault()!=null)
			e.setAttribute("default",getDefault());
		e.setAttribute("nullable",isNullable()?"true":"false");
		e.setAttribute("id",isId()?"true":"false");
		if(getSequence()!=null)
			e.setAttribute("sequence",getSequence());
		e.setText(getDesc());
		return e;
	}
	public String getDBCreation() {
		String out = getDBName();
		out += " "+getDBTypeDeclare();
		if(getDefault()!=null)
			out+=" DEFAULT "+getDefaultWDim();
		if(!isNullable())
			out+=" NOT NULL";
		if(isGenerated())
			out += " AUTO_INCREMENT";
		return out;
	}
	public String getDBChange(DatabaseMetaData dbmd, String schema) throws SQLException {
		String headCmd = "", out = "", comment = "";
		ResultSet rs = dbmd.getColumns(null, schema, umlClass.getDBTable(), getDBName());
		if (rs.next()) {
			if (!rs.getString("TYPE_NAME").equals(getDBType().replace("(%s)", "")) || getSize() != null && !rs.getString("COLUMN_SIZE").equals(getSize()))
				out += " " + getDBTypeDeclare();
			String def = rs.getString("COLUMN_DEF");
			if (!("" + def).trim().equals("" + getDefaultWDim()))
				out += " DEFAULT " + getDefaultWDim();
			if (out.length() > 0) {
				out = " MODIFY " + getDBName() + out;
				comment = " --changing from " + rs.getString("TYPE_NAME") + "(" + rs.getString("COLUMN_SIZE") + ")";
			}
		} else {
			ResultSet rs2=null;
			if (getDBFormerName() != null)
				rs2 = dbmd.getColumns(null, schema, umlClass.getDBTable(), getDBFormerName());
			if (rs2 != null && rs2.next()) {
				headCmd = "ALTER TABLE " + umlClass.getDBTable() + " RENAME COLUMN " + getDBFormerName() + " TO " + getDBName() + ";\n";
				if (!rs2.getString("TYPE_NAME").equals(getDBType()) || !rs2.getString("COLUMN_SIZE").equals(getSize()))
					out += " " + getDBTypeDeclare();
				String def = rs2.getString("COLUMN_DEF");
				if (!("" + def).trim().equals("" + getDefaultWDim()))
					out += " DEFAULT " + getDefaultWDim();
				if (out.length() > 0)
					out = " MODIFY " + getDBName() + out;
			} else {
				out = " ADD " + getDBCreation();
			}
			if (rs2 != null) rs2.close();
		}
		rs.close();
		if (out.length() > 0)
			out = "ALTER TABLE " + umlClass.getDBTable() + out + ";" + comment + "\n";
		return headCmd+out;
	}
}
