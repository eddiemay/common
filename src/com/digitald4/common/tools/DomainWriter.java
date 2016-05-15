package com.digitald4.common.tools;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.digitald4.common.jdbc.PDBConnection;
import com.digitald4.common.log.EspLogger;
import com.digitald4.common.util.FormatText;

public class DomainWriter {
	public final static String COMMA = ", ";
	public final static String FETCH_EXCEPTION_CLASS = " ";
	public final static String EXCEPTION_CLASS = " throws Exception ";
	private TreeSet<String> imports = new TreeSet<String>();
	private TreeSet<Property> properties = new TreeSet<Property>();
	private KeyConstraint idKey = new KeyConstraint(this, "pk", "pk", "pk", "pk", "pk", KeyConstraint.ID);
	private TreeSet<KeyConstraint> children = new TreeSet<KeyConstraint>();
	private TreeSet<KeyConstraint> parents = new TreeSet<KeyConstraint>();
	private ArrayList<String> namedQueries = new ArrayList<String>();
	private String name;
	private final String project;
	private String table;
	private UMLClass umlClass;

	public DomainWriter(String project, String name, String table) {
		this.project = project;
		this.name = name;
		setTable(table);
	}
	
	public DomainWriter(String project, UMLClass umlClass) {
		this.umlClass = umlClass;
		this.project = project;
		genFromUMLClass();
	}
	
	public String getProject() {
		return project;
	}
	
	public String getName() {
		if(umlClass!=null)
			return umlClass.getName().replaceAll(" ", "_");
		return name;
	}
	
	public String getTable() {
		if(umlClass!=null)
			return umlClass.getDBTable();
		return table;
	}
	
	public void setTable(String table) {
		this.table = table;
	}
	
	public TreeSet<String> getImports() {
		return imports;
	}
	
	public void addImport(String jImport) {
		getImports().add(jImport);
		if (jImport.contains("java.util.Date")) {
			getImports().add("com.digitald4.common.util.FormatText");
		}
	}
	
	public TreeSet<Property> getProperties() {
		return properties;
	}
	
	public void addProperty(Property prop) {
		Class<?> c = prop.getJavaClass();
		if (c.getName().contains(".") && !c.getName().startsWith("java.lang.")) {
			addImport(c.getName());
		}
		getProperties().add(prop);
	}
	
	public void setIdKey(KeyConstraint idKey) {
		this.idKey = idKey;
	}
	
	public KeyConstraint getIdKey() {
		return idKey;
	}
	
	public TreeSet<KeyConstraint> getChildren() {
		return children;
	}
	
	public void addChild(KeyConstraint child) {
		for (KeyConstraint p:getChildren()) {
			if (p.getName().equals(child.getName()) && p.getReference() == child.getReference()) {
				child.incrementReference();
			}
		}
		getChildren().add(child);
		if (child.isIndexed()) {
			addImport("com.digitald4.common.util.SortedList");
			addImport(child.getJavaPackageHeader() + ".model." + child.getJavaRefClass());
		}
	}
	
	public TreeSet<KeyConstraint> getParents() {
		return parents;
	}
	
	public void addParent(KeyConstraint parent) {
		//EspLogger.message(this, "Parent: "+parent);
		for (KeyConstraint p : getParents()) {
			if (p.getName().equals(parent.getName()) && p.getReference() == parent.getReference()) {
				parent.incrementReference();
			}
		}
		getParents().add(parent);
		if (parent.isIndexed()) {
			addNamedQueryEntry(parent.getJavaNamedQuery());
		}
		addImport(parent.getJavaPackageHeader() + ".model." + parent.getJavaRefClass());
	}

	public void addStandardImports() {
		addImport(getJavaModelPackage()+"."+getJavaName());
		addImport("java.util.List");
		addImport("java.util.Hashtable");
		addImport("java.util.Map");
		addImport("java.util.Vector");
		addImport("com.digitald4.common.jpa.PrimaryKey");
		addImport("com.digitald4.common.dao.DataAccessObject");
		addImport("javax.persistence.EntityManager");
		addImport("javax.persistence.Column");
		addImport("javax.persistence.Id");
		if (!getJavaSuperClass().equals("DataAccessObject")){
			String sc = getJavaSuperClass();
			if (sc.contains("<")) {
				sc = sc.substring(0, sc.indexOf('<'));
			}
			addImport(getJavaModelPackage()+"."+sc);
		}
	}

