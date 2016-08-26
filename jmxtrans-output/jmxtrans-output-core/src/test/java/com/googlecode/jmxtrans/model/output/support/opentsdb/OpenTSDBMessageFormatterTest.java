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
package com.googlecode.jmxtrans.model.output.support.opentsdb;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class OpenTSDBMessageFormatterTest {

	protected Query mockQuery;
	protected Result mockResult;

	@Before
	public void setupTest() {
		this.mockQuery = Mockito.mock(Query.class);
		this.mockResult = Mockito.mock(Result.class);

		// Setup common mock interactions.
		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("x-att1-x", (Object) "120021"));
		Mockito.when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		Mockito.when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		Mockito.when(this.mockResult.getTypeName()).
				thenReturn("Type=x-type-x,Group=x-group-x,Other=x-other-x,Name=x-name-x");

	}

	private OpenTSDBMessageFormatter createDefaultFormatter() throws LifecycleException, UnknownHostException {
		return new OpenTSDBMessageFormatter(ImmutableList.of("Type", "Group", "Name", "Missing"), ImmutableMap.<String, String>of());
	}

	@Test
	public void testMergedTypeNameValues() throws Exception {
		OpenTSDBMessageFormatter formatter = createDefaultFormatter();

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));
		String resultString = strings.iterator().next();
		Assert.assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(resultString.matches(".*\\btype=x-att1-x\\b.*"));
		Assert.assertTrue(resultString.matches(".*\\bTypeGroupNameMissing=x-type-x_x-group-x_x-name-x\\b.*"));
	}

	@Test
	public void testNonMergedTypeNameValues() throws Exception {

		OpenTSDBMessageFormatter formatter =
				new OpenTSDBMessageFormatter(ImmutableList.of("Type", "Group", "Name", "Missing"),
						ImmutableMap.<String, String>of(), OpenTSDBMessageFormatter.DEFAULT_TAG_NAME, null, false, null);

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));
		String resultString = strings.iterator().next();
		Assert.assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(resultString.matches(".*\\btype=x-att1-x\\b.*"));
		Assert.assertTrue(resultString.matches(".*\\bType=x-type-x\\b.*"));
		Assert.assertTrue(resultString.matches(".*\\bGroup=x-group-x\\b.*"));
		Assert.assertTrue(resultString.matches(".*\\bName=x-name-x\\b.*"));
		Assert.assertTrue(resultString.matches(".*\\bMissing=(\\s.*|$)"));
	}

	@Test
	public void testEmptyTagSetting() throws Exception {

		OpenTSDBMessageFormatter formatter =
				new OpenTSDBMessageFormatter(ImmutableList.<String>of(), ImmutableMap.<String, String>of());

		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("X-ATT-X", (Object) "120021"));

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));
		Assert.assertTrue(
				strings.iterator().next().matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021 host=[^ ]*$"));
	}

	@Test
	public void testMultipleTags() throws Exception {

		Map<String, String> tagMap;

		// Verify tag map with multiple values.
		tagMap = newHashMap();
		tagMap.put("x-tag1-x", "x-tag1val-x");
		tagMap.put("x-tag2-x", "x-tag2val-x");
		tagMap.put("x-tag3-x", "x-tag3val-x");

		OpenTSDBMessageFormatter formatter =
				new OpenTSDBMessageFormatter(ImmutableList.of("Type", "Group", "Name", "Missing"), ImmutableMap.copyOf(tagMap));

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));

		String metricString = strings.iterator().next();

		Assert.assertTrue(metricString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(metricString.matches(".*\\bhost=.*"));
		Assert.assertTrue(metricString.matches(".*\\bx-tag1-x=x-tag1val-x\\b.*"));
		Assert.assertTrue(metricString.matches(".*\\bx-tag2-x=x-tag2val-x\\b.*"));
		Assert.assertTrue(metricString.matches(".*\\bx-tag3-x=x-tag3val-x\\b.*"));

	}

	@Test
	public void testAddHostnameTag() throws Exception {

		OpenTSDBMessageFormatter formatter = createDefaultFormatter();
		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));
		Assert.assertTrue(strings.iterator().next().matches(".*host=.*"));

	}

	@Test
	public void testDontAddHostnameTag() throws Exception {

		OpenTSDBMessageFormatter formatter =
				new OpenTSDBMessageFormatter(ImmutableList.of("Type", "Group", "Name", "Missing"),
						ImmutableMap.<String, String>of(), OpenTSDBMessageFormatter.DEFAULT_TAG_NAME, null, true, null);

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));
		Assert.assertFalse(strings.iterator().next().matches(".*host=.*"));

	}

	@Test
	public void testEmptyResultValues() throws Exception {

		OpenTSDBMessageFormatter formatter = createDefaultFormatter();

		ImmutableMap<String, Object> values = ImmutableMap.of();
		Mockito.when(this.mockResult.getValues()).thenReturn(values);

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(0, Iterables.size(strings));

	}

	@Test
	public void testOneValueMatchingAttribute() throws Exception {

		OpenTSDBMessageFormatter formatter = createDefaultFormatter();

		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("X-ATT-X", (Object) "120021"));

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(1, Iterables.size(strings));
		String resultString = strings.iterator().next();

		Assert.assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(resultString.matches(".*\\bhost=.*"));
		Assert.assertFalse(resultString.matches(".*\\btype=.*"));

	}

	@Test
	public void testMultipleValuesWithMatchingAttribute() throws Exception {

		OpenTSDBMessageFormatter formatter = createDefaultFormatter();

		Mockito.when(this.mockResult.getValues()).
				thenReturn(ImmutableMap.of("X-ATT-X", (Object) "120021", "XX-ATT-XX", (Object) "210012"));

		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));

		Assert.assertEquals(2, Iterables.size(strings));
		Iterator<String> resultStringIterator = strings.iterator();
		String resultString1 = resultStringIterator.next();
		String resultString2 = resultStringIterator.next();

		String xLine;
		String xxLine;
		if (resultString1.contains("XX-ATT-XX")) {
			xxLine = resultString1;
			xLine = resultString2;
		} else {
			xLine = resultString1;
			xxLine = resultString2;
		}

		Assert.assertTrue(xLine.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(xLine.matches(".*\\btype=X-ATT-X\\b.*"));

		Assert.assertTrue(xxLine.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 210012.*"));
		Assert.assertTrue(xxLine.matches(".*\\btype=XX-ATT-XX\\b.*"));

	}

	@Test
	public void testNonNumericValue() throws Exception {

		OpenTSDBMessageFormatter formatter = createDefaultFormatter();
		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("X-ATT-X", (Object) "THIS-IS-NOT-A-NUMBER"));
		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));
		Assert.assertEquals(0, Iterables.size(strings));

	}

	@Test
	public void testJexlNaming() throws Exception {

		OpenTSDBMessageFormatter formatter =
				new OpenTSDBMessageFormatter(ImmutableList.of("Type", "Group", "Name", "Missing"),
						ImmutableMap.<String, String>of(), OpenTSDBMessageFormatter.DEFAULT_TAG_NAME, "'xx-jexl-constant-name-xx'", true, null);


		Iterable<String> strings = formatter.formatResults(ImmutableList.of(this.mockResult));
		Assert.assertEquals(1, Iterables.size(strings));
		Assert.assertTrue(strings.iterator().next().matches("^xx-jexl-constant-name-xx 0 120021.*"));

	}

	@Test(expected = LifecycleException.class)
	public void testInvalidJexlNaming() throws Exception {

		new OpenTSDBMessageFormatter(ImmutableList.of("Type", "Group", "Name", "Missing"),
						ImmutableMap.<String, String>of(), OpenTSDBMessageFormatter.DEFAULT_TAG_NAME, "invalid expression here", true, null);

	}

}