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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;

public class GelfWriterTests {

	private Result result;

	@Before
	public void setUp() throws Exception {
		this.result = new Result(1, "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap
			.of("key", (Object)1));

	}

	@Test
	public void testSendGelfMessage() throws Exception {
		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String,Object> settings = new HashMap<String,Object>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfWriter gelfWriter;

		try {
			gelfWriter = new GelfWriter(
				typenames,
				true,
				true,
				settings,
				"gelf.example.com",
				null,
				additionalFields,
				"TEST",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);
		} catch (final NullPointerException e) {
			throw new RuntimeException("Unexpected failure to create gelfwriter for test", e);
		}
		gelfWriter.start();
		gelfWriter.doWrite(dummyServer(), dummyQuery(), ImmutableList.<Result>of(result));
		Assert.assertTrue(
			"Transport hasn't been called",
			MockGelfTransport.calls.size() > 0
		);

		Assert.assertEquals(
			"Additional fields not set",
			"test",
			MockGelfTransport.calls.get(0).getAdditionalFields().get("test")
		);
	}

}
