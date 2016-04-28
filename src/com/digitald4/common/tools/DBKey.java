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

import java.util.TreeSet;

import com.digitald4.common.util.FormatText;
/**
 * 
 * @author Distribution Staff Engineering
 * @version 2.0
 */
public class DBKey implements Comparable<Object>{
	public final static int PRIMARY_KEY=1;
	public final static int FOREIGN_KEY=2;
	private String name;
	private int type=FOREIGN_KEY;
	private String fKTable;
	private boolean indexed;
	private int reference=1;
	private TreeSet<Attribute> columns = new TreeSet<Attribute>();
	
	public DBKey(String name, int type){
		this.name = name;
		this.type = type;
	}
	public DBKey(String name, String pKTable, boolean indexed){
		this.name = name;
		this.fKTable = pKTable;
		this.indexed = indexed;
	}
	public boolean isIndexed(){
		return indexed;
	}
	public void setIndexed(boolean indexed){
		this.indexed = indexed;
	}
	public int getReference(){
		return reference;
	}
	public void setReference(int reference){
		this.reference = reference;
	}
	public String getName(){
		return name;
	}
	public int getType(){
		return type;
	}
	public String getFKTable(){
		return fKTable;
	}
	public String getClassName(){
		return FormatText.toUpperCamel(fKTable.substring(fKTable.indexOf('_')+1));
	}
	public String getVarName(){
		return FormatText.toLowerCamel(fKTable.substring(fKTable.indexOf('_')+1))+getRefStr();
	}
	public String getCollectionName(){
		return FormatText.toLowerCamel(fKTable.substring(fKTable.indexOf('_')+1))+"s"+getRefStr();
	}
	public void addColumn(Attribute column){
		columns.add(column);
	}
	public TreeSet<Attribute> getColumns(){
		return columns;
	}
	public String toString(){
		return name+"\t"+fKTable+"\t"+columns.toString();
	}
	public int compareTo(Object o){
		return getName().compareTo(((DBKey)o).getName());
	}
	public String getWhereClauae(){
		String wc = "";
		for(Attribute att:columns){
			if(wc.length() > 0)
				wc += " AND ";
			wc += att.getColumnName()+"=?";
		}
		return " WHERE "+wc;
	}
	public String getGetInstance(String trueFalse){
		String instance="getInstance(";
		boolean first=true;
		for(Attribute att:getColumns()){
			if(!first)
				instance+=",";
			instance+=att.getGetMethodHeader();
			first=false;
		}
		if(trueFalse != null && trueFalse.length() > 0)
			instance += ",";
		return instance+trueFalse+");";
	}
	public String getRefStr() {
		if(reference<2)
			return "";
		return ""+reference;
	}
}
