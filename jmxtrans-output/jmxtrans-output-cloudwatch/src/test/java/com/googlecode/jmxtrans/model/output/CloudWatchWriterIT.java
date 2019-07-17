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
package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.util.ProcessConfigUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.googlecode.jmxtrans.guice.JmxTransModule.createInjector;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudWatchWriter}.
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
@Category({RequiresIO.class, IntegrationTest.class})
@PowerMockIgnore("javax.management.*")
public class CloudWatchWriterIT {

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		ProcessConfigUtils processConfigUtils = createInjector(new JmxTransConfiguration()).getInstance(ProcessConfigUtils.class);
		File input = new File(CloudWatchWriterIT.class.getResource("/cloud-watch.json").toURI());
		JmxProcess process = processConfigUtils.parseProcess(input);
		assertThat(process.getName()).isEqualTo("cloud-watch.json");

	}

}