	public void genFromUMLClass() {
		addStandardImports();
		int index=0;
		int pkIndex=0;
		for(UMLAttribute att:umlClass.getAttributes()){
			Property prop = new Property(this,index++,att.getName().toUpperCase().replaceAll(" ", "_"),att.getType(),att.getSize(),att.isNullable(),att.getDefault());
			if(att.isId())
				idKey.addProperty(new FKProperty(prop,pkIndex++));
			prop.setGenerated(att.isGenerated());
			if(prop.isGenerated())
				addImport("javax.persistence.GeneratedValue");
			prop.setHasSequence(att.getSequence()!=null);
			if(prop.hasSequence())
				addImport("javax.persistence.SequenceGenerator");
			addProperty(prop);
		}

		addNamedQueryEntry("@NamedQuery(name = \"findByID\", query=\"SELECT o FROM "+getJavaName()+" o WHERE "+getIdKey().getJPQLEntry()+"\"),");
		addNamedQueryEntry("@NamedQuery(name = \"findAll\", query=\"SELECT o FROM "+getJavaName()+" o\"),");
		addNamedQueryEntry("@NamedQuery(name = \"findAllActive\", query=\"SELECT o FROM "+getJavaName()+" o\"),");

		for (UMLReference ref : umlClass.getParentReferences()) {
			if (!ref.isStandard()) {
				KeyConstraint fK = new KeyConstraint(this,ref.getPackage(), ref.getName(), ref.getRefName(),
						ref.getRefClass(), ref.getDBName(), KeyConstraint.PARENT, ref.isIndexed());
				int i=0;
				for (UMLConnector conn:ref.getConnectors()) {
					for (Property prop:getProperties()) {
						if (!prop.isGloballyHandled() && prop.getName().equals(conn.getAttrDBName())) {
							fK.addProperty(new FKProperty(prop,i++,conn.getRefAttr()));
							break;
						}
					}
				}
				addParent(fK);
			}
		}
		for (UMLReference ref : umlClass.getChildReferences()) {
			KeyConstraint fK = new KeyConstraint(this, ref.getPackage(), ref.getRefName(), ref.getName(),
					ref.getUmlClass().getName(), ref.getDBName(), KeyConstraint.CHILD, ref.isIndexed());
			int i=0;
			for (UMLConnector conn:ref.getConnectors()) {
				for (Property prop:getProperties()) {
					if (!prop.isGloballyHandled() && prop.getName().equals(conn.getRefAttrDBName())) {
						fK.addProperty(new FKProperty(prop,i++,conn.getRefAttr()));
						break;
					}
				}
			}
			addChild(fK);
		}
	}

