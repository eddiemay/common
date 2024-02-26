package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

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

	public APIConnector loadIdToken() {
		try (BufferedReader br = new BufferedReader(new FileReader("data/id.token"))) {
			this.idToken = br.readLine();
		} catch (IOException ioe) {
			throw new DD4StorageException("Error reading Id Token", ioe);
		}
		return this;
	}

	public String sendPost(String url, String payload) {
		return send("POST", url, payload);
	}

	public String sendGet(String url) {
		return send("GET", url, null);
	}

	public String send(String method, String url, String payload) {
		try {
			return readStream(getInputStream(method, url, payload));
		} catch (IOException e) {
			throw new DD4StorageException("Error sending request", e);
		}
	}

	public InputStream getInputStream(String method, String url, String payload) throws IOException {
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
		con.setConnectTimeout(10000);
		con.setRequestMethod(method);
		con.setRequestProperty("Accept", "*/*");
		// con.setRequestProperty("Accept-Encoding", "zip, deflate, br");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
		con.setRequestProperty("Access-Control-Request-Method", "GET");
		con.setRequestProperty("Cache-Control", "max-age=0");
		con.setRequestProperty("Connection", "keep-alive");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Origin", "http://localhost");
		con.setRequestProperty("Referer", "http://localhost/");
		con.setRequestProperty("Sec-Fetch-Dest", "empty");
		con.setRequestProperty("Sec-Fetch-Dest", "cors");
		con.setRequestProperty("Sec-Fetch-Dest", "same-site");
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36");

		if (payload != null) {
			con.setRequestProperty("Content-Length", String.valueOf(payload.length()));
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			// System.out.println("payload: " + payload);
			dos.write(payload.getBytes());
			dos.flush();
			dos.close();
		}

		lastCall = System.currentTimeMillis();
		try {
			return con.getInputStream();
		} catch (IOException e) {
			System.out.println(readStream(con.getErrorStream()));
			throw new DD4StorageException("Error sending request", e);
		}
	}

	private static String readStream(InputStream inputStream) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			StringBuilder response = new StringBuilder();
			while ((line = br.readLine()) != null) {
				response.append(line).append("\n");
			}
			return response.toString();
		} catch (IOException e) {
			throw new DD4StorageException("Error sending request", e);
		}
	}
}
