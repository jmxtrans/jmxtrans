package com.googlecode.jmxtrans.connections;

import com.google.common.io.Closer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServer {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final int port;
	private Thread thread = null;

	public TCPEchoServer(int port) {
		this.port = port;
	}

	public void start() {
		if (thread != null) {
			throw new IllegalStateException("Server already started");
		}
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Closer closer = Closer.create();
				try {
					try {
						ServerSocket server = closer.register(new ServerSocket(port));
						while (true) {
							processRequests(server);
						}
					} catch (Throwable t) {
						throw closer.rethrow(t);
					} finally {
						closer.close();
					}
				} catch (IOException ioe) {
					log.error("Exception in TCP echo server", ioe);
				}

			}
		});
		thread.start();
	}

	private void processRequests(ServerSocket server) throws IOException {
		Closer closer = Closer.create();
		try {
			Socket socket = server.accept();
			BufferedReader in = closer.register(new BufferedReader(new InputStreamReader(socket.getInputStream())));
			PrintWriter out = closer.register(new PrintWriter(socket.getOutputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				out.print(line);
			}
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	public void stop() {
		if (thread == null) {
			throw new IllegalStateException("Server not started");
		}
		thread.interrupt();
	}
}
