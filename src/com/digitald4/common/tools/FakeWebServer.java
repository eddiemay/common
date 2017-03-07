package com.digitald4.common.tools;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by eddiemay on 3/4/17.
 */
public class FakeWebServer {
	private final int port;
	public FakeWebServer(int port) {
		this.port = port;
	}

	public void start() throws Exception {
		ServerSocket server = new ServerSocket(port);
		Socket client = server.accept();
		try (InputStream in = client.getInputStream();
				 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));) {
			String line;
			byte[] data = new byte[in.available()];
			in.read(data);
			System.out.println(new String(data));
			String result = "{\"status\": \"good\"}";
			bw.write("HTTP/1.1 200 OK\nContent-length: " + result.length() + "\nContent-Type: application/json\n\n");
			bw.write(result);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new FakeWebServer(8080).start();

	}
}
