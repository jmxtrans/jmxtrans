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
package com.googlecode.jmxtrans.model.output.gelf;

import org.assertj.core.api.Condition;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;

/**
 * An AssertJ Condition matcher, that matches, if a given GelfConfiguration matches
 * the default values in the GelfConfiguration class.
 * Beware, as this may change with a new version of GelfClient
 */
public class GelfDefaultConfigurationCondition extends Condition<GelfConfiguration> {
	@Override
	public boolean matches(final GelfConfiguration gelfConfiguration) {
		if (gelfConfiguration.getPort() != 12201) {
			return false;
		}

		if (gelfConfiguration.getTransport() != GelfTransports.TCP) {
			return false;
		}

		if (gelfConfiguration.getQueueSize() != 512) {
			return false;
		}

		if (gelfConfiguration.getConnectTimeout() != 1000) {
			return false;
		}

		if (gelfConfiguration.getReconnectDelay() != 500) {
			return false;
		}

		if (gelfConfiguration.isTcpNoDelay()) {
			return false;
		}

		if (gelfConfiguration.getSendBufferSize() != -1) {
			return false;
		}

		if (gelfConfiguration.isTlsEnabled()) {
			return false;
		}

		if (!gelfConfiguration.isTlsCertVerificationEnabled()) {
			return false;
		}

		if (gelfConfiguration.isTcpKeepAlive()) {
			return false;
		}

		if (gelfConfiguration.getMaxInflightSends() != 512) {
			return false;
		}

		return true;
	}
}
