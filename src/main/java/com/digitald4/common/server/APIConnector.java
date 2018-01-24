package com.digitald4.common.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class APIConnector {
	private final String apiUrl;
	private String idToken;

	public APIConnector(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public APIConnector login() throws IOException {
		idToken = new JSONObject(
				sendPost(getApiUrl() + "/users:login",
						"json=%7B%22username%22:%22eddiemay@gmail.com%22,%22password%22:%22vxae11%22%7D"))
				.getString("idToken");
		System.out.println("IdToken: " + idToken);
		return this;
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
		if (payload != null && ("GET".equals(method) || "DELETE".equals(method))) {
			url = url + "?" + payload;
			payload = null;
		}
		url = url.replaceAll(" ", "%20");
		System.out.println("\nSending '" + method + "' request to URL: " + url);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Cache-Control", "max-age=0");
		con.setRequestProperty("Connection", "keep-alive");
		con.setRequestProperty("Host", "stats.nba.com");
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");
		if (payload != null) {
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			// System.out.println("Payload: " + payload);
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
		// System.out.println("Response: " + response);
		System.out.println("Time: " + (System.currentTimeMillis() - startTime) + "ms");
		return response.toString();
	}
}
