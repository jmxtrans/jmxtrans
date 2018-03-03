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
package com.googlecode.jmxtrans.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Processes received notifications and passes them to output writers
 * after converting each notification to a result object.
 * A notification processor instance is registered as a "handback" object
 * per query when registering notification listeners.
 * Background: notification listeners need to be serializable and are
 * send over the wire via RMI when registering. The listener should only contain
 * logic and dispatch to the local "handback" when receiving a notification.
 */
class NotificationProcessor {
	private static final Logger logger = LoggerFactory.getLogger(NotificationProcessor.class);

	private final Server server;
	private final Query query;
	private final ObjectInstance oi;

	public NotificationProcessor(Server server, Query query, ObjectInstance oi) {
		this.server = server;
		this.query = query;
		this.oi = oi;
	}

	public void handleNotification(Notification notification) {
		List<Result> results = transform(notification);
		for (OutputWriter outputWriter : query.getOutputWriterInstances()) {
			try {
				outputWriter.doWrite(server, query, results);
			} catch (Exception e) {
				logger.error("Error during notification processing", e);
			}
		}
	}

	private List<Result> transform(Notification notification) {
		if (notification instanceof AttributeChangeNotification) {
			AttributeChangeNotification changeNotification = (AttributeChangeNotification) notification;
			List<Attribute> attributes = new ArrayList<>(1);
			attributes.add(new Attribute(changeNotification.getAttributeName(),
					changeNotification.getNewValue()));
			return new JmxResultProcessor(query, oi, attributes,
					oi.getClassName(), query.getObjectName().getDomain()).getResults();
		} else {
			//TODO: log a warning here?
		}
		return Collections.emptyList();
	}
}
