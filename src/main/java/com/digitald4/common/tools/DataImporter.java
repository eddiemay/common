package com.digitald4.common.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.DAOSQLImpl;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONObject;

public class DataImporter {
	private final DAO dao;
	private final String apiUrl;
	private String idToken;

	public DataImporter(DAO dao, String apiUrl) {
		this.dao = dao;
		this.apiUrl = apiUrl;
	}

	public void login() throws IOException {
		idToken = new JSONObject(
				sendPost(apiUrl + "/users:login",
				"json=%7B%22username%22:%22eddiemay@gmail.com%22,%22password%22:%22vxae11%22%7D"))
				.getString("idToken");
		System.out.println("IdToken: " + idToken);
	}

	public String sendPost(String url, String payload) throws IOException {
		return send("POST", url, payload);
	}

	public String sendGet(String url) throws IOException {
		return send("GET", url, null);
	}

	public String send(String method, String url, String payload) throws IOException {
		long startTime = System.currentTimeMillis();
		if (idToken != null) {
			if (payload != null) {
				payload = "idToken=" + idToken + "&" + payload;
			} else {
				url += "?idToken=" + idToken;
			}
		}
		url = url.replaceAll(" ", "%20");
		System.out.println("\nSending '" + method + "' request to URL: " + url);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (payload != null) {
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			System.out.println("Payload: " + payload);
			dos.writeBytes(payload);
			dos.flush();
			dos.close();
		}

		int responseCode = con.getResponseCode();
		System.out.println("Response Code: " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = in.readLine()) != null) {
			response.append(line);
		}
		in.close();
		System.out.println("Response: " + response);
		System.out.println("Time: " + (System.currentTimeMillis() - startTime) + "ms");
		return response.toString();
	}

	public <T extends GeneratedMessageV3> void runFor(Class<T> c) throws IOException {
		runFor(c, Query.getDefaultInstance());
	}

	public <T extends GeneratedMessageV3> void runFor(Class<T> c, Query query) throws IOException {
		String url = apiUrl + "/" + c.getSimpleName() + "s";
		List<T> results = dao.list(c, query).getResultList();
		if (!results.isEmpty()) {
			T type = results.get(0);
			JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(type.getDescriptorForType()).build();
			Printer jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
			results.stream().parallel().forEach(t -> {
				try {
					sendPost(url, "json={\"proto\": " + jsonPrinter.print(t) + "}");
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
		String url = apiUrl + "/" + c.getSimpleName() + "s";
		String response = sendGet(url);
		ListResponse.Builder builder = ListResponse.newBuilder();
		T type = DAOCloudDS.getDefaultInstance(c);
		JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(type.getDescriptorForType()).build();
		JsonFormat.parser().usingTypeRegistry(registry).merge(response, builder);
		return builder.build();
	}

	public static void main(String[] args) throws IOException {
		DataImporter dataImporter = new DataImporter(
				new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
						"jdbc:mysql://localhost/iisosnet_main?autoReconnect=true",
						"dd4_user", "getSchooled85")),
				"https://ip360-179401.appspot.com/api"
				// "http://localhost:8181/api"
		);
		dataImporter.login();
		// dataImporter.runFor(GeneralData.class);
		dataImporter.export(GeneralData.class)
				.getResultList()
				.forEach(any -> System.out.println(any.unpack(GeneralData.class)));
	}
}
