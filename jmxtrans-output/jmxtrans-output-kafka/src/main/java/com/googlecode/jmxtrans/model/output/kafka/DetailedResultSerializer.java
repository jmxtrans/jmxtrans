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
package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static java.util.Collections.singletonList;

@EqualsAndHashCode(exclude = {"objectMapper"})
public class DetailedResultSerializer implements ResultSerializer {
	private final ObjectMapper objectMapper;

	@JsonCreator
	public DetailedResultSerializer() {
		this.objectMapper = new ObjectMapper();
	}

	@Nonnull
	@Override
	public Collection<String> serialize(final Server server, final Query query, final Result result) throws IOException {
		return singletonList(objectMapper.writeValueAsString(new KResult(server, result)));
	}

	/**
	 * DTO containing server and result information
	 */
	@JsonSerialize(include = NON_NULL)
	@Immutable
	@ThreadSafe
	private static class KResult {
		// Server
		@Getter
		private final String alias;
		@Getter
		private final String pid;
		@Getter
		private final String host;
		@Getter
		private final String port;
		@Getter
		private final String source;
		// Result
		@Getter
		private final String attributeName;
		@Getter
		private final String className;
		@Getter
		private final String objDomain;
		@Getter
		private final String typeName;
		@Getter
		private final Map<String, String> typeNameMap;
		@Getter
		private final long epoch;
		@Getter
		private final String keyAlias;
		@Getter
		private final ImmutableList<String> valuePath;
		@Getter
		private final Object value;

		private KResult(Server server, Result result) {
			alias = server.getAlias();
			pid = server.getPid();
			host = server.getHost();
			port = server.getPort();
			source = server.getSource();
			attributeName = result.getAttributeName();
			className = result.getClassName();
			objDomain = result.getObjDomain();
			typeName = result.getTypeName();
			typeNameMap = result.getTypeNameMap();
			epoch = result.getEpoch();
			keyAlias = result.getKeyAlias();
			this.valuePath = result.getValuePath();
			this.value = result.getValue();
		}
	}
}
