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

import org.junit.Test;

import java.io.IOException;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

public class ToStringResultSerializerTest {

	void assertResultString(String serialized) {
		assertThat(serialized).contains("attributeName=ObjectPendingFinalizationCount");
		assertThat(serialized).contains("className=sun.management.MemoryImpl");
		assertThat(serialized).contains("objDomain=ObjectDomainName");
		assertThat(serialized).contains("typeName=type=Memory");
		assertThat(serialized).contains("value=10");
		assertThat(serialized).contains("keyAlias=MemoryAlias");
	}

	@Test
	public void serialize() throws IOException {
		ResultSerializer resultSerializer = new ToStringResultSerializer();
		String serialized = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResult());
		assertThat(serialized).startsWith("Result(");
		assertResultString(serialized);
	}

	@Test
	public void serializeVerbose() throws IOException {
		ResultSerializer resultSerializer = new ToStringResultSerializer(true);
		String serialized = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResult());
		assertThat(serialized).startsWith("Server(");
		assertThat(serialized).contains("host=host.example.net");
		assertThat(serialized).contains("port=4321");
		assertThat(serialized).contains("Query(");
		assertThat(serialized).contains("objectName=myQuery:key=val");
		assertResultString(serialized);
	}
}
