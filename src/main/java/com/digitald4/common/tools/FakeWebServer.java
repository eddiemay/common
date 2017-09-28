package com.digitald4.common.tools;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FakeWebServer {
	private final int port;
	private FakeWebServer(int port) {
		this.port = port;
	}

	private void start() throws Exception {
		ServerSocket server = new ServerSocket(port);
		// for (int x = 0; x < 5; x++) {
			Socket client = server.accept();
			try (InputStream in = client.getInputStream();
					 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
				byte[] header = new byte[512];
				int len = in.read(header);
				System.out.println("Header length: " + len + " content: " + new String(header));
				byte[] data = new byte[128];
				len = in.read(data);
				System.out.println("Data Content length: " + len + " content: " + new String(data));
				String result = "{\"status\": \"good\"}";
				bw.write("HTTP/1.1 200 OK\nContent-length: " + result.length() + "\nContent-Type: application/json\n\n");
				bw.write(result);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		// }
	}

	public static void main(String[] args) throws Exception {
		new FakeWebServer(8181).start();
	}
}
