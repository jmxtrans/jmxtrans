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
package com.googlecode.jmxtrans.attach;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;

/**
 * Util to class to attach to running JVM using PID
 */
class JVMAttacherImpl {
	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

	/**
	 * Connecto to JVM
	 *
	 * @param pid JVM PID
	 * @return JMX Connector address
	 * @throws JVMAttacherException Failed to attach
	 */
	static String attachToJVM(String pid) {
		try {
			VirtualMachine vm = VirtualMachine.attach(pid);
			try {
				String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);

				if (connectorAddress == null) {
					String agent = vm.getSystemProperties().getProperty("java.home") +
							File.separator + "lib" + File.separator + "management-agent.jar";
					vm.loadAgent(agent);

					connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
				}

				return connectorAddress;
			} finally {
				vm.detach();
			}
		} catch (AttachNotSupportedException | IOException | AgentLoadException | AgentInitializationException e) {
			throw new JVMAttacherException("Attach JVM with pid " + pid + " failed", e);
		}
	}

}
