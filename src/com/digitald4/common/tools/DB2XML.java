package com.digitald4.common.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import com.digitald4.common.log.EspLogger;
import com.digitald4.common.util.FormatText;

public class DB2XML {
	
	public static UMLClass genFromOracleDB(Connection conn, String schema, String table) throws SQLException{
		DatabaseMetaData dbmd = conn.getMetaData();
		UMLClass uc = new UMLClass(table.substring(table.indexOf("_")+1).replaceAll("_", " "));
		uc.setTablePrefix(table.substring(0, table.indexOf("_")));
		uc.setSuperClass(getSuperClass(table));
		ResultSet rs = dbmd.getTablePrivileges(null, schema, table);
		while(rs.next()){
			String prev = rs.getString("PRIVILEGE");
			String grantee = rs.getString("GRANTEE");
			if(prev.equals("SELECT"))
				uc.setSelectRole(grantee);
			if(prev.equals("INSERT"))
				uc.setInsertRole(grantee);
			if(prev.equals("UPDATE"))
				uc.setUpdateRole(grantee);
			if(prev.equals("DELETE"))
				uc.setDeleteRole(grantee);
		}
		rs.close();
		
		PreparedStatement ps = conn.prepareStatement("SELECT COMMENTS REMARKS FROM user_col_comments WHERE table_name=? AND column_name=?");
		rs = dbmd.getColumns(null,schema,table,null);
		while(rs.next()){
			String colName = rs.getString("COLUMN_NAME");
			int type = getColumnTypeFromDB(table,colName,rs.getInt("DATA_TYPE"),rs.getInt("COLUMN_SIZE"),rs.getInt("DECIMAL_DIGITS"));
			if(!isGloballyHandled(colName) && type != Types.BLOB){
				String def = rs.getString("COLUMN_DEF");
				if(def != null){
					if(def.contains("'"))
						def = def.replaceAll("'", "");
					def = def.trim();
				}
				UMLAttribute attr = new UMLAttribute(uc,colName.replaceAll("_", " "));
				attr.setType(getJavaType(type));
				attr.setSize(rs.getString("COLUMN_SIZE"));
				attr.setDefault(def);
				attr.setNullable(rs.getInt("NULLABLE")!=ResultSetMetaData.columnNoNulls);
				ps.setString(1, table);
				ps.setString(2, colName);
				ResultSet rsC = ps.executeQuery();
				if(rsC.next()){
					String comment = rsC.getString("REMARKS");
					attr.setDesc(comment);
					if(comment != null){
						//prop.setAbandoned(comment.toUpperCase().contains("ABANDONED"));
						//prop.setDeprecated(comment.toUpperCase().contains("DEPRECATED"));
						attr.setSequence(comment.toUpperCase().contains("AUTO_INCREMENT")?"SEQ":null);
					}
				}
				rsC.close();
				//if(!prop.isAbandoned())
					uc.addAttribute(attr);
			}
		}
		rs.close();
		ps.close();
		rs = dbmd.getPrimaryKeys(null,schema,table);
		while(rs.next()){
			String colName = rs.getString("COLUMN_NAME").replaceAll("_", " ");
			for(UMLAttribute attr:uc.getAttributes()){
				if(attr.getName().equals(colName)){
					attr.setId(true);
					break;
				}
			}
		}
		rs.close();
		
		Hashtable<String,UMLReference> pFKHash = new Hashtable<String,UMLReference>();
		rs = dbmd.getCrossReference(null,schema,null,null,schema,table);
		while(rs.next()){
			if(!DB2XML.isGloballyHandled(rs.getString("FKCOLUMN_NAME"))){
				UMLReference ref = pFKHash.get(rs.getString("FK_NAME"));
				if(ref == null){
					String pkTable = rs.getString("PKTABLE_NAME");
					String name = pkTable.substring(pkTable.indexOf('_')+1).replaceAll("_", " ");
					ref = new UMLReference(uc,name,rs.getString("FK_NAME").substring(rs.getString("FK_NAME").indexOf("K")+1),rs.getString("FK_NAME").endsWith("I"));
					pFKHash.put(rs.getString("FK_NAME"),ref);
				}
				ref.addConnector(new UMLConnector(ref,rs.getString("FKCOLUMN_NAME").replaceAll("_", " "),rs.getString("PKCOLUMN_NAME").replaceAll("_", " ")));
			}
		}
		rs.close();
		for(UMLReference ref:new TreeSet<UMLReference>(pFKHash.values()))
			if(ref.getConnectors().size() > 0)
				uc.addReference(ref);
		return uc;
	}
	
