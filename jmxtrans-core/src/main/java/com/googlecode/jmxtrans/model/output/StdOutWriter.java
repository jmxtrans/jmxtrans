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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import lombok.ToString;

import java.util.Map;

/**
 * Basic filter good for testing that just outputs the Result objects using
 * System.out.
 *
 * @author jon
 */
@ToString
public class StdOutWriter implements OutputWriterFactory {

	private final ImmutableList<String> typeNames;
	private final  boolean booleanAsNumber;
	private final  Boolean debugEnabled;
	private final ResultSerializer resultSerializer;
	private final  Map<String, Object> settings;

	@JsonCreator
	public StdOutWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("resultSerializer") ResultSerializer resultSerializer,
			@JsonProperty("settings") Map<String, Object> settings
			) {
			this.typeNames = typeNames;
			this.booleanAsNumber = booleanAsNumber;
			this.debugEnabled = debugEnabled;
			this.settings = settings;
			this.resultSerializer = MoreObjects.firstNonNull(resultSerializer, ToStringResultSerializer.DEFAULT);
	}

	@Override
	public OutputWriter create() {
		return new W(
				typeNames,
				booleanAsNumber,
				debugEnabled,
				settings,
				resultSerializer
		);
	}


	public static class W extends BaseOutputWriter {
		private final ResultSerializer resultSerializer;
		public W (
				ImmutableList<String> typeNames,
				boolean booleanAsNumber,
				Boolean debugEnabled,
				Map<String, Object> settings,
				ResultSerializer resultSerializer) {
			super(typeNames, booleanAsNumber, debugEnabled, settings);
			this.resultSerializer = resultSerializer;
		}

		@Override
		public void validateSetup(Server server, Query query) throws ValidationException {
			// nothing to validate
		}

		@Override
		@SuppressWarnings("squid:S106") // using StdOut is the goal of StdOutWriter
		protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
			for (Result r : results) {
				String s = resultSerializer.serialize(server, query, r);
				if (s != null) {
					System.out.println(s);
				}
			}
		}
	}
}
