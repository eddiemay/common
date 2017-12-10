package com.digitald4.common.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by eddiemay on 10/30/16.
 */
public class ProtoToSQLConverter {
	private final static String ENTITY_START = "message ";
	private final static String CREATE_TABLE = "CREATE TABLE %s(\n"
			+ "%s\n" // Columns
			+ ");\n";
	private final static String DB_COLUMN = "  %s %s";
	private final static String DB_REQUIRED = " NOT NULL";
	private final static String DB_ID = DB_COLUMN + DB_REQUIRED + " AUTO_INCREMENT PRIMARY KEY";

	private final String protoFile;

	public ProtoToSQLConverter(String protoFile) {
		this.protoFile = protoFile;
	}

	public void execute() {
		String entity = null;
		String columns = "";
		int depth = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(protoFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				try {
					if (entity != null) {
						if (line.startsWith("}")) {
							if (--depth == 0) {
								System.out.println(String.format(CREATE_TABLE, entity, columns));
								entity = null;
							}
						} else if (line.contains("=") && depth == 1 && !line.startsWith("//")) {
							int i = line.indexOf('=');
							while (line.charAt(--i) == ' ') ;
							while (line.charAt(--i) != ' ') ;
							String colName = line.substring(i, line.indexOf('=')).trim();
							int j = i;
							while (line.charAt(--j) == ' ') ;
							while (line.charAt(--j) != ' ' && j > 0) ;
							String type = line.substring(j, i).trim();
							String dbType = null;
							switch (type) {
								case "bool":
									dbType = "tinyint(1)";
									break;
								case "int32":
									dbType = "int(11)";
									break;
								case "int64":
									dbType = colName.endsWith("id") ? "BIGINT" : "datetime";
									break;
								case "float":
									dbType = "float";
									break;
								case "double":
									dbType = "double";
									break;
								case "string":
									dbType = "varchar(128)";
									break;
								case "bytes":
									dbType = "blob";
									break;
								default:
									dbType = "varchar(1024)";
									break;
							}
							if (columns.length() > 0) {
								columns += ",\n";
							}
							columns += String.format(colName.equals("id") ? DB_ID : DB_COLUMN, colName, dbType);
						}
					} else if (line.startsWith(ENTITY_START)) {
						entity = line.substring(ENTITY_START.length(), line.indexOf("{")).trim();
						columns = "";
						depth++;
					}
				} catch (Exception e) {
					throw new RuntimeException("Error processing: " + line, e);
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new ProtoToSQLConverter(args[0]).execute();
	}
}
