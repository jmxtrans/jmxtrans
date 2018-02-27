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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleNumericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

public class DailyKeyOutWriterTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testWrite() throws Exception {
		// Given
		File logFile = temporaryFolder.newFile("dailykeyout.log");
		DailyKeyOutWriter writer = new DailyKeyOutWriter(
				ImmutableList.<String>of(),
				true, false,
				logFile.getAbsolutePath(),
				null, null, null, null, null,
				ImmutableMap.<String, Object>of()
		);
		Server server = dummyServer();
		Query query = dummyQuery();
		// When
		writer.validateSetup(server, query);
		writer.doWrite(server, query, singleNumericResult());
		// Then
		String log = Files.toString(logFile, Charset.forName("UTF-8"));
		assertThat(log.trim()).isEqualTo("host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount\t10\t0");
	}

}
