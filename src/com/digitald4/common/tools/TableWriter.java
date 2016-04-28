package com.digitald4.common.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class TableWriter {
	public static void processImport(String base) throws JDOMException, IOException {
		System.out.println("processing import "+base);
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File("../"+base+"/src/conf/Schema.xml");
		Document document = builder.build(xmlFile);
		Element rootNode = document.getRootElement();
		for (Object o:rootNode.getChildren("CLASS")) {
			new UMLClass((Element)o);
		}
	}
	public static void runUMLClasses(String base, Connection con, String schema, String pattern, PrintStream out)throws Exception{
		pattern = pattern.toUpperCase();
		ArrayList<UMLClass> classes = new ArrayList<UMLClass>();
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File("../"+base+"/src/conf/Schema.xml");
		Document document = builder.build(xmlFile);
		Element rootNode = document.getRootElement();
		for (Object e:rootNode.getChildren("IMPORT")) {
			processImport(((Element)e).getAttributeValue("name"));
		}
		for (Object o:rootNode.getChildren("CLASS")) {
			UMLClass umlClass = new UMLClass((Element)o);
			if(umlClass.getDBTable().toUpperCase().contains(pattern))
				classes.add(umlClass);
		}
		for (UMLClass umlClass:classes) {
			//out.println("--================== "+umlClass+" ====================");
			umlClass.getDBChange(con == null ? null : con.getMetaData(), schema, out, true);
		}
		out.print(UMLClass.getUndo());
	}
	public static void main(String[] args) {
		try {
			String base = JOptionPane.showInputDialog("Input project base");
			Class.forName("org.gjt.mm.mysql.Driver").newInstance();
			String database = base.equals("budget") ? "budget" : "iisosnet_main";
			String server = base.equals("budget") ? "localhost" : "198.38.82.101";
			server = "localhost";
			System.out.println("Connecting to: " + "jdbc:mysql://" + server + "/" + database + "?autoReconnect=true");
			Connection con = java.sql.DriverManager.getConnection("jdbc:mysql://" + server + "/" + database + "?autoReconnect=true", "eddiemay", "");
			//PrintStream ps = new PrintStream(new FileOutputStream("out.sql"));
			runUMLClasses(base, con, base, JOptionPane.showInputDialog("Input umlclass pattern"), System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
