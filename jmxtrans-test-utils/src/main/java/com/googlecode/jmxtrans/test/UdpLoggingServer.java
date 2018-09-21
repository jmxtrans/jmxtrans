/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.test;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkState;

public class UdpLoggingServer extends ExternalResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(UdpLoggingServer.class);

	private final Charset charset;
	private ProcessRequestLoop loop;

	public UdpLoggingServer(@Nonnull Charset charset) {
		this.charset = charset;
	}

	private static class ProcessRequestLoop implements Runnable {
		private final DatagramSocket socket;
		private final Charset charset;
		private final AtomicBoolean running = new AtomicBoolean();
		private final ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<String>();

		private ProcessRequestLoop(Charset charset) throws SocketException {
			this.socket = new DatagramSocket();
			running.set(true);
			this.charset = charset;
		}

		public int getPort() {
			return this.socket.getLocalPort();
		}

		@Override
		public void run() {
			LOGGER.info("UDP server started on port {}", getPort());
			try {
				while (isRunning()) {
					processRequests();
				}
			} catch (IOException ioe) {
				LOGGER.error("Exception in UDP server", ioe);
			} finally {
				running.set(false);
			}
		}

		private boolean isRunning() {
			return running.get();
		}

		private void processRequests() throws IOException {
			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);
			String messageReceived = new String(packet.getData(), 0, packet.getLength(), charset);
			LOGGER.debug("Message received: {}", messageReceived);
			receivedMessages.add(messageReceived);
		}

		private boolean messageReceived(@Nonnull String message) {
			return receivedMessages.contains(message);
		}

		private void stop() {
			running.set(false);
		}
	}

	@Override
	protected void before() throws Throwable {
		checkState(!isRunning(), "UDP Server already started");
		loop = new ProcessRequestLoop(charset);
		Thread thread = new Thread(loop);
		thread.start();
	}

	public boolean isRunning() {
		return loop != null && loop.isRunning();
	}

	public boolean messageReceived(@Nonnull String message) {
		checkState(loop != null, "UDP server not started");
		return loop.messageReceived(message);
	}

	@Nonnull
	public InetSocketAddress getLocalSocketAddress() {
		checkState(isRunning(), "Server not started");
		return new InetSocketAddress("localhost", loop.getPort());
	}


	@Override
	protected void after() {
		checkState(isRunning(), "UDP server not started");
		loop.stop();
	}
}
