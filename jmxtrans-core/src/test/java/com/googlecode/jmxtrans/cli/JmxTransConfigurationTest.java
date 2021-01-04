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
package com.googlecode.jmxtrans.cli;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxTransConfigurationTest {

	@Test
	public void loadProperties() throws IOException {
		try (InputStream inputStream = getClass().getResourceAsStream("/example-jmxtrans.properties")) {
			Properties properties = new Properties();
			properties.load(inputStream);
			JmxTransConfiguration configuration = new JmxTransConfiguration();
			configuration.loadProperties(properties);
			assertThat(configuration.isContinueOnJsonError()).isTrue();
			assertThat(configuration.isRunEndlessly()).isTrue();
			assertThat(configuration.getRunPeriod()).isEqualTo(45);
			assertThat(configuration.getQueryProcessorExecutorPoolSize()).isEqualTo(4);
			assertThat(configuration.getQueryProcessorExecutorWorkQueueCapacity()).isEqualTo(1000);
			assertThat(configuration.getResultProcessorExecutorPoolSize()).isEqualTo(8);
			assertThat(configuration.getResultProcessorExecutorWorkQueueCapacity()).isEqualTo(2000);
			assertThat(configuration.getAdditionalJars()).containsExactly(new File("lib1.jar"), new File("lib2.jar"));
		}
	}

}
