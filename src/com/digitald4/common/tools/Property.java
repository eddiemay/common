package com.digitald4.common.tools;


import java.util.NoSuchElementException;

import com.digitald4.common.util.FormatText;

public class Property implements Comparable<Object>{
	private int index;
	private String name;
	private FieldType type;
	private String defaultValue;
	private String size;
	private boolean nullable;
	private String comment;
	private boolean hasSequence;
	private boolean deprecated;
	private boolean abandoned;
	private DomainWriter dao;
	private boolean generated;
	
	public Property(DomainWriter dao, int index, String name, FieldType type, String size){
		this(dao,index,name,type,size,false,null);
	}
	public Property(DomainWriter dao, int index, String name, FieldType type, String size, boolean nullable){
		this(dao,index,name,type,size,nullable,null);
	}
	public Property(DomainWriter dao, int index, String name, FieldType type, String size, boolean nullable, String defaultValue){
		this.dao = dao;
		this.index = index;
		this.name = name;
		this.type = type;
		this.size = size;
		this.nullable = nullable;
		this.defaultValue = defaultValue;
	}
	
	public int getIndex(){
		return index;
	}
	
	public String getName(){
		return name;
	}
	
	public FieldType getType(){
		return type;
	}
	
	public boolean isNullable(){
		return nullable;
	}
	
	public String getDefaultValue(){
		return defaultValue;
	}
	
	public String getSize(){
		return size;
	}
	
	public String getJavaName(){
		return FormatText.toLowerCamel(getName());
	}
	
	public String getJavaType(){
		return type.getJavaClass().getSimpleName();
	}
	public Class<?> getJavaClass(){
		return type.getJavaClass();
	}
	
	public String getJavaConstName(){
		return getJavaName().toUpperCase();
	}
	
	public String getJavaDefaultValue(){
		String dv = getDefaultValue();
		if(dv==null) return null;
		String dim="";
		if(getJavaClass() == String.class)
			dim="\"";
		if(getJavaClass() == boolean.class)
			dv=dv.equals("1")?"true":"false";
		return dim+dv+dim;
	}
	
	public String getJavaDeclare(){
		return getJavaType()+" "+getJavaName();
	}
	
	public String getJavaFieldEntry() {
		String out = ""
			+"\tprivate "+getJavaDeclare();
		if(getJavaDefaultValue() != null)
			out += " = "+getJavaDefaultValue();
		return out+";";
	}
	
	public String getJavaLimitEntry(){
		return "\tpublic final static int "+getJavaConstName()+"_LIMIT = "+getSize()+";";
	}
	
	public int compareTo(Object o){
		if(o instanceof Property){
			Property prop = (Property)o;
			if(getIndex() < prop.getIndex())
				return -1;
			if(getIndex() > prop.getIndex())
				return 1;
		}
		return toString().compareTo(""+o);
	}

	public String getJavaGetMethod() {
		return ((getJavaType().equals("boolean"))?"is":"get")+FormatText.toUpperCamel(getName())+"()";
	}
	
	
	public String getJavaSetMethodHeader(){
		String out = "set"+FormatText.toUpperCamel(getName());
		return out;
	}
	
	public String getJavaSetMethod() {
		return getJavaSetMethodHeader()+"("+getJavaType()+" "+getJavaName()+")";
	}

	public String getJavaGetMethodEntry() {
		String out = "";
		out+="\t@Column(name=\""+getName()+"\",nullable="+isNullable()+(getSize()==null?"":",length="+getSize())+")\n" 
			+"\tpublic "+getJavaType()+" "+getJavaGetMethod()+"{\n"
			+ "\t\treturn "+getJavaName()+";\n"
			+ "\t}\n";
		return out;
	}
	
	public String getJavaSetMethodEntry() {
		String out = "\tpublic "+dao.getJavaName()+" "+getJavaSetMethod()+DomainWriter.EXCEPTION_CLASS+" {\n"
			+ "\t\t"+getJavaType()+" oldValue = "+getJavaGetMethod()+";\n"
			//+ "\t\tObject oldValue = null;\n"
			+ "\t\tif (!isSame(" + getJavaName() + DomainWriter.COMMA + "oldValue)) {\n"
			+ "\t\t\tthis." + getJavaName() + " = " + getJavaName()+";\n"
			+ "\t\t\tsetProperty(\"" + getName() + "\"" + DomainWriter.COMMA + getJavaName() + DomainWriter.COMMA + "oldValue);\n";
		for(KeyConstraint kc:dao.getParents()){
			try {
				if (kc.getProperties().last().getProp( )== this) {
					out+="\t\t\t"+kc.getJavaVarName()+"=null;\n";
				}
			} catch (NoSuchElementException nsee) {
				System.out.println("Can not find properties for Reference: "+kc.getName());
				throw nsee;
			}
		}
		out+="\t\t}\n\t\treturn ("+dao.getJavaName()+")this;\n\t}\n";

		return out;
	}
	
	public String getJavaGetPVEntry(){
		String out = "\t\t\tcase "+getName()+": return "+getJavaGetMethod()+";\n";
		return out;
	}
	
	public String getJavaSetPVEntry(){
		String parseCode = type.getParseCode();
		if (parseCode == null) {
			return "";
		}
		return "\t\t\tcase " + getName() + ":" + getJavaSetMethodHeader()+"(" + parseCode +"(value)); break;\n";
	}

	public String getJavaDiffEntry() {
		String diff = "\t\tif(!isSame("+getJavaGetMethod()+",o."+getJavaGetMethod()+")) diffs.add(\""+getName()+"\");\n";
		return diff;
	}
	
	public String getJavaPropertyType(){
		return getJavaType();
	}
	
	public String getJavaRefreshEntry() {
		String out = "\t\t"+getJavaName()+" = pc.get"+getJavaPropertyType()+"(\""+getName()+"\");\n";
		return out;
	}
	
	public String getJavaInsertStatusEntry(){
		return "\t\tif(isNull("+getJavaGetMethod()+")) errors.add(\""+getName()+" is Required.\");\n";
	}
	public boolean isGloballyHandled() {
		return (getName().equalsIgnoreCase("insert_ts") || getName().equalsIgnoreCase("Modified_ts") || getName().equalsIgnoreCase("deleted_ts") || getName().equalsIgnoreCase("insert_user_id") || getName().equalsIgnoreCase("Modified_user_id") || getName().equalsIgnoreCase("deleted_user_id"));
	}
	public void setHasSequence(boolean hasSequence) {
		this.hasSequence = hasSequence;
	}
	public boolean hasSequence() {
		return hasSequence;
	}
	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}
	public boolean isDeprecated() {
		return deprecated;
	}
	public void setAbandoned(boolean abandoned) {
		this.abandoned = abandoned;
	}
	public boolean isAbandoned() {
		return abandoned;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getComment() {
		return comment;
	}
	public void setGenerated(boolean generated) {
		this.generated = generated;
	}
	public boolean isGenerated(){
		return generated;
	}
}
