package com.digitald4.common.tools;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class UMLIndex implements Comparable<UMLIndex>{
	private UMLClass umlClass;
	private ArrayList<String> columns = new ArrayList<String>();
	public UMLIndex(UMLClass umlClass, ArrayList<String> columns){
		this.umlClass = umlClass;
		this.columns = columns;
	}
	public UMLClass getUmlClass() {
		return umlClass;
	}
	public void setUmlClass(UMLClass umlClass) {
		this.umlClass = umlClass;
	}
	public Collection<String> getColumns(){
		return columns;
	}
	public void addColumn(String column){
		getColumns().add(column);
	}
	public String getDBName(){
		String name = getUmlClass().getTablePrefixStr() + "FK";
		for(String col:getColumns()){
			if(col.equalsIgnoreCase("PLANYEAR"))
				name+="_PY";
			else if(col.equalsIgnoreCase("YEAR"))
				name+="_YR";
			else if(col.equalsIgnoreCase("BBANK_SUB_ID"))
				name+="_BSUBID";
			else if(col.equalsIgnoreCase("ABANK_SUB_ID"))
				name+="_ASUBID";
			else if(col.toUpperCase().contains("SERVICE"))
				name+="_"+col.replaceAll("SERVICE", "SRV").replaceAll("_", " ");
			//else if(col.toUpperCase().contains("WORK_GROUP"))
				//name+="_"+col.replaceAll("WORK_GROUP", "WG").replaceAll("_", " ");
			else
				name += "_"+col.replaceAll("_", " ");
		}
		return name.toUpperCase().replaceAll(" ", "");
	}
	public String getDBCreation() {
		String out="";
		for(String col:getColumns()){
			if(out.length()>0)
				out+=",";
			out+=col.replaceAll(" ", "_");
		}
		return "CREATE INDEX " + getDBName() + " ON " + umlClass.getDBTable() + " (" + out + ");";
	}
	public String getDBChange(DatabaseMetaData dbmd) throws SQLException {
		if(!umlClass.getDBIndexes(dbmd).contains(getDBName()))
			return getDBCreation();
		return "";
	}
	public String toString(){
		return getDBName();
	}
	@Override
	public int compareTo(UMLIndex o) {
		return toString().compareTo(o.toString());
	}
	public boolean isSameAsPK() {
		Collection<UMLAttribute> pks = umlClass.getPKAttributes();
		if(getColumns().size() != pks.size())
			return false;
		for(UMLAttribute att:pks){
			if(!getColumns().contains(att.getDBName()))
				return false;
		}
		return true;
	}

}
