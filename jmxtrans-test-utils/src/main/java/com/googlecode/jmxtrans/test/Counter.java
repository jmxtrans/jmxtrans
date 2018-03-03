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

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

class Counter extends NotificationBroadcasterSupport implements CounterMXBean {
	private int counter = 0;
	private final String name;

	Counter(String name) {
		this.name = name;
	}

	@Override
	public synchronized Integer getValue() {
		int oldValue = counter;
		int newValue = counter++;
		// Send value change via notification also.
		try {
			// Value == sequence nr - just a simple test ;)
			Notification n = new AttributeChangeNotification(this,
					newValue, System.currentTimeMillis(),
					"NotificationValue changed", "NotificationValue", "int",
					oldValue, newValue);

			sendNotification(n);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return newValue;
	}


	@Override
	public String getName() {
		return name;
	}
}
