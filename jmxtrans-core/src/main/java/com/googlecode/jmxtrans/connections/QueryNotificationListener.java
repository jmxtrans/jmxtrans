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
package com.googlecode.jmxtrans.connections;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects received notifications for a query.
 * Notifications should be fetched via {@link #dumpNotifiations(List)}.
 * TODO: if no one is calling dumpNotifiations() but we continue to receive
 * notifications, jmxtrans would eventually go into OOM
 */
class QueryNotificationListener implements NotificationListener {

	private final List<Notification> notifications = new ArrayList<>();

	private final ObjectName objectName;

	QueryNotificationListener(ObjectName objectName) {
		this.objectName = objectName;
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		addNotification(notification);
	}

	private synchronized void addNotification(Notification notification) {
		notifications.add(notification);
	}

	public synchronized void dumpNotifiations(List<Notification> target) {
		target.addAll(notifications);
		notifications.clear();
	}

	public ObjectName getObjectName() {
		return objectName;
	}
}
