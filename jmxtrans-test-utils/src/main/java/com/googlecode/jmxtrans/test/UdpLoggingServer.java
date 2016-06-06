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
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkState;

public class UdpLoggingServer extends ExternalResource {
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Nullable private Thread thread = null;

	private final Object startSynchro = new Object();
	private volatile DatagramSocket socket;

	private final ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<String>();
	private final Charset charset;

	public UdpLoggingServer(@Nonnull Charset charset) {
		this.charset = charset;
	}

	private void start() {
		checkState(thread == null, "UDP Server already started");

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Closer closer = Closer.create();
				try {
					try {
						socket = closer.register(new DatagramSocket());
						while (true) {
							processRequests(socket);
						}
					} catch (Throwable t) {
						throw closer.rethrow(t);
					} finally {
						closer.close();
						socket = null;
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
			log.error("UDP server seems to take too long to start", interrupted);
		}
	}

	private void processRequests(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		String messageReceived = new String(packet.getData(), 0, packet.getLength(), charset);
		log.debug("Message received: {}", messageReceived);
		receivedMessages.add(messageReceived);
	}

	public boolean messageReceived(@Nonnull String message) {
		return receivedMessages.contains(message);
	}

	private void stop() {
		checkState(thread != null, "UDP server not started");
		thread.interrupt();
	}

	@Nonnull
	public InetSocketAddress getLocalSocketAddress()  {
		checkState(socket != null, "Server not started");
		return new InetSocketAddress("localhost", socket.getLocalPort());
	}

	@Override
	protected void before() throws Throwable {
		start();
	}

	@Override
	protected void after() {
		stop();
	}
}
