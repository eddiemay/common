package com.digitald4.common.tools;


import com.digitald4.common.util.FormatText;

public class FKProperty implements Comparable<Object>{
	private Property prop;
	private String refColumn;
	private int index;
	public FKProperty(Property prop, int index) {
		this(prop,index,prop.getName());
	}
	public FKProperty(Property prop, int index, String refColumn) {
		this.prop = prop;
		this.index = index;
		this.refColumn = refColumn;
	}
	public Property getProp(){
		return prop;
	}
	public String getRefColumn(){
		return refColumn;
	}
	public void setRefColumn(String refColumn){
		this.refColumn = refColumn;
	}
	public int getIndex(){
		return index;
	}
	public String getJavaRefColumnName(){
		return FormatText.toLowerCamel(getRefColumn());
	}
	public String getJavaGetMethod(){
		return "get"+FormatText.toUpperCamel(getRefColumn())+"()";
	}
	public String getJavaName() {
		return getProp().getJavaName();
	}
	public String getJavaType(){
		return getProp().getJavaType();
	}
	public String getName(){
		return prop.getName();
	}
	public String getJavaSetMethodHeader() {
		return prop.getJavaSetMethodHeader();
	}
	public String getJavaDeclare() {
		return prop.getJavaDeclare();
	}
	public int compareTo(Object o){
		if(o instanceof FKProperty){
			FKProperty pkp = (FKProperty)o;
			if(getIndex() < pkp.getIndex())
				return -1;
			if(getIndex() > pkp.getIndex())
				return 1;
		}
		return toString().compareTo(toString());
	}
}
