package com.digitald4.common.tools;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.storage.DAO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by eddiemay on 8/21/16.
 */
public class GeneralDataJSCompiler {
	private static final String DECLARATION = "com.digitald4.%s.GeneralData = {\n";
	private final String project;
	private final DAO<GeneralData> dao;
	private final String outputFile;

	public GeneralDataJSCompiler(String project, DAO<GeneralData> dao, String outputFile) {
		this.project = project;
		this.dao = dao;
		this.outputFile = outputFile;
	}

	public void compile() throws DD4StorageException {
		Map<Integer, List<GeneralData>> hash = new HashMap<>();
		List<GeneralData> generalDatas = dao.getAll();
		for (GeneralData generalData : generalDatas) {
			List<GeneralData> children = hash.get(generalData.getGroupId());
			if (children == null) {
				children = new ArrayList<>();
				hash.put(generalData.getGroupId(), children);
			}
			children.add(generalData);
		}
		StringBuilder output = new StringBuilder(String.format(DECLARATION, project));
		for (GeneralData generalData : hash.get(0)) {
			output.append("\t" + generalData.getName().replaceAll(" ", "_").toUpperCase() + ": "
					+ generalData.getId() + ",\n");
		}
		output.append("};\n");
		System.out.println(output);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));) {
			writer.write(output.toString());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