	public static boolean isGloballyHandled(String name) {
		return (name.equalsIgnoreCase("insert_ts") || name.equalsIgnoreCase("Modified_ts") || name.equalsIgnoreCase("deleted_ts") || name.equalsIgnoreCase("insert_user_id") || name.equalsIgnoreCase("Modified_user_id") || name.equalsIgnoreCase("deleted_user_id"));
	}
	
	public static String getSuperClass(String table){
		String javaName = FormatText.toUpperCamel(table.substring(table.indexOf("_")));
		if(javaName.equals("Department") || javaName.equals("Zone") || javaName.equals("Region") || javaName.equals("District") || javaName.equals("Sys"))
			return "Ag Object";
		if(!javaName.equals("ProjRevFile") && (javaName.endsWith("File") || javaName.startsWith("File")))
			return "Blob File";
		if(javaName.endsWith("Daily") && !javaName.contains("Sys"))
			return "Daily Peak";
		if(javaName.equals("DepartmentYear") || javaName.equals("SysYear"))
			return "Ag Year";
		if(javaName.equals("AbankYear") || javaName.equals("BbankYear"))
			return "Sub Year";
		if(javaName.equals("Abank") || javaName.equals("Bbank"))
			return "Sub";
		if(table.startsWith("MDI") && !table.startsWith("MDIS") && !table.startsWith("MDIT") && !javaName.equals("Org"))
			return "MDI Object";
		return "Data Access Object";
	}
	
	public static int getColumnTypeFromDB(String table, String colName, int type, int columnSize, int decimalDigits) {
		String javaName = FormatText.toUpperCamel(table.substring(table.indexOf("_")));
		if(type == Types.DECIMAL && decimalDigits > 0)
			type = Types.DOUBLE;
		else if(type == Types.DECIMAL && columnSize == 1)
			type = Types.BOOLEAN;
		else if(type == Types.DECIMAL && columnSize > 9)
			type = Types.BIGINT;
		else if((type==Types.DATE || type==Types.TIMESTAMP) && colName.endsWith("TIME") && !javaName.equals("CktEdnaRead"))
			type = Types.TIME;
		else if(type == Types.DATE && !colName.endsWith("DATE"))
			type = Types.TIMESTAMP;
		return type;
	}
	
	public static String getJavaType(int type){
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
				
			case Types.CLOB:
				return "Clob";
		}
		return "Unknown"+type;
	}
	public static void runTables(String schema, String pattern)throws Exception{
		pattern = pattern.toUpperCase();
		TreeSet<String> tables = new TreeSet<String>();
		Class.forName("oracle.jdbc.OracleDriver");
		Connection con = DriverManager.getConnection("", "mdi", "edison");
		DatabaseMetaData dbmd = con.getMetaData();
		ResultSet rs = dbmd.getTables(null,schema,pattern,new String[]{"TABLE"});
		while(rs.next()){
			String table = rs.getString("TABLE_NAME");
			EspLogger.message(DomainWriter.class,"Table found: "+table);
			if(DomainWriter.isGoodTable(table))
				tables.add(table);
		}

		EspLogger.message(DomainWriter.class,"Running the following tables in 5 secs:");
		for(String table:tables)
			EspLogger.message(DomainWriter.class,"\t"+table.substring(table.indexOf('_')+1)+"\t"+table);
		//		Thread.sleep(5000);
		for(String table:tables){
			String className = table.substring(table.indexOf('_')+1);
			EspLogger.message(DomainWriter.class,className);
			genFromOracleDB(con, schema, table);
		}
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		DomainWriter.init();
		runTables("MDI",JOptionPane.showInputDialog("Input table pattern"));
		UMLClass.save("src/conf/ESP_SchemaOut.xml");
	}

}
