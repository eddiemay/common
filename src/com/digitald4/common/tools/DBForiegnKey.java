package com.digitald4.common.tools;


public class DBForiegnKey {
	private String name;
	private String table;
	private String refTable;
	private String[] columns = new String[10];
	private String[] refCols = new String[10];
	private int deleteRule;
	
	public DBForiegnKey(String table, String name, String refTable){
		this.name = name;
		this.table = table;
		this.refTable = refTable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getRefTable() {
		return refTable;
	}

	public void setRefTable(String refTable) {
		this.refTable = refTable;
	}
	
	public void setColumn(int index, String name){
		columns[index] = name;
	}
	
	public void setRefCol(int index, String name){
		refCols[index] = name;
	}
	
	public String getColumn(int index){
		return columns[index];
	}
	
	public String getRefCol(int index){
		return refCols[index];
	}
	
	public String getDBCreation(){
		String out1="", out2="";
		for(int i=1; i<columns.length; i++){
			if(columns[i]!=null){
				if(out1.length()>0){
					out1+=",";
					out2+=",";
				}
				out1+=columns[i];
				out2+=refCols[i];
			}
		}
		return "CONSTRAINT "+getName()+" FOREIGN KEY ("+out1+") REFERENCES MDI."+getRefTable()+" ("+out2+") ON DELETE "+getDeleteRule();
	}

	public void setDeleteRule(int deleteRule) {
		this.deleteRule = deleteRule;
	}
	public String getDeleteRule(){
		switch(deleteRule){
			case 1: return "NO ACTION";
			case 0: return "CASCADE";
			case 2: return "SET NULL";
		}
		return "";
	}
}