	public void genFromOracleDB(Connection conn, String schema, String table) throws SQLException {
		addStandardImports();
		DatabaseMetaData dbmd = conn.getMetaData();
		PreparedStatement ps = conn.prepareStatement("SELECT COMMENTS REMARKS FROM user_col_comments WHERE table_name=? AND column_name=?");
		ResultSet rs = dbmd.getColumns(null,schema,table,null);
		int index=0;
		while (rs.next()) {
			String colName = rs.getString("COLUMN_NAME");
			FieldType type = FieldType.getColumnTypeFromDB(colName,rs.getInt("DATA_TYPE"),rs.getInt("COLUMN_SIZE"),rs.getInt("DECIMAL_DIGITS"));
			if (type != FieldType.BLOB) {
				String def = rs.getString("COLUMN_DEF");
				if (def != null) {
					if (def.contains("'")) {
						def = def.replaceAll("'", "");
					}
					def = def.trim();
				}
				Property prop = new Property(this,index++,rs.getString("COLUMN_NAME"),type,rs.getString("COLUMN_SIZE"),rs.getInt("NULLABLE")!=ResultSetMetaData.columnNoNulls,def);
				ps.setString(1, table);
				ps.setString(2, colName);
				ResultSet rsC = ps.executeQuery();
				if (rsC.next()) {
					String comment = rsC.getString("REMARKS");
					prop.setComment(comment);
					if (comment != null) {
						prop.setAbandoned(comment.toUpperCase().contains("ABANDONED"));
						prop.setDeprecated(comment.toUpperCase().contains("DEPRECATED"));
						prop.setHasSequence(comment.toUpperCase().contains("AUTO_INCREMENT"));
						if(prop.hasSequence())
							addImport("javax.persistence.SequenceGenerator");
					}
				}
				rsC.close();
				if (!prop.isAbandoned()) {
					addProperty(prop);
				}
			}
		}
		rs.close();
		ps.close();
		rs = dbmd.getPrimaryKeys(null,schema,table);
		while (rs.next()) {
			String colName = rs.getString("COLUMN_NAME");
			for (Property prop:getProperties()) {
				if (prop.getName().equals(colName)) {
					idKey.addProperty(new FKProperty(prop,rs.getInt("KEY_SEQ")));
					break;
				}
			}
		}
		rs.close();

		addNamedQueryEntry("@NamedQuery(name = \"findByID\", query=\"SELECT o FROM " + getJavaName()
				+ " o WHERE " + getIdKey().getJPQLEntry() + "\"),");
		addNamedQueryEntry("@NamedQuery(name = \"findAll\", query=\"SELECT o FROM "+ getJavaName()
				+ " o\"),");
		addNamedQueryEntry("@NamedQuery(name = \"findAllActive\", query=\"SELECT o FROM "
				+ getJavaName() + " o\"),");

		Hashtable<String,KeyConstraint> pFKHash = new Hashtable<String,KeyConstraint>();
		rs = dbmd.getCrossReference(null, schema, null, null, schema, table);
		while (rs.next()) {
			KeyConstraint fK = pFKHash.get(rs.getString("FK_NAME"));
			if (fK == null) {
				String pkTable = rs.getString("PKTABLE_NAME");
				String name = pkTable.substring(pkTable.indexOf('_') + 1);
				fK = new KeyConstraint(this, project, name, name, name, rs.getString("FK_NAME"),
						KeyConstraint.PARENT, rs.getString("FK_NAME").endsWith("I"));
				pFKHash.put(rs.getString("FK_NAME"),fK);
			}
			String colName = rs.getString("FKCOLUMN_NAME");
			for (Property prop:getProperties()) {
				if (!prop.isGloballyHandled() && prop.getName().equals(colName)) {
					fK.addProperty(new FKProperty(prop,rs.getInt("KEY_SEQ"),rs.getString("PKCOLUMN_NAME")));
					break;
				}
			}
		}
		rs.close();
		for (KeyConstraint fK:new TreeSet<KeyConstraint>(pFKHash.values())) {
			if (fK.getProperties().size() > 0) {
				addParent(fK);
			}
		}

		Hashtable<String,KeyConstraint> cFKHash = new Hashtable<String,KeyConstraint>();
		rs = dbmd.getCrossReference(null, schema, table, null, schema, null);
		while (rs.next()) {
			if (isGoodTable(rs.getString("FKTABLE_NAME"))){
				KeyConstraint fK = cFKHash.get(rs.getString("FK_NAME"));
				if (fK == null) {
					String fkTable = rs.getString("FKTABLE_NAME");
					String name = fkTable.substring(fkTable.indexOf('_')+1);
					fK = new KeyConstraint(this, project, name, name, name, rs.getString("FK_NAME"),
							KeyConstraint.CHILD, rs.getString("FK_NAME").endsWith("I"));
					cFKHash.put(rs.getString("FK_NAME"), fK);
				}
				String colName = rs.getString("PKCOLUMN_NAME");
				for (Property prop:getProperties()) {
					if (prop.getName().equals(colName)) {
						fK.addProperty(new FKProperty(prop,rs.getInt("KEY_SEQ"),rs.getString("FKCOLUMN_NAME")));
						break;
					}
				}
			}
		}
		rs.close();
		for (KeyConstraint fK:new TreeSet<KeyConstraint>(cFKHash.values())) {
			if (fK.getProperties().size() > 0) {
				addChild(fK);
			}
		}
	}

	//Java Generating stuff
	public String getJavaDomain() {
		String out = "";
		//out += getJavaGenInfo(); 
		out += "package "+getJavaDAOPackage() + ";\n\n";
		out += getJavaImports() + "\n";
		out += getJavaCopyRight();
		out += getJavaDescription();
		out += getJavaClassDeclaration();
		out += getJavaStaticFields();
		//out += getJavaFieldLimits();
		out += getJavaFields();
		//out += getJavaStaticMethods();
		out += getJavaConstructors();
		out += getJavaBasicMethods();
		out += getJavaPropertyMethods();
		out += getJavaParentMethods();
		out += getJavaChildMethods();
		out += getJavaRawPropertyMethods();
		out += getJavaCopyMethods();
		out += getJavaDifference();
		out += getJavaInsertMethods();
		return out+"}\n";
	}
	
