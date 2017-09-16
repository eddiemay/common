package com.digitald4.common.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.storage.DataConnector;
import com.digitald4.common.storage.DataConnectorSQLImpl;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public class DataImporter {
	private final DataConnector dataConnector;
	private final String apiUrl;
	private String idToken;

	public DataImporter(DataConnector dataConnector, String apiUrl) {
		this.dataConnector = dataConnector;
		this.apiUrl = apiUrl;
	}

	public void login() throws Exception {
		idToken = new JSONObject(
				sendPost(apiUrl + "/users:login",
				"json=%7B%22username%22:%22eddiemay@gmail.com%22,%22password%22:%22vxae11%22%7D"))
				.getString("idToken");
		System.out.println("IdToken: " + idToken);
	}

	private String sendPost(String url, String payload) throws Exception {
		HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setDoOutput(true);
		DataOutputStream dos = new DataOutputStream(con.getOutputStream());
		if (idToken != null) {
			payload = "idToken=" + idToken + "&" + payload;
		}
		dos.writeBytes(payload);
		dos.flush();
		dos.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL: " + url);
		System.out.println("Payload: " + payload);
		System.out.println("Response Code: " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = in.readLine()) != null) {
			response.append(line);
		}
		in.close();
		System.out.println("Response: " + response);
		return response.toString();
	}

	public <T extends GeneratedMessageV3> void runFor(Class<T> c) throws Exception {
		runFor(c, ListRequest.getDefaultInstance());
	}

	public <T extends GeneratedMessageV3> void runFor(Class<T> c, ListRequest listRequest) throws Exception {
		String url = apiUrl + "/" + c.getSimpleName() + "s";
		List<T> results = dataConnector.list(c, listRequest).getResultList();
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

	public static void main(String[] args) throws Exception {
		DataImporter dataImporter = new DataImporter(
				new DataConnectorSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
						"jdbc:mysql://localhost/iisosnet_main?autoReconnect=true",
						"dd4_user", "getSchooled85")),
				"https://ip360-179401.appspot.com/api"
		);
		dataImporter.login();
		dataImporter.runFor(GeneralData.class);
	}
}
