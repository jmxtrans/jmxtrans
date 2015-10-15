/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.connections;

import com.google.common.io.Closer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static com.google.common.base.Preconditions.checkState;

public class TCPEchoServer {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Thread thread = null;
	private volatile ServerSocket server;

	private final Object startSynchro = new Object();

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
						server = closer.register(new ServerSocket(0));
						while (true) {
							processRequests(server);
						}
					} catch (Throwable t) {
						throw closer.rethrow(t);
					} finally {
						closer.close();
						server = null;
					}
				} catch (IOException ioe) {
					log.error("Exception in TCP echo server", ioe);
				}

			}
		});
		thread.start();
		try {
			synchronized (startSynchro) {
				startSynchro.wait(1000);
			}
		} catch (InterruptedException interrupted) {
			log.error("TCP Echo server seems to take too long to start", interrupted);
		}
	}

	private void processRequests(ServerSocket server) throws IOException {
		Closer closer = Closer.create();
		try {
			Socket socket = server.accept();
			synchronized (startSynchro) {
				startSynchro.notifyAll();
			}
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
		checkState(thread != null, "Server not started");
		thread.interrupt();
	}

	public InetSocketAddress getLocalSocketAddress()  {
		checkState(server != null, "Server not started");
		return new InetSocketAddress("localhost", server.getLocalPort());
	}
}
