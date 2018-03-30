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
package com.googlecode.jmxtrans.notifications;

import com.googlecode.jmxtrans.connections.JMXConnection;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.model.JmxResultProcessor;
import com.googlecode.jmxtrans.model.NotificationProcessor;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Processes received notifications and passes them to output writers
 * after converting each notification to a result object.
 * A notification processor instance is registered as a "handback" object
 * per {@link ObjectName} in {@link #subscribeToNotifications()}.
 *
 * <p>
 * Notification listeners need to be serializable and are
 * send over the wire via RMI when registering. The listener should only contain
 * logic and dispatch to the local "handback" when receiving a notification.
 * </p>
 * <p>
 * We cannot know if the remote adds further MXBean matching our name query.
 * Therefore, the subscriptions are checked periodically. The notification
 * processor performs book keeping of already subscribed object names
 * to avoid duplicate listeners.
 * </p>
 * <p>
 * Notification processors are managed in an object pool, one processor
 * per server - query tuple. If anything goes wrong during
 * {@link #subscribeToNotifications()} the notification processor
 * will be closed ({@link #close()}) and removed from the pool.
 * The next call to subscribe will make sure that subscriptions
 * are re-created.
 * </p>
 */
public class NotificationProcessorImpl implements NotificationProcessor {
	private static final Logger logger = LoggerFactory.getLogger(NotificationProcessorImpl.class);

	private final Server server;
	private final Query query;
	private final ExecutorRepository resultExecutorRepository;
	private final JMXConnection jmxConnection;
	private final Map<ObjectName, NotificationListener> registeredListeners = new HashMap<>();

	NotificationProcessorImpl(Server server, Query query, ExecutorRepository resultExecutorRepository, JMXConnection jmxConnection) {
		this.server = server;
		this.query = query;
		this.resultExecutorRepository = resultExecutorRepository;
		this.jmxConnection = jmxConnection;
	}

	/**
	 * Needs to be synchronized to ensure thread-safe access of registeredListeners map.
	 */
	@Override
	public synchronized void subscribeToNotifications() throws Exception {
		MBeanServerConnection mbeanServer = jmxConnection.getMBeanServerConnection();
		// Query remote object names periodically to 1) find out if the JMX connection is still alive
		// and 2) ensure that we have a notification listener in place for all object names.
		Set<ObjectName> queryNames = mbeanServer.queryNames(query.getObjectName(), null);
		Set<ObjectName> queryNamesToSubscribe = getQueryNamesToSubscribe(queryNames);
		Set<Map.Entry<ObjectName, NotificationListener>> queryNamesToUnSubscribe = getListenersToUnSubscribe(queryNames);
		for (ObjectName queryName : queryNamesToSubscribe) {
			final ObjectInstance oi = mbeanServer.getObjectInstance(queryName);
			addNotificationListener(oi);
		}
		for (Map.Entry<ObjectName, NotificationListener> entry : queryNamesToUnSubscribe) {
			removeNotificationListener(entry);
		}

	}

	private void removeNotificationListener(Map.Entry<ObjectName, NotificationListener> entry) throws IOException, InstanceNotFoundException, ListenerNotFoundException {
		logger.info("Removing notification listener for object name {}.", entry.getKey());
		registeredListeners.remove(entry.getKey());
		jmxConnection.removeNotificationListener(entry.getKey(), entry.getValue());
	}

	private void addNotificationListener(final ObjectInstance oi) throws IOException, InstanceNotFoundException {
		Object handback = this;
		logger.info("Adding notification listener for object instance {}.", oi);
		NotificationListener listener = new NotificationListener() {
			@Override
			public void handleNotification(Notification notification, Object handback) {
				logger.info("Handle notification {}.", notification);
				NotificationProcessorImpl handler = (NotificationProcessorImpl) handback;
				if (notification instanceof AttributeChangeNotification) {
					handler.handleNotification(oi, (AttributeChangeNotification) notification);
				} else {
					logger.error("Don't know how to process this notification {}.", notification);
				}
			}
		};
		//Internal bookkeeping so we know which listeners are registered.
		registeredListeners.put(oi.getObjectName(), listener);
		jmxConnection.addNotificationListener(oi.getObjectName(),
				listener,
				null, // TODO: filter??
				handback);
	}

	/**
	 * Gets listeners which should be unsubscribed. Compares object names of registered listeners with
	 * existing object names from the MBean.
	 *
	 * @param queryNames from remote MBean
	 * @return object names / listeners to be unsubscribed
	 */
	private synchronized Set<Map.Entry<ObjectName, NotificationListener>> getListenersToUnSubscribe(Set<ObjectName> queryNames) {
		Set<Map.Entry<ObjectName, NotificationListener>> toUnSubscribe = new HashSet<>();
		for (Map.Entry<ObjectName, NotificationListener> registeredListener : registeredListeners.entrySet()) {
			if(!queryNames.contains(registeredListener.getKey())) {
				toUnSubscribe.add(registeredListener);
			}
		}
		return toUnSubscribe;
	}

	/**
	 * Gets query names for which notification listeners need to be subscribed.
	 *
	 * @param queryNames from remote MBean
	 * @return query names to be subscribed
	 */
	private synchronized Set<ObjectName> getQueryNamesToSubscribe(Set<ObjectName> queryNames) {
		Set<ObjectName> toSubscribe = new HashSet<>();
		for (ObjectName queryName : queryNames) {
			if (registeredListeners.containsKey(queryName)) {
				// Already added.
				logger.debug("Query name {} was already added.", queryName);
				continue;
			}
			toSubscribe.add(queryName);
		}
		return toSubscribe;
	}

	private void handleNotification(final ObjectInstance oi, final AttributeChangeNotification notification) {
		ThreadPoolExecutor executor = resultExecutorRepository.getExecutor(server);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				List<Result> results = transform(oi, notification);
				for (OutputWriter outputWriter : query.getOutputWriterInstances()) {
					try {
						outputWriter.doWrite(server, query, results);
					} catch (Exception e) {
						logger.error("Error during notification processing", e);
					}
				}
			}
		};
		executor.submit(task);
	}

	private List<Result> transform(ObjectInstance oi, AttributeChangeNotification changeNotification) {
		List<Attribute> attributes = new ArrayList<>(1);
		attributes.add(new Attribute(changeNotification.getAttributeName(),
				changeNotification.getNewValue()));
		long epoch = changeNotification.getTimeStamp();
		return new JmxResultProcessor(query, oi, attributes,
				oi.getClassName(), query.getObjectName().getDomain(),
				epoch).getResults();
	}

	@Override
	public void close() {
		jmxConnection.close();
	}
}