	public String getJavaGenInfo() {
		return "/**\n * AUTO GENERATED ON: " + FormatText.MYSQL_DATETIME.format(
				Calendar.getInstance().getTime()) + " " + System.currentTimeMillis() + "\n" + "*/\n";
	}
	
	public String getJavaPackageHeader() {
		return "com.digitald4." + project;
	}
	
	public String getJavaDomainPackage() {
		return getJavaPackageHeader() + ".domain";
	}
	
	public String getJavaDAOPackage() {
		return getJavaPackageHeader() + ".dao";
	}
	
	public String getJavaModelPackage() {
		return getJavaPackageHeader() + ".model";
	}
	
	public String getJavaCopyRight() {
		return "/** TODO Copy Right*/\n";
	}
	
	public String getJavaDescription() {
		return "/**Description of class, (we need to get this from somewhere, database? xml?)*/\n";
	}
	
	public String getJavaImports() {
		String out = "";
		for (String i : getImports()) {
			out += "import " + i + ";\n";
		}
		return out;
	}
	
	public String getJavaName() {
		return FormatText.toUpperCamel(getName());
	}
	
	public String getJavaVarName() {
		return FormatText.toLowerCamel(getName());
	}
	
	public String getJavaDomainName() {
		return getJavaName() + "DAO";
	}
	
	public String getJavaSuperClass() {
		if (umlClass != null) {
			if (umlClass.getSuperClass() == null) {
				return "DataAccessObject";
			}
			return FormatText.toUpperCamel(umlClass.getSuperClass());
		}
		return "DataAccessObject";
	}
	
	public ArrayList<String> getNamedQueryEntries() {
		return namedQueries;
	}
	
	public void addNamedQueryEntry(String namedQuery) {
		namedQueries.add(namedQuery);
	}
	
	public ArrayList<String> getNamedNativeQueryEntries() {
		ArrayList<String> entries = new ArrayList<String>();
		entries.add("@NamedNativeQuery(name = \"refresh\", query=\"SELECT o.* FROM " + getTable()
				+ " o WHERE " + getIdKey().getSQLEntry() + "\"),");
		return entries;
	}
	
	public String getJavaClassDeclaration() {
		return "public abstract class " + getJavaDomainName() + " extends " + getJavaSuperClass() + "{\n";
	}
	
	public String getJavaHashtableType() {
		return "Hashtable<String, TreeSet<" + getJavaName() + ">>";
	}
	
	public String getJavaStaticFields() {
		String out = "\tpublic enum KEY_PROPERTY{" + getIdKey().getPropNames() + "};\n";
		out += "\tpublic enum PROPERTY{";
		boolean first = true;
		for (Property prop:getProperties()) {
			if (!first) {
				out += ",";
			}
			out += prop.getName();
			first = false;
		}
		out += "};\n";
		return out;
	}
	
	public String getJavaFieldLimits() {
		String out = "";
		for (Property prop:getProperties()) {
			if (!prop.isGloballyHandled()) {
				out += prop.getJavaLimitEntry()+"\n";
			}
		}
		return out;
	}
	
	public String getJavaFields() {
		String out = "";
		for (Property prop : getProperties()) {
			if (!prop.isGloballyHandled()) {
				out += prop.getJavaFieldEntry() + "\n";
			}
		}
		for (KeyConstraint key : getChildren()) {
			if (key.isIndexed()) {
				out += key.getJavaFieldEntry() + "\n";
			}
		}
		for (KeyConstraint key : getParents()) {
			//if(key.isIndexed())
			out += key.getJavaFieldEntry()+"\n";
		}
		return out;
	}
	
	public String getJavaCollection() {
		return "List<" + getJavaName() + ">";
	}
	
