/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
package com.googlecode.jmxtrans.test;

import com.google.common.io.Closer;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;

public class TCPEchoServer extends ExternalResource {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Thread thread = null;
	private volatile ServerSocket server;

	private final Object startSynchro = new Object();
	private final ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<>();
	private final AtomicInteger connectionsAccepted = new AtomicInteger();

	@Override
	public void before() {
		start();
	}

	@Override
	protected void after() {
		stop();
	}

	@SuppressWarnings("squid:S2189") // server is only stopped when interrupted. Might be ugly, but good enough for a test server.
	public void start() {
		checkState(thread == null, "Server already started");

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Closer closer = Closer.create();
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
		Socket socket = server.accept();
		connectionsAccepted.incrementAndGet();
		try (
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8))
		){
			synchronized (startSynchro) {
				startSynchro.notifyAll();
			}
			String line;
			while ((line = in.readLine()) != null) {
				receivedMessages.add(line);
				out.print(line);
			}
		}
	}

	public void stop() {
		checkState(thread != null, "Server not started");
		thread.interrupt();
	}

	public boolean messageReceived(@Nonnull String message) {
		return receivedMessages.contains(message);
	}

	public InetSocketAddress getLocalSocketAddress()  {
		checkState(server != null, "Server not started");
		return new InetSocketAddress("localhost", server.getLocalPort());
	}

	public int getConnectionsAccepted() {
		return connectionsAccepted.get();
	}
}
