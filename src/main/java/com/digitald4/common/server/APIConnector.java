package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.LoginRequest;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class APIConnector {
	private final String apiUrl;
	private final String apiVersion;
	private String idToken;
	private final long callInterval;
	private long lastCall;

	public APIConnector(String apiUrl, String apiVersion, long callInterval) {
		this.apiUrl = apiUrl;
		this.apiVersion = apiVersion;
		this.callInterval = callInterval;
	}

	public APIConnector(String apiUrl, String apiVersion) {
		this(apiUrl, apiVersion, 0);
	}

	public String formatUrl(String resourceName) {
		String url = apiUrl + "/" + resourceName;
		if (apiVersion != null && !apiVersion.isEmpty()) {
			url += "/" + apiVersion;
		}

		return url;
	}

	public APIConnector setIdToken(String idToken) {
		this.idToken = idToken;
		return this;
	}

	public APIConnector login() {
		JSONObject loginRequest = new JSONObject(
				new LoginRequest().setUsername("eddiemay").setPassword("6B7DE1B846CC2A047CE71E1214C3B6F7"));
		return setIdToken(
				new JSONObject(sendPost(apiUrl + "/users/v1/login", "" + loginRequest)).getString("id"));
	}

	public String sendPost(String url, String payload) {
		return send("POST", url, payload);
	}

	public String sendGet(String url) {
		return send("GET", url, null);
	}

	public String send(String method, String url, String payload) {
		try {
			long waitTime = (lastCall + callInterval) - System.currentTimeMillis();
			if (waitTime > 0) {
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (idToken != null) {
				// if (payload != null) {
					// payload = "idToken=" + idToken + "&" + payload;
				if (url.contains("?")) {
					url += "&idToken=" + idToken;
				} else {
					url += "?idToken=" + idToken;
				}
			}
			if (payload != null && ("GET".equals(method) || "DELETE".equals(method))) {
				url = url + "?" + payload;
				payload = null;
			}
			url = url.replaceAll(" ", "%20");
			// System.out.println("Sending request: " + url + " with payload: " + payload);

			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod(method);
			con.setRequestProperty("Accept", "*/*");
			// con.setRequestProperty("Accept-Encoding", "zip, deflate, br");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
			con.setRequestProperty("Access-Control-Request-Headers", "x-nba-stats-origin,x-nba-stats-token");
			con.setRequestProperty("Access-Control-Request-Method", "GET");
			con.setRequestProperty("Cache-Control", "max-age=0");
			con.setRequestProperty("Connection", "keep-alive");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Host", "stats.nba.com");
			con.setRequestProperty("Origin", "https://www.nba.com");
			con.setRequestProperty("Referer", "https://www.nba.com/");
			con.setRequestProperty("Sec-Fetch-Dest", "empty");
			con.setRequestProperty("Sec-Fetch-Dest", "cors");
			con.setRequestProperty("Sec-Fetch-Dest", "same-site");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36");
			/* Accept:
			Accept-Encoding: gzip, deflate, br
			Accept-Language: en-US,en;q=0.9
			Access-Control-Request-Headers: x-nba-stats-origin,x-nba-stats-token
			Access-Control-Request-Method: GET
			Connection: keep-alive
			Host: stats.nba.com
			Origin: https://www.nba.com
			Referer: https://www.nba.com/
			Sec-Fetch-Dest: empty
			Sec-Fetch-Mode: cors
			Sec-Fetch-Site: same-site
			User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36*/
			if (payload != null) {
				con.setRequestProperty("Content-Length", String.valueOf(payload.length()));
				con.setDoOutput(true);
				DataOutputStream dos = new DataOutputStream(con.getOutputStream());
				dos.writeBytes(payload);
				dos.flush();
				dos.close();
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();
			while ((line = in.readLine()) != null) {
				response.append(line).append("\n");
			}
			in.close();
			lastCall = System.currentTimeMillis();
			return response.toString();
		} catch (IOException e) {
			throw new DD4StorageException("Error sending request", e);
		}
	}
}
