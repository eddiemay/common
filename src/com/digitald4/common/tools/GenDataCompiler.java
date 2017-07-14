package com.digitald4.common.tools;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.storage.DAO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenDataCompiler {
	private static final String JAVA_DECLARATION = "package com.digitald4.%s.storage;\n\npublic class GenData {\n";
	private static final String JAVA_ENTRY = "\tpublic static final int %s = %d;\n";
	private static final String JS_DECLARATION = "com.digitald4.%s.GenData = {\n";
	private static final String JS_ENTRY = "\t%s: %d,\n";

	private final String project;
	private final DAO<GeneralData> dao;
	private final String javaFile;
	private final String jsFile;

	public GenDataCompiler(String project, DAO<GeneralData> dao, String javaFile, String jsFile) {
		this.project = project;
		this.dao = dao;
		this.javaFile = javaFile;
		this.jsFile = jsFile;
	}

	public void compile() throws DD4StorageException {
		Map<Integer, List<GeneralData>> hash = new HashMap<>();
		List<GeneralData> generalDatas = dao.list(ListRequest.getDefaultInstance()).getItemsList();
		for (GeneralData generalData : generalDatas) {
			List<GeneralData> children = hash.get(generalData.getGroupId());
			if (children == null) {
				children = new ArrayList<>();
				hash.put(generalData.getGroupId(), children);
			}
			children.add(generalData);
		}

		StringBuilder javaOut = new StringBuilder(String.format(JAVA_DECLARATION, project));
		StringBuilder jsOut = new StringBuilder(String.format(JS_DECLARATION, project));
		for (GeneralData generalData : hash.get(0)) {
			String catName = generalData.getName().replaceAll(" ", "_").toUpperCase();
			javaOut.append(String.format(JAVA_ENTRY, catName, generalData.getId()));
			jsOut.append(String.format(JS_ENTRY, catName, generalData.getId()));
			if (hash.containsKey(generalData.getId())) {
				for (GeneralData child : hash.get(generalData.getId())) {
					String childName = catName + "_" + child.getName().replaceAll(" ", "_").toUpperCase();
					javaOut.append(String.format(JAVA_ENTRY, childName, child.getId()));
					jsOut.append(String.format(JS_ENTRY, childName, child.getId()));
				}
			}
		}
		javaOut.append("}\n");
		System.out.println(javaOut);
		jsOut.append("};\n");
		System.out.println(jsOut);

		try (BufferedWriter javaWriter = new BufferedWriter(new FileWriter(javaFile));
				BufferedWriter jsWriter = new BufferedWriter(new FileWriter(jsFile));) {
			javaWriter.write(javaOut.toString());
			jsWriter.write(jsOut.toString());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
