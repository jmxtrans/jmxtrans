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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.SocketOutputWriter;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

@EqualsAndHashCode
@ToString
public class ZabbixWriterFactory implements OutputWriterFactory {
	private final boolean booleanAsNumber;
	@Nonnull private final ImmutableList<String> typeNames;
	@Nonnull private final String host;
	@Nonnull private final Integer port;
	private final int socketTimeoutMs;
	@Nonnull private final Boolean addPrefix;
	@Nullable private final String zabbixDiscoveryRule;
	@Nullable private final String zabbixDiscoveryKey;
	@Nullable private final String zabbixDiscoveryValue;

	public ZabbixWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("socketTimeoutMs") Integer socketTimeoutMs,
			@JsonProperty("addPrefix") Boolean addPrefix,
			@JsonProperty("zabbixDiscoveryRule") String zabbixDiscoveryRule,
			@JsonProperty("zabbixDiscoveryKey") String zabbixDiscoveryKey,
			@JsonProperty("zabbixDiscoveryValue") String zabbixDiscoveryValue
			) {
		this.booleanAsNumber = booleanAsNumber;
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
		this.host = checkNotNull(host, "Host cannot be null.");
		this.port = checkNotNull(port, "Port cannot be null.");
		this.socketTimeoutMs = firstNonNull(socketTimeoutMs, 200);
		this.addPrefix = firstNonNull(addPrefix, Boolean.TRUE);
		this.zabbixDiscoveryRule = zabbixDiscoveryRule;
		this.zabbixDiscoveryKey = zabbixDiscoveryKey;
		this.zabbixDiscoveryValue = zabbixDiscoveryValue;
		if (zabbixDiscoveryRule != null) {
			if (zabbixDiscoveryKey == null || zabbixDiscoveryValue == null) {
				throw new NullPointerException("When zabbixDiscoveryRule is used, zabbixDiscoveryKey and zabbixDiscoveryValue must be not null");
			}
		}
	}

	@Override
	public ResultTransformerOutputWriter<SocketOutputWriter<ZabbixWriter>> create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				new SocketOutputWriter<ZabbixWriter>(
						new ZabbixWriter(
								new JsonFactory(),
								typeNames,
								addPrefix,
								zabbixDiscoveryRule,
								zabbixDiscoveryKey,
								zabbixDiscoveryValue
								),
						host,
						port,
						socketTimeoutMs,
						UTF_8
				));
	}
}
