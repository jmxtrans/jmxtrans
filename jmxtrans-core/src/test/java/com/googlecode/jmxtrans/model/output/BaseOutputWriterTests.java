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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Zack Radick Date: 1/20/12
 */
public class BaseOutputWriterTests {

	@Before
	public void setUpOutputWriter() {
		System.setProperty("myHost", "w2");
		System.setProperty("myPort", "123");
	}

	private Result newBooleanResult(boolean value) {
		return new Result(0, "", "", "", "", "", ImmutableList.<String>of(Boolean.toString(value)), value);
	}

	@Test
	public void booleanValuesAreTransformedToNumber() throws Exception {
		final ArrayList<Result> processedResults = Lists.newArrayList();
		BaseOutputWriter outputWriter = new BaseOutputWriter(
				ImmutableList.<String>of(), true, false, Maps.<String, Object>newHashMap()) {
			@Override
			protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
				processedResults.addAll(results);
			}
			@Override
			public void validateSetup(Server server, Query query) throws ValidationException {
			}
		};
		ImmutableList<Result> results = ImmutableList.of(newBooleanResult(true), newBooleanResult(false));
		outputWriter.doWrite(null, null, results);

		assertThat(processedResults).hasSize(2);
		assertThat(processedResults.get(0).getValue()).isEqualTo(1);
		assertThat(processedResults.get(1).getValue()).isEqualTo(0);
	}

	@Test
	public void booleanValuesAreNotTransformedToNumber() throws Exception {
		final ArrayList<Result> processedResults = Lists.newArrayList();
		BaseOutputWriter outputWriter = new BaseOutputWriter(
				ImmutableList.<String>of(), false, false, Maps.<String, Object>newHashMap()) {
			@Override
			protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
				processedResults.addAll(results);
			}
			@Override
			public void validateSetup(Server server, Query query) throws ValidationException {
			}
		};
		ImmutableList<Result> results = ImmutableList.of(newBooleanResult(true), newBooleanResult(false));
		outputWriter.doWrite(null, null, results);

		assertThat(processedResults).hasSize(2);
		assertThat(processedResults.get(0).getValue()).isEqualTo(true);
		assertThat(processedResults.get(1).getValue()).isEqualTo(false);
	}

	@After
	public void removeSystemProperties() {
		System.clearProperty("myHost");
		System.clearProperty("myPort");
	}

	private static final class TestBaseOuputWriter extends BaseOutputWriter {
		public TestBaseOuputWriter() {
			super(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap());
		}

		@Override
		public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
			throw new UnsupportedOperationException("doWrite() not implemented for TestBaseOutputWriter.");
		}

		@Override
		public void validateSetup(Server server, Query query) throws ValidationException {
			throw new UnsupportedOperationException("validateSetup() not implemented for TestBaseOutputWriter.");
		}
	}
}
