package com.digitald4.common.tools;


import java.util.TreeSet;

import com.digitald4.common.util.FormatText;

public class KeyConstraint implements Comparable<Object> {
	public final static int ID=1;
	public final static int CHILD=2;
	public final static int PARENT=3;
	private String pkg;
	private String name;
	private String dbName;
	private String refName;
	private String refClass;
	private DomainWriter dao;
	private int type;
	private boolean indexed;
	private TreeSet<FKProperty> properties = new TreeSet<FKProperty>();
	private int reference=1;
	public KeyConstraint(DomainWriter dao, String pkg, String name, String refName, String refClass, String dbName, int type){
		this(dao, pkg, name, refName, refClass, dbName, type, false);
	}
	public KeyConstraint(DomainWriter dao, String pkg, String name, String refName, String refClass, String dbName, int type, boolean indexed){
		this.dao = dao;
		this.pkg = pkg;
		this.name = name;
		this.refName = refName;
		this.refClass = refClass;
		this.dbName = dbName;
		this.type = type;
		this.indexed = indexed;
	}
	public DomainWriter getDAO(){
		return dao;
	}
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name = name;
	}
	public String getRefName(){
		return refName;
	}
	public void setRefName(String refName){
		this.refName = refName;
	}
	public String getRefClass(){
		return refClass;
	}
	public String getDbName(){
		return dbName;
	}
	public int getType(){
		return type;
	}
	public boolean isIndexed() {
		return indexed;
	}
	public int getReference() {
		return reference;
	}
	public void incrementReference(){
		reference++;
	}
	public String getReferenceStr(){
		if(getReference()>1)
			return ""+getReference();
		return "";
	}
	public TreeSet<FKProperty> getProperties(){
		return properties;
	}
	public void addProperty(FKProperty prop){
		getProperties().add(prop);
	}
	
	public String getJavaPackageHeader() {
		return "com.digitald4." + pkg;
	}
	
	public String getJavaRefClass(){
		return FormatText.toUpperCamel(getRefClass());
	}
	public String getJavaName(){
		return FormatText.toUpperCamel(getName());
	}
	public String getJavaRefName(){
		return FormatText.toUpperCamel(getRefName());
	}
	public String getJavaVarName(){
		return FormatText.toLowerCamel(getName())+getReferenceStr();
	}
	public String getJavaCollectionName(){
		if(getType() == CHILD)
			return FormatText.toLowerCamel(getName())+"s"+getReferenceStr();
		return getDAO().getJavaVarName()+"sBy"+getJavaName()+getReferenceStr();
	}
	public String getJavaDeclare(){
		if(getType() == CHILD)
			return "List<"+getJavaRefClass()+"> "+getJavaCollectionName();
		return getJavaRefClass()+" "+getJavaVarName();
	}
	public String getJavaFieldEntry() {
		return "\tprivate "+getJavaDeclare()+";";
	}
	public String getJavaGetMethod(){
		return "get"+getJavaName()+(getType()==CHILD?"s":"")+getReferenceStr()+"()";
	}
	public String getJavaSetMethod(){
		return "set"+getJavaName()+(getType()==CHILD?"s":"")+getReferenceStr()+"("+getJavaRefClass()+" "+getJavaVarName()+")";
	}
	public String getJavaHashtableGetMethod(){
		return "get"+getDAO().getJavaName()+"sBy"+getJavaName()+"()";
	}
	public String getJavaParameterList() {
		String out = "";
		for(FKProperty prop:getProperties()){
			if(out.length() > 0)
				out += DomainWriter.COMMA;
			out += prop.getJavaDeclare();
		}
		return out;
	}
	public String getJavaParameterVars() {
		String out = "";
		for(FKProperty prop:getProperties()){
			if(out.length() > 0)
				out += DomainWriter.COMMA;
			out += prop.getJavaName();
		}
		return out;
	}
	public String getJavaParameterCollectionList() {
		String out = "";
		for(FKProperty prop:getProperties()){
			if(out.length() > 0)
				out += DomainWriter.COMMA;
			out += "pc.get"+FormatText.toUpperCamel(prop.getJavaType())+"(\""+prop.getName()+"\")";
		}
		return out;
	}
	public String getJavaParameterMethods() {
		String out = "";
		for(FKProperty prop:getProperties()){
			if(out.length() > 0)
				out += DomainWriter.COMMA;
			out += prop.getProp().getJavaGetMethod();
		}
		return out;
	}
	public String getJavaCopyEntry() {
		String copy = "\t\tfor("+getJavaRefClass()+" child:"+getJavaGetMethod()+")\n";
		copy+="\t\t\tcp.add"+getJavaName()+getReferenceStr()+"(child.copy());\n";
		return copy;
	}
	public String getJavaCollectionMethod(){
		if(getType() == CHILD)
			return "get"+getJavaName()+"sBy"+getDAO().getJavaName()+getReferenceStr();
		return "get"+getDAO().getJavaName()+"sBy"+getJavaName()+getReferenceStr();
	}
	public String getJavaGetMethodEntry() {
		if(getType() == CHILD){
			String out = "\tpublic List<"+getJavaRefClass()+"> "+getJavaGetMethod()+DomainWriter.FETCH_EXCEPTION_CLASS+"{\n"
					+ "\t\tif (isNewInstance() || "+getJavaCollectionName()+" != null) {\n"
					+ "\t\t\tif (" + getJavaCollectionName() + " == null) {\n"
					+ "\t\t\t\t" + getJavaCollectionName() + " = new SortedList<"+getJavaRefClass()+">();\n"
					+ "\t\t\t}\n"
					+ "\t\t\treturn " + getJavaCollectionName() + ";\n"
					+ "\t\t}\n"
					+ "\t\treturn getNamedCollection(" + getJavaRefClass() + ".class, \"findBy" + getJavaRefName() + getReferenceStr() + "\", " + getJavaParameterMethods() + ");\n"
					+ "\t}\n";
			return out;
		}
		String out = "\tpublic " + getJavaRefClass() + " " + getJavaGetMethod() + DomainWriter.FETCH_EXCEPTION_CLASS + "{\n"
				+ "\t\tif (" + getJavaVarName() + " == null) {\n"
				+ "\t\t\treturn getEntityManager().find(" + getJavaRefClass() + ".class, " + getJavaParameterMethods() + ");\n"
				+ "\t\t}\n"
				+ "\t\treturn " + getJavaVarName() + ";\n"
				+ "\t}\n";
		return out;
	}
	public String getJavaNamedQuery() {
		return "@NamedQuery(name = \"findBy"+getJavaName()+getReferenceStr()+"\", query=\"SELECT o FROM "+dao.getJavaName()+" o WHERE "+getJPQLEntry()+"\"),";
	}
	public String getJavaSetMethodEntry() {
		FKProperty prop = getProperties().last();
		String out = "\tpublic "+dao.getJavaName()+" "+getJavaSetMethod()+DomainWriter.EXCEPTION_CLASS+"{\n";
		out += "\t\t"+prop.getJavaSetMethodHeader()+"("+getJavaVarName()+"==null?null:"+getJavaVarName()+"."+prop.getJavaGetMethod()+");\n";
		out += "\t\tthis."+getJavaVarName()+"="+getJavaVarName()+";\n";
		out += "\t\treturn ("+dao.getJavaName()+")this;\n\t}\n";
		return out;
	}
	public String getJavaAddMethodEntry() {
		String out = "\tpublic "+dao.getJavaName()+" add"+getJavaName()+getReferenceStr()+"("+getJavaRefClass()+" "+getJavaVarName()+")"+DomainWriter.EXCEPTION_CLASS+"{\n";
		out += "\t\t"+getJavaVarName()+".set"+getJavaRefName()+"(("+getDAO().getJavaName()+")this);\n";
		out += "\t\tif(isNewInstance() || "+getJavaCollectionName()+" != null)\n";
		out += "\t\t\t"+getJavaGetMethod()+".add("+getJavaVarName()+");\n";
		out += "\t\telse\n";
		out += "\t\t\t"+getJavaVarName()+".insert();\n";
		out += "\t\treturn ("+dao.getJavaName()+")this;\n\t}\n";
		return out;
	}
	public String getJavaRemoveMethodEntry() {
		String out = "\tpublic "+dao.getJavaName()+" remove"+getJavaName()+getReferenceStr()+"("+getJavaRefClass()+" "+getJavaVarName()+")"+DomainWriter.EXCEPTION_CLASS+"{\n";
		out += "\t\tif(isNewInstance() || "+getJavaCollectionName()+" != null)\n";
		out += "\t\t\t"+getJavaGetMethod()+".remove("+getJavaVarName()+");\n";
		out += "\t\telse\n";
		out += "\t\t\t"+getJavaVarName()+".delete();\n";
		out += "\t\treturn ("+dao.getJavaName()+")this;\n\t}\n";
		return out;
	}
	public String getJavaHashByParentEntry() {
		String vn = "by"+getJavaName()+getReferenceStr();
		String out = "\t\t\tSortedList<"+getDAO().getJavaName()+"> "+vn+" = getCollectionSet(new String[]{"+getPropNames()+"},new Object[]{"+getJavaParameterMethods()+"});\n";
		out += "\t\t\tif("+vn+" != null)\n";
		out += "\t\t\t\t"+vn+".add(("+getDAO().getJavaName()+")this);\n";
		return out;
	}
	public String getJavaHashChildrenEntry() {
		return "\t\t\t"+getJavaName()+".addCollectionSet(new String[]{"+getPropNames()+"},new Object[]{"+getJavaParameterMethods()+"},new TreeSet<"+getJavaName()+">());\n";
	}
	public String getJavaParentInsertEntry(){
		String out = "\t\tif("+getJavaVarName()+" != null && "+getJavaVarName()+".isNewInstance())\n"
			+ "\t\t\t\t"+getJavaVarName()+".insert();\n";
		return out;
	}
	public String getJavaChildInsertSetEntry(){
		String out = "\t\tif ("+getJavaCollectionName()+" != null) {\n"
			+ "\t\t\tfor ("+getJavaRefClass()+" "+getJavaVarName()+" : "+getJavaGetMethod()+") {\n"
			+ "\t\t\t\t"+getJavaVarName()+".set"+getJavaRefName()+"(("+getDAO().getJavaName()+")this);\n"
			+"\t\t\t}\n\t\t}\n";
		return out;
	}
	public String getJavaChildInsertEntry(){
		String out = "\t\tif ("+getJavaCollectionName()+" != null) {\n"
			+ "\t\t\tfor ("+getJavaRefClass()+" "+getJavaVarName()+" : "+getJavaGetMethod()+") {\n"
			+ "\t\t\t\t"+getJavaVarName()+".insert();\n"
			+ "\t\t\t}\n"
			+ "\t\t\t"+getJavaCollectionName()+" = null;\n"
			+ "\t\t}\n";
		return out;
	}
	public String getPropNames(){
		String out = "";
		for(FKProperty prop:getProperties()){
			if(out.length()>0)
				out += ",";
			out += prop.getName();
		}
		return out;
	}
	public String getPropStrings(){
		String out = "";
		for(FKProperty prop:getProperties()){
			if(out.length()>0)
				out += ",";
			out += "\""+prop.getRefColumn()+"\"";
		}
		return out;
	}
	public String getJavaCacheEntry() {
		String out = "\t\tif(by.startsWith(\"All\") || by.equals(\""+getJavaName()+"\")){\n"
			+ "\t\t\tList<"+getDAO().getJavaName()+"> collection = getCollectionSet(new String[]{"+getPropNames()+"},new Object[]{"+getJavaParameterMethods()+"});\n"
			+ "\t\t\tif(collection == null){\n"
			+ "\t\t\t\tcollection = new SortedList<"+getDAO().getJavaName()+">();\n"
			+ "\t\t\t\taddCollectionSet(new String[]{"+getPropNames()+"},new Object[]{"+getJavaParameterMethods()+"},collection);\n"
			+ "\t\t\t}\n"
			+ "\t\t\tcollection.add(("+getDAO().getJavaName()+")this);\n"
			+ "\t\t}\n";
		return out;
	}
	public String toString(){
		return getName();
	}
	public int compareTo(Object o) {
		int ret = getDbName().compareTo(((KeyConstraint)o).getDbName());
		if(ret == 0)
			ret = toString().compareTo(""+o);
		return ret;
	}
	public String getJPQLEntry() {
		String out="";
		int i=1;
		for(FKProperty prop:getProperties()){
			if(i > 1)
				out += " AND ";
			out += "o."+prop.getName()+"=?"+(i++);
		}
		return out;
	}
	
	public String getSQLEntry() {
		String out="";
		for(FKProperty prop:getProperties()){
			if(out.length() > 0)
				out += " AND ";
			out += "o."+prop.getName()+"=?";
		}
		return out;
	}
	
	public boolean contains(Property prop){
		for(FKProperty fkp:getProperties())
			if(fkp.getProp() == prop)
				return true;
		return false;
	}
}