	public String getJavaConstructors(){
		String out = "\tpublic "+getJavaDomainName()+"(EntityManager entityManager) {\n"
				+ "\t\tsuper(entityManager);\n"
				+ "\t}\n";
		out += "\tpublic "+getJavaDomainName()+"(EntityManager entityManager, "+getIdKey().getJavaParameterList()+") {\n"
				+ "\t\tsuper(entityManager);\n";
		for (FKProperty prop:getIdKey().getProperties())
			out+="\t\tthis."+prop.getJavaName()+"="+prop.getJavaName()+";\n";
		out+="\t}\n";
		out+="\tpublic "+getJavaDomainName()+"(EntityManager entityManager, " + getJavaDomainName() + " orig) {\n";
		out+="\t\tsuper(entityManager, orig);\n";
		for (FKProperty prop:getIdKey().getProperties()) {
			if (!prop.getProp().hasSequence() && !prop.getProp().isGenerated()) {
				out+="\t\tthis."+prop.getProp().getJavaName()+"=orig."+prop.getProp().getJavaGetMethod()+";\n";
			}
		}
		out+="\t\tcopyFrom(orig);\n";
		out+="\t}\n";
		out+="\tpublic void copyFrom("+getJavaDomainName()+" orig){\n";
		for (Property prop : getProperties()) {
			if (!prop.isGloballyHandled() && !getIdKey().contains(prop)) {
				out += "\t\tthis." + prop.getJavaName() + " = orig." + prop.getJavaGetMethod() + ";\n";
			}
		}
		out+="\t}\n";
		return out;
	}
	public String getJavaBasicMethods(){
		String out = "";
		out += "\t@Override\n"
				+ "\tpublic String getHashKey() {\n"
				+ "\t\treturn getHashKey(getKeyValues());\n"
				+ "\t}\n";
		out += "\tpublic Object[] getKeyValues() {\n"
				+ "\t\treturn new Object[]{" + getIdKey().getJavaParameterVars() + "};\n"
				+ "\t}\n";
		out += "\t@Override\n"
				+ "\tpublic int hashCode() {\n"
				+ "\t\treturn PrimaryKey.hashCode(getKeyValues());\n"
				+ "\t}\n";
		return out;
	}
	public String getJavaPropertyMethods(){
		String out = "";
		for(Property prop:getProperties()){
			if(getIdKey().contains(prop))
				out+="\t@Id\n";
			if(!prop.isGloballyHandled()){
				if(prop.isGenerated()){
					out+="\t@GeneratedValue\n";
				}
				if(prop.hasSequence()){
					String seq = getTable().substring(0, getTable().indexOf('_')+1)+"SEQ";
					out+="\t@SequenceGenerator(name=\""+seq+"\",sequenceName=\""+seq+"\")\n";
				}
				out += prop.getJavaGetMethodEntry();
				out += prop.getJavaSetMethodEntry();
			}
		}
		return out;
	}
	public String getJavaParentMethods(){
		String out = "";
		for(KeyConstraint key:getParents()){
			out += key.getJavaGetMethodEntry();
			out += key.getJavaSetMethodEntry();
		}
		return out;
	}
	public String getJavaChildMethods(){
		String out = "";
		for(KeyConstraint key:getChildren()){
			if(key.isIndexed()){
				out += key.getJavaGetMethodEntry();
				out += key.getJavaAddMethodEntry();
				out += key.getJavaRemoveMethodEntry();
			}
		}
		return out;
	}
	public String getJavaRawPropertyMethods(){
		String out = "";
		out = "\tpublic Map<String,Object> getPropertyValues() {\n"
				+ "\t\tHashtable<String,Object> values = new Hashtable<String,Object>();\n"
				+ "\t\tfor(PROPERTY prop:PROPERTY.values()) {\n"
				+ "\t\t\tObject value = getPropertyValue(prop);\n"
				+ "\t\t\tif(value!=null)\n"
				+ "\t\t\t\tvalues.put(\"\"+prop,value);\n"
				+ "\t\t}\n"
				+ "\t\treturn values;\n"
				+ "\t}\n\n"
				+ "\tpublic " + getJavaName() + " setPropertyValues(Map<String,Object> data)"+EXCEPTION_CLASS+" {\n"
				+ "\t\tfor(String key:data.keySet())\n"
				+ "\t\t\tsetPropertyValue(key, data.get(key).toString());\n"
				+ "\t\treturn (" + getJavaName() + ")this;\n"
				+ "\t}\n\n";
		out += "\t@Override\n"
				+ "\tpublic Object getPropertyValue(String property) {\n"
				+ "\t\treturn getPropertyValue(PROPERTY.valueOf(formatProperty(property)));\n"
				+ "\t}\n";
		out += "\tpublic Object getPropertyValue(PROPERTY property) {\n"
				+ "\t\tswitch (property) {\n";
		for (Property prop:getProperties()) {
			out += prop.getJavaGetPVEntry();
		}
		out += "\t\t}\n"
				+ "\t\treturn null;\n"
				+ "\t}\n\n";
		out += "\t@Override\n" 
				+ "\tpublic " + getJavaName() + " setPropertyValue(String property, String value)"+EXCEPTION_CLASS+" {\n"
				+ "\t\tif(property == null) return (" + getJavaName() + ")this;\n"
				+ "\t\treturn setPropertyValue(PROPERTY.valueOf(formatProperty(property)),value);\n"
				+ "\t}\n\n";
		out += "\tpublic " + getJavaName() + " setPropertyValue(PROPERTY property, String value)"+EXCEPTION_CLASS+" {\n"
				+ "\t\tswitch (property) {\n";
		for (Property prop:getProperties()) {
			out += prop.getJavaSetPVEntry();
		}
		out += "\t\t}\n"
				+ "\t\treturn (" + getJavaName() + ")this;\n"
				+ "\t}\n\n";
		return out;
	}
	public String getJavaCopyMethods(){
		String copy="\tpublic "+getJavaName()+" copy()"+EXCEPTION_CLASS+"{\n"
				+"\t\t"+getJavaName()+" cp = new "+getJavaName()+"(getEntityManager(), ("+getJavaName()+")this);\n"
				+"\t\tcopyChildrenTo(cp);\n"
				+"\t\treturn cp;\n"
				+"\t}\n"
				+"\tpublic void copyChildrenTo("+getJavaDomainName()+" cp)"+EXCEPTION_CLASS+"{\n"
				+"\t\tsuper.copyChildrenTo(cp);\n";
		for(KeyConstraint key:getChildren())
			if(key.isIndexed())
				copy += key.getJavaCopyEntry();
		copy +="\t}\n";
		return copy;
	}
	public String getJavaDifference() {
		String diff="\tpublic Vector<String> getDifference("+getJavaDomainName()+" o){\n"
				+"\t\tVector<String> diffs = super.getDifference(o);\n";
		for(Property prop:getProperties())
			if(!prop.isGloballyHandled())
				diff += prop.getJavaDiffEntry();
		diff+="\t\treturn diffs;\n"
				+"\t}\n";
		return diff;
	}
	public String getJavaInsertMethods() {
		String out = "";
		out += "\t@Override\n"
				+ "\tpublic void insertParents()"+EXCEPTION_CLASS+"{\n";
		for(KeyConstraint parent:getParents())
			if(parent.isIndexed())
				out+=parent.getJavaParentInsertEntry();
		out += "\t}\n";
		out += "\t@Override\n"
				+ "\tpublic void insertPreCheck()"+EXCEPTION_CLASS+"{\n";
		for (Property prop : getProperties()) {
			if (!prop.isNullable() && !prop.isGenerated()) {
				out += "\t\tif (isNull(" + prop.getJavaGetMethod() + "))\n"
						+ "\t\t\t throw new Exception(\""+prop.getName()+" is required.\");\n";
			}
		}
		out+="\t}\n";
		out += "\t@Override\n"
		+ "\tpublic void insertChildren()"+EXCEPTION_CLASS+"{\n";
		for(KeyConstraint child:getChildren())
			if(child.isIndexed())
				out+=child.getJavaChildInsertSetEntry();
		for(KeyConstraint child:getChildren())
			if(child.isIndexed())
				out+=child.getJavaChildInsertEntry();
		out+="\t}\n";
		return out;
	}
	public static boolean isGoodTable(String table){
		return (!table.contains("MDIR") && !table.startsWith("MDI005_") && !table.contains("REPORT") && !table.contains("SCE_UNIT_COST") && !table.equals("MDI215_PROJ_REV") && !table.endsWith("PROJ_CO") && !table.contains("XFMR_SP"));
	}
	public static void runTables(String pattern, String project)throws Exception{
		pattern = pattern.toUpperCase();
		TreeSet<String> tables = new TreeSet<String>();
		DatabaseMetaData dbmd = PDBConnection.getInstance().getConnection().getMetaData();
		ResultSet rs = dbmd.getTables(null,null,pattern,new String[]{"TABLE"});
		while(rs.next()){
			String table = rs.getString("TABLE_NAME");
			EspLogger.message(DomainWriter.class,"Table found: "+table);
			if(isGoodTable(table))
				tables.add(table);
		}

		EspLogger.message(DomainWriter.class,"Running the following tables in 5 secs:");
		for (String table : tables) {
			EspLogger.message(DomainWriter.class, "\t" + table.substring(table.indexOf('_') + 1) + "\t" + table);
		}
		//		Thread.sleep(5000);
		for (String table : tables){
			String className = table.substring(table.indexOf('_') + 1);
			EspLogger.message(DomainWriter.class,className);
			DomainWriter dao = new DomainWriter(project, className, table);
			dao.genFromOracleDB(PDBConnection.getInstance().getConnection(), "MDI", table);
			String code = dao.getJavaDomain();
			writeFile("src/" + dao.getJavaDAOPackage().replace('.', '/') + "/", dao.getJavaDomainName() + ".java", code);
			try{
				updateBizObj(dao);
			}
			catch(FileNotFoundException fnfe){
				createBizObj(dao);
			}
		}
	}
	public static void runUMLClasses(String pattern, String project)throws Exception{
		pattern = pattern.toUpperCase();
		ArrayList<UMLClass> classes = new ArrayList<UMLClass>();
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File("../"+project+"/src/conf/Schema.xml");
		Document document = builder.build(xmlFile);
		Element rootNode = document.getRootElement();
		for (Object o:rootNode.getChildren("CLASS")){
			UMLClass umlClass = new UMLClass(project, (Element) o);
			//EspLogger.message(DomainWriter.class,"Class: "+umlClass.getName());
			if(umlClass.getDBTable().toUpperCase().contains(pattern)){
				EspLogger.message(DomainWriter.class,"Class found: "+umlClass);
				classes.add(umlClass);
			}
		}

		EspLogger.message(DomainWriter.class, "Running the following classes in 5 secs:");
		for(UMLClass umlClass:classes)
			EspLogger.message(DomainWriter.class, "\t" + umlClass);
		//		Thread.sleep(5000);
		for (UMLClass umlClass : classes) {
			EspLogger.message(DomainWriter.class,umlClass);
			DomainWriter dao = new DomainWriter(project, umlClass);
			String code = dao.getJavaDomain();
			writeFile("../"+project+"/src/"+dao.getJavaDAOPackage().replace('.', '/')+"/",dao.getJavaDomainName()+".java",code);
			try{
				updateBizObj(dao);
			}
			catch(FileNotFoundException fnfe){
				createBizObj(dao);
			}
		}
	}
	public static void createBizObj(DomainWriter dao) throws Exception{
		StringBuffer sb = new StringBuffer();
		String basePath = "../"+dao.getProject()+"/src/"+dao.getJavaModelPackage().replace('.', '/');
		String fileName = dao.getJavaName()+".java";
		sb.append("package "+dao.getJavaModelPackage()+";\n");
		sb.append("import "+dao.getJavaDAOPackage()+"."+dao.getJavaDomainName()+";\n");
		sb.append("import javax.persistence.Entity;\n");
		sb.append("import javax.persistence.NamedNativeQueries;\n");
		sb.append("import javax.persistence.NamedNativeQuery;\n");
		sb.append("import javax.persistence.NamedQueries;\n");
		sb.append("import javax.persistence.NamedQuery;\n");
		sb.append("import javax.persistence.Table;\n");
		String ta = "@Table(name=\""+dao.getTable()+"\")\n";
		sb.append("@Entity\n");
		sb.append(ta);
		sb.append("@NamedQueries({\n");
		for(String nqe:dao.getNamedQueryEntries())
			sb.append("\t"+nqe+"//AUTO-GENERATED\n");
		sb.append("})\n");
		sb.append("@NamedNativeQueries({\n");
		for(String nnqe:dao.getNamedNativeQueryEntries())
			sb.append("\t"+nnqe+"//AUTO-GENERATED\n");
		sb.append("})\n");
		sb.append("public class "+dao.getJavaName()+" extends "+dao.getJavaDomainName()+"{\n");
		sb.append("\tpublic "+dao.getJavaName()+"(){\n\t}\n");
		sb.append("\tpublic "+dao.getJavaName()+"("+dao.getIdKey().getJavaParameterList()+"){\n");
		sb.append("\t\tsuper("+dao.getIdKey().getJavaParameterVars()+");\n");
		sb.append("\t}\n");
		sb.append("\tpublic "+dao.getJavaName()+"("+dao.getJavaName()+" orig){\n");
		sb.append("\t\tsuper(orig);\n\t}\n");
		sb.append("}\n");
		//EspLogger.message(DomainWriter.class, ""+sb);
		writeFile(basePath+"/",fileName,sb.toString());
	}
	public static void updateBizObj(DomainWriter dao) throws Exception{
		StringBuffer sb = new StringBuffer();
		String basePath = "../"+dao.getProject()+"/src/"+dao.getJavaModelPackage().replace('.', '/');
		String fileName = dao.getJavaName()+".java";
		BufferedReader br = new BufferedReader(new FileReader(basePath+"/"+fileName));
		String line = br.readLine();
		boolean eImport=false,tImport=false,nqImport=false,nnqImport=false;
		boolean entitySet=false,tableSet=false;
		boolean nq=false,nnq=false;
		String ta = "@Table(schema=\""+dao.getProject()+"\",name=\""+dao.getTable()+"\")\n";
		while(line != null){
			if(line.contains("@Entity"))
				entitySet=true;
			else if(line.contains("import javax.persistence.Entity;"))
				eImport=true;
			else if(line.contains("import javax.persistence.Table;"))
				tImport=true;
			else if(line.contains("import javax.persistence.NamedQuery;"))
				nqImport=true;
			else if(line.contains("import javax.persistence.NamedNativeQuery;"))
				nnqImport=true;
			else if(line.contains("@NamedQueries"))
				nq=true;
			else if(line.contains("@NamedNativeQueries"))
				nnq=true;
			else if(line.contains(dao.getJavaName()) && line.contains("class") && line.contains("public")){
				if(!eImport)
					sb.append("import javax.persistence.Entity;\n");
				if(!tImport)
					sb.append("import javax.persistence.Table;\n");
				if(!nqImport){
					sb.append("import javax.persistence.NamedQueries;\n");
					sb.append("import javax.persistence.NamedQuery;\n");
				}
				if(!nnqImport){
					sb.append("import javax.persistence.NamedNativeQueries;\n");
					sb.append("import javax.persistence.NamedNativeQuery;\n");
				}
				if(!entitySet)
					sb.append("@Entity\n");
				if(!tableSet)
					sb.append(ta);
				if(!nq){
					sb.append("@NamedQueries({\n");
					for(String nqe:dao.getNamedQueryEntries())
						sb.append("\t"+nqe+"//AUTO-GENERATED\n");
					sb.append("})\n");
				}
				if(!nnq){
					sb.append("@NamedNativeQueries({\n");
					for(String nnqe:dao.getNamedNativeQueryEntries())
						sb.append("\t"+nnqe+"//AUTO-GENERATED\n");
					sb.append("})\n");
				}
			}
			//The following items are overwritten everytime
			if(line.contains("@Table")){
				sb.append(ta);
				tableSet=true;
			}
			else if(line.contains("@NamedQuery") && line.contains("\"findByID\""))
				for(String nqe:dao.getNamedQueryEntries())
					sb.append("\t"+nqe+"//AUTO-GENERATED\n");
			else if(line.contains("@NamedQuery") && line.contains("//AUTO-GENERATED"));
			else if(line.contains("@NamedNativeQuery") && line.contains("\"refresh\""))
				for(String nnqe:dao.getNamedNativeQueryEntries())
					sb.append("\t"+nnqe+"//AUTO-GENERATED\n");
			else if(line.contains("@NamedNativeQuery") && line.contains("//AUTO-GENERATED"));
			else
				sb.append(line+"\n");
			line = br.readLine();
		}
		br.close();
		//EspLogger.message(DomainWriter.class, ""+sb);
		writeFile(basePath+"/",fileName,sb.toString());
	}
	public static void writeFile(String basePath, String className, String code)throws Exception{
		boolean write=true;
		try{
			BufferedReader br = new BufferedReader(new FileReader(basePath+"/"+className));
			String line = br.readLine();
			if(line.startsWith("//KEEP")){
				EspLogger.message(DomainWriter.class,line);
				write=false;
			}
			br.close();
		}catch(FileNotFoundException e){
			EspLogger.message(DomainWriter.class,"File: "+basePath+className+" does not exist generating...");
		}
		if(write){
			try {
				FileOutputStream fos = new FileOutputStream(basePath+className);
				fos.write(code.getBytes());
				fos.close();
				EspLogger.message(DomainWriter.class,className+" created!");
			} catch (IOException ioe) {
				EspLogger.error(DomainWriter.class, "Can not write file, did you create the package?");
				throw ioe;
			}
		}
	}
	public static void main(String[] args){
		init();
		try {
			runUMLClasses(JOptionPane.showInputDialog("Input umlclass pattern"),JOptionPane.showInputDialog("Input Base project"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void init(){
		EspLogger.init(true,EspLogger.LEVEL.MESSAGE);
	}
}
