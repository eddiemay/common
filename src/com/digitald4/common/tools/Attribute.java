/*
 * Copyright (c) 2002-2010 ESP Suite. All Rights Reserved.
 *
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Authors: Technology Integration Group, SCE
 * Developers: Eddie Mayfield, Frank Gonzales, Augustin Muniz,
 * Kate Suwan, Hiro Kushida, Andrew McNaughton, Brian Stonerock,
 * Russell Ragsdale, Patrick Ridge, Everett Aragon.
 * 
 */
package com.digitald4.common.tools;

import java.sql.Types;
import java.util.TreeSet;

import com.digitald4.common.util.FormatText;
/**
 *
 * @author Distribution Staff Engineering
 * @version 2.0
 */
public class Attribute implements Comparable<Object>{
	private int index;
	private int type;
	private int size;
	private String table;
	private String dbCol;
	private String pkCol;
	private boolean nullable;
	private boolean primaryKey;
	private boolean autoNumbered;
	private String defaultValue;
	private boolean abandoned;
	private boolean deprecated;

	public Attribute(int index, String table, String dbCol, int type){
		this(index,table,dbCol,type,0,true,false,false,dbCol);
	}
	public Attribute(int index, String table, String dbCol, int type, String pkCol){
		this(index,table,dbCol,type,0,true,false,false,pkCol);
	}
	public Attribute(int index, String table, String dbCol, int type, int size, boolean nullable, boolean primaryKey, boolean autoNumbered){
		this(index,table,dbCol,type,size,nullable,primaryKey,autoNumbered,dbCol);
	}
	public Attribute(int index, String table, String dbCol, int type, int size, boolean nullable, boolean primaryKey, boolean autoNumbered, String pkCol){
		this.index = index;
		this.table = table;
		this.type = type;
		this.size = size;
		this.dbCol = dbCol;
		this.nullable = nullable;
		this.primaryKey = primaryKey;
		this.autoNumbered = autoNumbered;
		this.pkCol = pkCol;
	}
	public String getClassName(){
		return FormatText.toUpperCamel(table.substring(table.indexOf('_')+1));
	}
	public int getIndex(){
		return index;
	}
	public boolean isNullable(){
		return nullable;
	}
	public boolean isPrimaryKey(){
    	return primaryKey;
    }
	public String getName(){
		return FormatText.toLowerCamel(dbCol);
	}
	public String getPKName(){
		return FormatText.toLowerCamel(pkCol);
	}
	public int getSize(){
		return size;
	}
	public void setSize(int size){
		this.size = size;
	}
	public String getLimitConst(){
		return "\tpublic final static int "+getName().toUpperCase()+"_LIMIT = "+getSize()+";";
	}
	public void setDefaultValue(String defaultValue){
		if(defaultValue != null){
			if(getType() == Types.VARCHAR)
				this.defaultValue = defaultValue.replaceAll("'", "\"");
			else if(getType() == Types.BOOLEAN)
				this.defaultValue = defaultValue.equals("0")?"false":"true";
			else
				this.defaultValue = defaultValue;
		}
		else
			this.defaultValue = defaultValue;
	}
	public String getDefaultValue(){
		return defaultValue;
	}
	public String getColumnName(){
		return dbCol;
	}
	public int getType(){
		return type;
	}
	public boolean isAutoNumbered(){
		return autoNumbered;
	}
	public void setAutoNumbered(boolean autoNumbered){
		this.autoNumbered = autoNumbered;
	}
	public String getProcessCol(){
		switch(type){
			case Types.VARCHAR:
				return ":NEW."+dbCol;
			case Types.DATE:
			case Types.TIMESTAMP:
				return "TO_CHAR(:NEW."+dbCol+",'YYYY-MM-DD')";
		}
		return "TO_CHAR(:NEW."+dbCol+")";
	}
	public String getPKSetEntry(){
    	if(getDataType().equals("Calendar"))
    		return "\t\tps.setDate(si++,getDate("+getGetMethodHeader()+"));";
    	return "\t\tps.set"+getDataType().substring(0,1).toUpperCase()+getDataType().substring(1)+"(si++,"+getGetMethodHeader()+");";
    }
	public String getDBSetEntry(){
    	if(type == Types.DATE)
    		return "\t\tps.setDate(si++,getDate("+getGetMethodHeader()+"));";
    	else if(type == Types.TIMESTAMP)
    		return "\t\tps.setTimestamp(si++,getTimestamp("+getGetMethodHeader()+"));";
    	else if(type == Types.CHAR)
    		return "\t\tps.setString(si++,\"\"+"+getGetMethodHeader()+");";
    	else if(type == Types.BLOB)
    		return "\t\tps.setBinaryStream(si++,new java.io.ByteArrayInputStream(getInputBytes()),getInputBytes().length);";
    	else if(dbCol.endsWith("_ID") && !dbCol.equals("SIM_ID") && type != Types.VARCHAR){
    		return "\t\tps.setObject(si++,getId("+getGetMethodHeader()+"));";
    	}
    	return "\t\tps.set"+getDataType().substring(0,1).toUpperCase()+getDataType().substring(1)+"(si++,"+getGetMethodHeader()+");";
    }
	public String getDeclare(){
		String declare = "\tprivate "+getDataType()+" "+getName();
		if(getDefaultValue() != null)
			declare += " = "+getDefaultValue();
		return declare+"; //"+dbCol;
	}
	public String getGetMethodHeader(){
		return (getDataType().equals("boolean")?"is":"get")+getName().substring(0,1).toUpperCase()+getName().substring(1)+"()";
	}
	public String getPKGetMethodHeader(){
		return (getDataType().equals("boolean")?"is":"get")+getPKName().substring(0,1).toUpperCase()+getPKName().substring(1)+"()";
	}
	public String getSetMethodHeader(){
		return "set"+getName().substring(0,1).toUpperCase()+getName().substring(1);
	}
    public String getGetMethod(boolean simable){
        String javadoc = "\t/**\n"
        +"\t * Returns the "+getName()+"\n"
        +"\t * @return The "+getName()+"\n"
        +"\t */\n";
        String get = "\t"+((isDeprecated())?"protected ":"public ")+getDataType()+" "+getGetMethodHeader()+"{\n";
        if(!isPrimaryKey() && simable)
        	get += "\t\tif(ptr != null)\n"
			+"\t\t\treturn ptr."+getGetMethodHeader()+";\n";
        get += "\t\treturn "+getName()+";\n"
        +"\t}\n";
        return javadoc+get;
    }
    public String getSetMethod(TreeSet<DBKey> pFKs){
        String javadoc = "\t/**\n"
        +"\t * Sets the "+getName()+"\n"
        +"\t * @param The new "+getName()+"\n"
        +"\t */\n";
        String set = "\t"+((isDeprecated())?"protected ":"public ")+"void set"+getName().substring(0,1).toUpperCase()+getName().substring(1)+"("+getDataType()+" "+getName()+")throws SQLException{\n";
        if(isPrimaryKey()){
        	set+="\t\tif(!isNewInstance())\n"
        		+"\t\t\tthrow new SQLException(\"Changing of primary key of existing record is not allowed.\");\n";
        	set+="\t\tthis."+getName()+" = "+getName()+";\n";
        }
        else{
	        set+="\t\tif(isSame("+getName()+","+getGetMethodHeader()+")) return;\n";
	        if(dbCol.endsWith("_ID") && !dbCol.equals("SIM_ID") && type != Types.VARCHAR){
	        	set+="\t\tif("+getName()+" == 0)\n"
	        	+"\t\t\tsetProperty(\""+dbCol+"\",null);\n"
	        	+"\t\telse\n"
	        	+"\t";
	        }
	        set+="\t\tsetProperty(\""+dbCol+"\","+getName()+");\n";
	        DBKey dbkey = getDBKey(pFKs);
	        if(dbkey != null && dbkey.isIndexed()){
	        	set+="\t\tif(!isNewInstance()){\n"
	        	+"\t\t\t"+dbkey.getVarName()+" = "+dbkey.getClassName()+"."+dbkey.getGetInstance("false")+"\n"
	        	+"\t\t\tif("+dbkey.getVarName()+" != null && "+dbkey.getVarName()+".has"+getClassName()+"s"+dbkey.getRefStr()+"BeenRead())\n"
				+"\t\t\t\t"+dbkey.getVarName()+".refresh"+getClassName()+"s"+dbkey.getRefStr()+"();\n"
				+"\t\t}\n";
	        }
	        set+="\t\tthis."+getName()+" = "+getName()+";\n";
	        if(dbkey != null){
		        if(dbkey.isIndexed()){
		        	set+="\t\tif(!isNewInstance()){\n"
		        	+"\t\t\t"+dbkey.getVarName()+" = "+dbkey.getClassName()+"."+dbkey.getGetInstance("false")+"\n"
		        	+"\t\t\tif("+dbkey.getVarName()+" != null && "+dbkey.getVarName()+".has"+getClassName()+"s"+dbkey.getRefStr()+"BeenRead())\n"
					+"\t\t\t\t"+dbkey.getVarName()+".refresh"+getClassName()+"s"+dbkey.getRefStr()+"();\n"
		        	+"\t\t}\n";
	        	}
		        else
		        	set+="\t\t"+dbkey.getVarName()+" = null;\n";
	        }
        }
        set+="\t}\n";
        return javadoc+set;
    }
    private DBKey getDBKey(TreeSet<DBKey> pFKs){
    	if(pFKs == null)
    		return null;
    	for(DBKey dbkey:pFKs)
    		if(dbkey.getColumns().last().getColumnName().equals(getColumnName()))
    			return dbkey;
    	return null;
    }
    public String getRefreshEntry(TreeSet<DBKey> pFKs){
    	DBKey dbkey = getDBKey(pFKs);
    	if(dbkey != null){
    		if(dbkey.isIndexed()){
	    		String ret="if(!newInstance && "+getGetMethodHeader()+" != "+getDBGetString()+"){\n";
	    		ret+="\t\t\t"+dbkey.getClassName()+" "+dbkey.getVarName()+" = "+dbkey.getClassName()+"."+dbkey.getGetInstance("false")+"\n"
	    		+"\t\t\tif("+dbkey.getVarName()+" != null && "+dbkey.getVarName()+".has"+getClassName()+"s"+dbkey.getRefStr()+"BeenRead())\n"
	    		+"\t\t\t\t"+dbkey.getVarName()+".refresh"+getClassName()+"s"+dbkey.getRefStr()+"();\n";
	    		ret+="\t\t\t"+getName()+" = "+getDBGetString()+";\n";
	    		ret+="\t\t\t"+dbkey.getVarName()+" = "+dbkey.getClassName()+"."+dbkey.getGetInstance("false")+"\n"
	    		+"\t\t\tif("+dbkey.getVarName()+" != null && "+dbkey.getVarName()+".has"+getClassName()+"s"+dbkey.getRefStr()+"BeenRead())\n"
	    		+"\t\t\t\t"+dbkey.getVarName()+".refresh"+getClassName()+"s"+dbkey.getRefStr()+"();\n";
	    		ret+="\t\t}\n";
	    		ret+="\t\telse\n";
	    		ret+="\t\t\t"+getName()+" = "+getDBGetString()+";";
	    		return ret;
    		}
    		return "if(!newInstance && "+getGetMethodHeader()+" != "+getDBGetString()+")\n"
    			+"\t\t\t"+dbkey.getVarName()+" = null;\n"
    			+"\t\t"+getName()+" = "+getDBGetString()+";";
    	}
    	return getName()+" = "+getDBGetString()+";";
    }
    public String getNullTest(){
    	if(type == Types.BLOB)
    		return "\t\tif(isNull(getInputBytes())) errors.add(\"A File is Required\");\n";
    	return "\t\tif(isNull("+getGetMethodHeader()+")) errors.add(\""+getColumnName().replace('_',' ')+" is Required.\");\n";
    }
    public String getDBGetString(){
    	if(type == Types.DATE)
			return "getCalendar(rs.getDate(\""+dbCol+"\"))";
    	else if(type == Types.TIMESTAMP)
    		return "getCalendar(rs.getTimestamp(\""+dbCol+"\"))";
    	return "rs.get"+getDataType().substring(0,1).toUpperCase()+getDataType().substring(1)+"(\""+dbCol+"\")";
    }
    public String getDataType(){
		switch(type){
			case Types.BIGINT:
				return "long";

			case Types.NUMERIC:
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.DECIMAL:
				return "int";

			case Types.DOUBLE:
			case Types.FLOAT:
				return "double";

			case Types.TINYINT:
			case Types.BIT:
			case Types.BOOLEAN:
				return "boolean";
				
			case Types.CHAR:
				return "char";

			case Types.VARCHAR:
				return "String";

			case Types.TIMESTAMP:
			case Types.DATE:
				return "Calendar";

			case Types.TIME:
				return "Time";
		}
		return "Unknown: "+type;
	}
	public String toString(){
		return getDataType()+" "+getName();
	}
	public int compareTo(Object o){
		int ret=0;
		if(o instanceof Attribute){
			if(index != ((Attribute)o).getIndex())
				ret = (index < ((Attribute)o).getIndex())?-1:1;
			else
				ret = getName().compareTo(((Attribute)o).getName());
		}
		else
			ret = toString().compareTo(o.toString());
		return ret;
	}
	public boolean isStandard() {
		return type != Types.BLOB && !getColumnName().startsWith("MODIFIED") && !getColumnName().startsWith("INSERT") && !getColumnName().startsWith("DELETED");
	}
	public void setAbandoned(boolean abandoned) {
		this.abandoned = abandoned;
	}
	public boolean isAbandoned(){
		return abandoned;
	}
	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}
	public boolean isDeprecated(){
		return deprecated;
	}
}
