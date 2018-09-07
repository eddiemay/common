package com.digitald4.common.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import java.io.IOException;
import java.util.List;

public class DataImporter {
	private final APIConnector apiConnector;
	private final DAO dao;
	private String idToken;

	public DataImporter(APIConnector apiConnector, DAO dao) {
		this.apiConnector = apiConnector;
		this.dao = dao;
	}

	public <T extends GeneratedMessageV3> void runFor(Class<T> c) throws IOException {
		runFor(c, Query.getDefaultInstance());
	}

	public <T extends GeneratedMessageV3> void runFor(Class<T> c, Query query) throws IOException {
		String url = apiConnector.getApiUrl() + "/" + c.getSimpleName() + "s";
		List<T> results = dao.list(c, query).getResults();
		if (!results.isEmpty()) {
			T type = results.get(0);
			JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(type.getDescriptorForType()).build();
			Printer jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
			results.stream().parallel().forEach(t -> {
				try {
					apiConnector.sendPost(url, "json={\"proto\": " + jsonPrinter.print(t) + "}");
				} catch (Exception ioe) {
					ioe.printStackTrace();
				}
			});
		}
	}

	public <T extends GeneratedMessageV3> ListResponse export(Class<T> c) throws IOException {
		return export(c, ListRequest.getDefaultInstance());
	}

	public <T extends GeneratedMessageV3> ListResponse export(Class<T> c, ListRequest request) throws IOException {
		String url = apiConnector.getApiUrl() + "/" + c.getSimpleName() + "s";
		String response = apiConnector.sendGet(url);
		ListResponse.Builder builder = ListResponse.newBuilder();
		T type = ProtoUtil.getDefaultInstance(c);
		JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(type.getDescriptorForType()).build();
		JsonFormat.parser().usingTypeRegistry(registry).merge(response, builder);
		return builder.build();
	}

	public static void main(String[] args) throws IOException {
		DataImporter dataImporter = new DataImporter(
				new APIConnector("\"https://ip360-179401.appspot.com/api\"").login(),
				new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
						"jdbc:mysql://localhost/iisosnet_main?autoReconnect=true",
						"dd4_user", "getSchooled85"))
		);
		// dataImporter.runFor(GeneralData.class);
		dataImporter.export(GeneralData.class)
				.getResultList()
				.forEach(any -> System.out.println(ProtoUtil.unpack(GeneralData.class, any)));
	}
}
