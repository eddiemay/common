package com.digitald4.common.tools;

import com.digitald4.common.model.GeneralData;

import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.Query;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GenDataCompiler {
	private static final String JAVA_DECLARATION = "package com.digitald4.%s.storage;\n\npublic class GenData {\n";
	private static final String JAVA_ENTRY = "\tpublic static final long %s = %d;\n";
	private static final String JS_DECLARATION = "com.digitald4.%s.GenData = {\n";
	private static final String JS_ENTRY = "\t%s: %d,\n";

	private final GeneralDataStore store;
	private final String project;
	private final String javaFile;
	private final String jsFile;

	public GenDataCompiler(GeneralDataStore store, String project, String javaFile, String jsFile) {
		this.store = store;
		this.project = project;
		this.javaFile = javaFile;
		this.jsFile = jsFile;
	}

	public void compile() {
		Map<Long, List<GeneralData>> hash = store.list(Query.forList()).getItems().stream()
				.collect(Collectors.groupingBy(GeneralData::getGroupId));

		StringBuilder javaOut = new StringBuilder(String.format(JAVA_DECLARATION, project));
		StringBuilder jsOut = new StringBuilder(String.format(JS_DECLARATION, project));
		for (GeneralData generalData : hash.get(0L)) {
			String catName = fixName.apply(generalData.getName());

			javaOut.append(String.format(JAVA_ENTRY, catName, generalData.getId()));
			jsOut.append(String.format(JS_ENTRY, catName, generalData.getId()));
			if (hash.containsKey(generalData.getId())) {
				for (GeneralData child : hash.get(generalData.getId())) {
					String childName = catName + "_" + fixName.apply(child.getName());
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
				BufferedWriter jsWriter = new BufferedWriter(new FileWriter(jsFile))) {
			javaWriter.write(javaOut.toString());
			jsWriter.write(jsOut.toString());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static final Function<String, String> fixName = name -> name
			.replaceAll("[ /]", "_")
			.replaceAll("[,.()]", "")
			.toUpperCase();
}
