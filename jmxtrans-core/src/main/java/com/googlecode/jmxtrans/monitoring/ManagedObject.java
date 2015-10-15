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
package com.googlecode.jmxtrans.monitoring;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * The Interface ManagedObject represents a MBean managed object,
 * force some ObjectName methods, used on register / unregister
 * in the MBeanServer.
 *
 * @author marcos.lois
 */
public interface ManagedObject {

	/**
	 * Gets the object name.
	 *
	 * @return the object name
	 * @throws MalformedObjectNameException if the object name is not valid
	 */
	ObjectName getObjectName() throws MalformedObjectNameException;

	/**
	 * Sets the object name.
	 *
	 * @param objectName the object name
	 * @throws MalformedObjectNameException if the object name is not valid
	 */
	void setObjectName(ObjectName objectName) throws MalformedObjectNameException;

	/**
	 * Sets the object name.
	 *
	 * @param objectName the object name
	 * @throws MalformedObjectNameException if the object name is not valid
	 */
	void setObjectName(String objectName) throws MalformedObjectNameException;
}
