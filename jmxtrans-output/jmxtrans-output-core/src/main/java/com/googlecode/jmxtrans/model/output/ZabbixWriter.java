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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.OutputStreamBasedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ThreadSafe
public class ZabbixWriter implements OutputStreamBasedOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(ZabbixWriter.class);

	@Nonnull private final JsonFactory jsonFactory;
	@Nonnull private final ImmutableList<String> typeNames;
	@Nonnull private final Boolean addPrefix;
	@Nonnull private final String zabbixKeyTemplate;
	@Nullable private final String zabbixDiscoveryRule;
	@Nullable private final String zabbixDiscoveryKey1;
	@Nullable private final String zabbixDiscoveryValue1;
	@Nullable private final String zabbixDiscoveryKey2;
	@Nullable private final String zabbixDiscoveryValue2;

	public ZabbixWriter(@Nonnull JsonFactory jsonFactory,
						@Nonnull ImmutableList<String> typeNames,
						@Nonnull Boolean addPrefix,
						@Nonnull String zabbixKeyTemplate,
						@Nullable String zabbixDiscoveryRule,
						@Nullable String zabbixDiscoveryKey1,
						@Nullable String zabbixDiscoveryValue1,
						@Nullable String zabbixDiscoveryKey2,
						@Nullable String zabbixDiscoveryValue2
						) {
		this.jsonFactory = jsonFactory;
		this.typeNames = typeNames;
		this.addPrefix = addPrefix;
		this.zabbixKeyTemplate = zabbixKeyTemplate;
		this.zabbixDiscoveryRule = zabbixDiscoveryRule;
		this.zabbixDiscoveryKey1 = zabbixDiscoveryKey1;
		this.zabbixDiscoveryValue1 = zabbixDiscoveryValue1;
		this.zabbixDiscoveryKey2 = zabbixDiscoveryKey2;
		this.zabbixDiscoveryValue2 = zabbixDiscoveryValue2;
	}

	@Override
	public void write(
			@Nonnull OutputStream outputStream,
			@Nonnull InputStream inputStream,
			@Nonnull Charset charset,
			@Nonnull Server server,
			@Nonnull Query query,
			@Nonnull Iterable<Result> results) throws IOException {

		/* Zabbix Sender JSON
		ZABBIX HEADER (see https://www.zabbix.com/documentation/5.0/manual/appendix/protocols/header_datalen)
		ZABBIX BODY (see https://www.zabbix.com/documentation/5.0/manual/appendix/items/trapper)
		{
			"request":"sender data",
			"data":[
				{ "host":"Host name 1", "key":"item_key", "value":"33", "clock": 1381482894 },
				{ "host":"Host name 2", "key":"item_key", "value":"55", "clock": 1381482894 }
			],
			"clock": 1381482905
		}
		*/
		try (
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			OutputStreamWriter w = new OutputStreamWriter(data, charset);
			JsonGenerator g = jsonFactory.createGenerator(w);
			ByteArrayOutputStream data2 = new ByteArrayOutputStream();

			ByteArrayOutputStream data3 = new ByteArrayOutputStream();
			OutputStreamWriter w3 = new OutputStreamWriter(data3, charset);
			JsonGenerator g3 = jsonFactory.createGenerator(w3);
		) {
			// Make output to JSON
			//g.useDefaultPrettyPrinter();
			g.writeStartObject();
			g.writeStringField("request", "sender data");
			g.writeArrayFieldStart("data");

			// Add JMX discovery string to request
			if (zabbixDiscoveryRule != null) {
				Set<String> unique = new HashSet<String>();
				String key = "";

				if (addPrefix) {
					// Add prefix if requested
					key = "jmxtrans.";
				}
				key += zabbixDiscoveryRule;

				g.writeStartObject();
				g.writeStringField("host", server.getLabel());
				g.writeStringField("key", key);
				g3.writeStartArray();

				// Enumerate all results and create uniq list of zabbixDiscoveryValues
				if (zabbixDiscoveryKey2 != null ) {
					// Two keys/values
					for (Result result : results) {
						String value1 = KeyUtils.getKeyStringZabbix(zabbixDiscoveryValue1, query, result, typeNames);
						String value2 = KeyUtils.getKeyStringZabbix(zabbixDiscoveryValue2, query, result, typeNames);
						unique.add(value1+"\0"+value2);
					}
					for (String v: unique) {
						String[] vs = v.split("\0");
						g3.writeStartObject();
						g3.writeStringField("{#"+zabbixDiscoveryKey1+"}", vs[0]);
						g3.writeStringField("{#"+zabbixDiscoveryKey2+"}", vs[1]);
						g3.writeEndObject();
					}
				} else {
					// One key/value
					for (Result result : results) {
						String value1 = KeyUtils.getKeyStringZabbix(zabbixDiscoveryValue1, query, result, typeNames);
						unique.add(value1);
					}
					for (String v: unique) {
						g3.writeStartObject();
						g3.writeStringField("{#"+zabbixDiscoveryKey1+"}", v);
						g3.writeEndObject();
					}
				}
				g3.writeEndArray();
				g3.flush();
				g.writeStringField("value", data3.toString(charset.toString()));
				g.writeEndObject();
			}

			for (Result result : results) {
				String key = "";

				log.debug("Query result: {}", result);

				if (addPrefix) {
					// Add prefix if requested
					key = "jmxtrans.";
				}
				key += KeyUtils.getKeyStringZabbix(zabbixKeyTemplate, query, result, typeNames);
				Object value = result.getValue();

				g.writeStartObject();
				g.writeStringField("host", server.getLabel());
				g.writeStringField("key", key);
				g.writeStringField("value", value.toString());
				g.writeNumberField("clock", result.getEpoch() / 1000);
				g.writeEndObject();
			}

			g.writeEndArray();
			//g.writeNumberField("clock", System.currentTimeMillis() / 1000);
			g.writeEndObject();
			g.flush();

			log.debug("Request: {}", data.toString(charset.toString()));

			// Calculate header
			int dataLen = data.size();
			byte[] header = new byte[] {
				'Z', 'B', 'X', 'D', '\1',
				(byte)(dataLen & 0xFF),
				(byte)((dataLen >> 8) & 0xFF),
				(byte)((dataLen >> 16) & 0xFF),
				(byte)((dataLen >> 24) & 0xFF),
				'\0', '\0', '\0', '\0'};

			// Joint response to one byte array
			data2.write(header);
			data2.write(data.toByteArray());
			data2.flush();

			// Write response to the server
			outputStream.write(data2.toByteArray());
			outputStream.flush();

			// Read answer, cut header and write to debug log
			byte[] response = readAllBytes(inputStream);
			log.debug("Response: {}", new String(Arrays.copyOfRange(response, 5+4+4, response.length), charset));
		}
	}

	public static byte[] readAllBytes(InputStream inputStream) throws IOException {
			final int bufLen = 4 * 0x400; // 4KB
			byte[] buf = new byte[bufLen];
			int readLen;
			IOException exception = null;

			try {
					try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
							while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
									outputStream.write(buf, 0, readLen);

							return outputStream.toByteArray();
					}
			} catch (IOException e) {
					exception = e;
					throw e;
			} finally {
					if (exception == null) inputStream.close();
					else try {
							inputStream.close();
					} catch (IOException e) {
							exception.addSuppressed(e);
					}
			}
	}
}
