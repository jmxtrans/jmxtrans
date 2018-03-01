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
package com.googlecode.jmxtrans.connections;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.AttributeChangeNotification;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

@ToString
@ThreadSafe
public class JMXConnection implements Closeable {
	@Nullable
	private final JMXConnector connector;
	@Nonnull
	@Getter
	private final MBeanServerConnection mBeanServerConnection;
	private boolean markedAsDestroyed;
	private static final Logger logger = LoggerFactory.getLogger(JMXConnection.class);

	private final IdentityHashMap<Object, QueryNotificationListener> notificationListeners = new IdentityHashMap<>();

	public JMXConnection(@Nullable JMXConnector connector, @Nonnull MBeanServerConnection mBeanServerConnection) {
		this.connector = connector;
		this.mBeanServerConnection = mBeanServerConnection;
		this.markedAsDestroyed = false;
	}

	private static NotificationFilter getNotificationFilter(final ImmutableList<String> attributes) {
		return new NotificationFilter() {
			@Override
			public boolean isNotificationEnabled(Notification notification) {
				if (notification instanceof AttributeChangeNotification) {
					// Check if subscribed attribute
					AttributeChangeNotification changeNotification = (AttributeChangeNotification) notification;
					return attributes.contains(changeNotification.getAttributeName());
				}
				return false;
			}
		};
	}

	public synchronized boolean isNotificationListenerRegistered(Object query) {
		return notificationListeners.containsKey(query);
	}

	public synchronized void addNotificationListener(Object query, ObjectName objectName, ImmutableList<String> attributes) throws IOException, InstanceNotFoundException {
		if (notificationListeners.containsKey(query)) {
			logger.warn("Notification listener was already registered {}", query);
			return;
		}
		QueryNotificationListener notificationListener = new QueryNotificationListener(objectName);
		notificationListeners.put(query, notificationListener);
		this.mBeanServerConnection.addNotificationListener(objectName,
				notificationListener, getNotificationFilter(attributes), null);
	}

	public synchronized List<Notification> getNotifications(Object query) {
		List<Notification> notifications = new ArrayList<>();
		QueryNotificationListener queryNotificationListener = this.notificationListeners.get(query);
		if (queryNotificationListener != null) {
			queryNotificationListener.dumpNotifiations(notifications);
		}
		return notifications;
	}

	public boolean isAlive() {
		return !markedAsDestroyed;
	}

	public void setMarkedAsDestroyed() {
		markedAsDestroyed = true;
	}

	@Override
	public void close() {
		if (connector != null) {
			try {
				connector.close();
			} catch (IOException e) {
				logger.error("Error occurred during close connection {}", this, e);
			}
		}
		for (QueryNotificationListener listener : notificationListeners.values()) {
			try {
				mBeanServerConnection.removeNotificationListener(listener.getObjectName(), listener);
			} catch (Exception ex) {
				logger.error("Error occurred while removing notification listeners {}", listener.getObjectName(), ex);
			}
		}
	}
}
