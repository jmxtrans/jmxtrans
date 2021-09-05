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
package com.googlecode.jmxtrans;

import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JmxTransformerTest {

	@Test
	public void startDateIsSpreadAccordingToRunPeriod() {
		JmxTransformer jmxTransformer = createJmxTransformer(new JmxTransConfiguration());

		Date now = new Date();

		assertThat(jmxTransformer.computeSpreadStartDate(60))
				.isBetween(now, new Date(now.getTime() + MILLISECONDS.convert(60, SECONDS)));
	}

	@Test
	public void findProcessConfigFiles() throws URISyntaxException {
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		configuration.setProcessConfigDir(new File(ConfigurationParserTest.class.getResource("/example.json").toURI()).getParentFile());
		JmxTransformer jmxTransformer = createJmxTransformer(configuration);

		List<File> processConfigFiles = jmxTransformer.getProcessConfigFiles();

		assertThat(processConfigFiles).hasSize(7);
	}

	private JmxTransformer createJmxTransformer(JmxTransConfiguration configuration) {
		return new JmxTransformer(null, configuration, null, null, mock(ExecutorRepository.class), mock(ExecutorRepository.class), null);
	}
}
