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
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static com.google.common.collect.Lists.newArrayList;

public class ZabbixWriterTest {

	@Test
	public void metricsAreFormattedCorrectly() throws IOException {
		ZabbixWriter zabbixWriter = new ZabbixWriter(new JsonFactory(), ImmutableList.<String>of(), Boolean.TRUE, "${MBEAN}.${TYPENAMES}.${KEY}", null, null, null, null, null);

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		ByteArrayInputStream dataIn = new ByteArrayInputStream(new String("1234567890ABC{\"response\":\"success\",\"info\":\"processed: 2; failed: 0; total: 2; seconds spent: 0.000056\"}").getBytes());

		zabbixWriter.write(dataOut, dataIn, Charset.forName("UTF-8"), dummyServer(), dummyQuery(), dummyResults());

		String json = new String(dataOut.toByteArray());

		/* Zabbix Sender JSON
		{
			"request":"sender data",
			"data":[
				{"host":"host.example.net","key":"jmxtrans.MemoryAlias..ObjectPendingFinalizationCount","value":"10","clock":0},
				{"host":"host.example.net","key":"jmxtrans.VerboseMemory..Verbose","value":"true","clock":0},
				{"host":"host.example.net","key":"jmxtrans.VerboseMemory..Verbose","value":"false","clock":0}
			],
			"clock": 1381482905
		}
		*/

		assertThat(json).startsWith("ZBXD");
		// Skip header
		json = json.substring(5+4+4);
		//System.out.println(json);

		assertThatJson(json)
			.node("request").isEqualTo("sender data");
		assertThatJson(json)
			.node("data").isArray().ofLength(3);
		assertThatJson(json)
			.node("data[0].host").isEqualTo("host.example.net")
			.node("data[0].key").isEqualTo("jmxtrans.MemoryAlias..ObjectPendingFinalizationCount")
			.node("data[0].value").isEqualTo("\"10\"")
			.node("data[0].clock").isEqualTo(0)
			;
	}

	@Test
	public void metricsAreFormattedCorrectlyDiscovery1() throws IOException {
		ZabbixWriter zabbixWriter = new ZabbixWriter(new JsonFactory(), ImmutableList.<String>of(), Boolean.TRUE, "${MBEAN}.${TYPENAMES}.${KEY}", "discoveryRule", "discoveryKey", "discoveryValue", null, null);

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		ByteArrayInputStream dataIn = new ByteArrayInputStream(new String("1234567890ABC{\"response\":\"success\",\"info\":\"processed: 2; failed: 0; total: 2; seconds spent: 0.000056\"}").getBytes());

		zabbixWriter.write(dataOut, dataIn, Charset.forName("UTF-8"), dummyServer(), dummyQuery(), dummyResults());

		String json = new String(dataOut.toByteArray());

		/* Zabbix Sender JSON
		{
			"request":"sender data",
			"data":[
				{"host":"host.example.net","key":"jmxtrans.discoveryRule","value":[{"{#discoveryKey}":"discoveryValue"}]},
				{"host":"host.example.net","key":"jmxtrans.MemoryAlias..ObjectPendingFinalizationCount","value":"10","clock":0},
				{"host":"host.example.net","key":"jmxtrans.VerboseMemory..Verbose","value":"true","clock":0},
				{"host":"host.example.net","key":"jmxtrans.VerboseMemory..Verbose","value":"false","clock":0}
			],
			"clock": 1381482905
		}
		*/

		assertThat(json).startsWith("ZBXD");
		// Skip header
		json = json.substring(5+4+4);
		//System.out.println(json);

		assertThatJson(json)
			.node("request").isEqualTo("sender data");
		assertThatJson(json)
			.node("data").isArray().ofLength(4);
		assertThatJson(json)
			.node("data[0].host").isEqualTo("host.example.net")
			.node("data[0].key").isEqualTo("jmxtrans.discoveryRule")
			;
		assertThatJson(json)
			.node("data[0].value").isStringEqualTo("[{\"{#discoveryKey}\":\"discoveryValue\"}]");
		assertThatJson(json)
			.node("data[1].host").isEqualTo("host.example.net")
			.node("data[1].key").isEqualTo("jmxtrans.MemoryAlias..ObjectPendingFinalizationCount")
			.node("data[1].value").isEqualTo("\"10\"")
			.node("data[1].clock").isEqualTo(0)
			;
	}

	@Test
	public void metricsAreFormattedCorrectlyDiscovery2() throws IOException {
		ZabbixWriter zabbixWriter = new ZabbixWriter(new JsonFactory(), ImmutableList.<String>of(), Boolean.TRUE, "${MBEAN}.${TYPENAMES}.${KEY}", "discoveryRule", "discoveryKey1", "discoveryValue1", "discoveryKey2", "discoveryValue2");

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		ByteArrayInputStream dataIn = new ByteArrayInputStream(new String("1234567890ABC{\"response\":\"success\",\"info\":\"processed: 2; failed: 0; total: 2; seconds spent: 0.000056\"}").getBytes());

		zabbixWriter.write(dataOut, dataIn, Charset.forName("UTF-8"), dummyServer(), dummyQuery(), dummyResults());

		String json = new String(dataOut.toByteArray());

		/* Zabbix Sender JSON
		{
			"request":"sender data",
			"data":[
				{"host":"host.example.net","key":"jmxtrans.discoveryRule","value":[{"{#discoveryKey}":"discoveryValue"}]},
				{"host":"host.example.net","key":"jmxtrans.MemoryAlias..ObjectPendingFinalizationCount","value":"10","clock":0},
				{"host":"host.example.net","key":"jmxtrans.VerboseMemory..Verbose","value":"true","clock":0},
				{"host":"host.example.net","key":"jmxtrans.VerboseMemory..Verbose","value":"false","clock":0}
			],
			"clock": 1381482905
		}
		*/

		assertThat(json).startsWith("ZBXD");
		// Skip header
		json = json.substring(5+4+4);
		//System.out.println(json);

		assertThatJson(json)
			.node("request").isEqualTo("sender data");
		assertThatJson(json)
			.node("data").isArray().ofLength(4);
		assertThatJson(json)
			.node("data[0].host").isEqualTo("host.example.net")
			.node("data[0].key").isEqualTo("jmxtrans.discoveryRule")
			;
		assertThatJson(json)
			.node("data[0].value").isStringEqualTo("[{\"{#discoveryKey1}\":\"discoveryValue1\",\"{#discoveryKey2}\":\"discoveryValue2\"}]");
		assertThatJson(json)
			.node("data[1].host").isEqualTo("host.example.net")
			.node("data[1].key").isEqualTo("jmxtrans.MemoryAlias..ObjectPendingFinalizationCount")
			.node("data[1].value").isEqualTo("\"10\"")
			.node("data[1].clock").isEqualTo(0)
			;
	}

	@Test
	public void metricsAreFormattedCorrectlyDiscoveryNoPrefix() throws IOException {
		ZabbixWriter zabbixWriter = new ZabbixWriter(new JsonFactory(), ImmutableList.<String>of(), Boolean.FALSE, "${MBEAN}.${TYPENAMES}.${KEY}", "discoveryRule", "discoveryKey", "discoveryValue", null, null);

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		ByteArrayInputStream dataIn = new ByteArrayInputStream(new String("1234567890ABC{\"response\":\"success\",\"info\":\"processed: 2; failed: 0; total: 2; seconds spent: 0.000056\"}").getBytes());

		zabbixWriter.write(dataOut, dataIn, Charset.forName("UTF-8"), dummyServer(), dummyQuery(), dummyResults());

		String json = new String(dataOut.toByteArray());

		/* Zabbix Sender JSON
		{
			"request":"sender data",
			"data":[
				{"host":"host.example.net","key":"discoveryRule","value":"{"{#discoveryKey}":"discoveryValue"}"},
				{"host":"host.example.net","key":"MemoryAlias..ObjectPendingFinalizationCount","value":"10","clock":0},
				{"host":"host.example.net","key":"Verbose..VerboseMemory","value":"true","clock":0},
				{"host":"host.example.net","key":"Verbose..VerboseMemory","value":"false","clock":0}
			],
			"clock": 1381482905
		}
		*/

		assertThat(json).startsWith("ZBXD");
		// Skip header
		json = json.substring(5+4+4);
		//System.out.println(json);

		assertThatJson(json)
			.node("request").isEqualTo("sender data");
		assertThatJson(json)
			.node("data").isArray().ofLength(4);
		assertThatJson(json)
			.node("data[0].host").isEqualTo("host.example.net")
			.node("data[0].key").isEqualTo("discoveryRule")
			;
		assertThatJson(json)
			.node("data[0].value").isStringEqualTo("[{\"{#discoveryKey}\":\"discoveryValue\"}]");
		assertThatJson(json)
			.node("data[1].host").isEqualTo("host.example.net")
			.node("data[1].key").isEqualTo("MemoryAlias..ObjectPendingFinalizationCount")
			.node("data[1].value").isEqualTo("\"10\"")
			.node("data[1].clock").isEqualTo(0)
			;
	}

	@Test
	public void metricsAreFormattedCorrectlyDiscovery3() throws IOException {
		Query query = Query.builder()
			.setObj("java.lang:type=GarbageCollector,name=*")
			.addAttr("CollectionCount")
			.addAttr("CollectionTime")
			.setTypeNames(newArrayList("name"))
			.setResultAlias("test.18901.gc")
			.build();
		Result result = new Result(
			0,
			"CollectionCount",
			"com.ibm.lang.management.internal.ExtendedGarbageCollectorMXBeanImpl",
			"java.lang",
			"test.18901.gc",
//			null,
			"type=GarbageCollector,name=global",
			ImmutableList.<String>of(),
			10
		);

		//System.out.println(query);
		//System.out.println(result);
		ZabbixWriter zabbixWriter = new ZabbixWriter(new JsonFactory(), ImmutableList.<String>of("name"), Boolean.FALSE, "${MBEAN} x ${TYPENAMES} x ${KEY}", "discoveryRule", "discoveryKey1", "discoveryValue1 ${MBEAN} x ${TYPENAMES} x ${KEY}", null, null);

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		ByteArrayInputStream dataIn = new ByteArrayInputStream(new String("1234567890ABC{\"response\":\"success\",\"info\":\"processed: 2; failed: 0; total: 2; seconds spent: 0.000056\"}").getBytes());

		zabbixWriter.write(
			dataOut,
			dataIn,
			Charset.forName("UTF-8"),
			dummyServer(),
			query,
			ImmutableList.of(result)
		);

		String json = new String(dataOut.toByteArray());

		assertThat(json).startsWith("ZBXD");
		// Skip header
		json = json.substring(5+4+4);
		//System.out.println(json);

		assertThatJson(json)
			.node("request").isEqualTo("sender data");
		assertThatJson(json)
			.node("data").isArray().ofLength(2);
		assertThatJson(json)
			.node("data[0].host").isEqualTo("host.example.net")
			.node("data[0].key").isEqualTo("discoveryRule")
			;
		assertThatJson(json)
			.node("data[0].value").isStringEqualTo("[{\"{#discoveryKey1}\":\"discoveryValue1 test.18901.gc x global x CollectionCount\"}]");
		assertThatJson(json)
			.node("data[1].host").isEqualTo("host.example.net")
			.node("data[1].key").isEqualTo("test.18901.gc x global x CollectionCount")
			.node("data[1].value").isEqualTo("\"10\"")
			.node("data[1].clock").isEqualTo(0)
			;
	}

	@Test
	public void metricsAreFormattedCorrectlyDiscovery4() throws IOException {
		Query query = Query.builder()
			.setObj("java.lang:type=GarbageCollector,name=*")
			.addAttr("CollectionCount")
			.addAttr("CollectionTime")
			.setTypeNames(newArrayList("name"))
			.setResultAlias("test.18901.gc")
			.build();
		Result result = new Result(
			0,
			"CollectionCount",
			"com.ibm.lang.management.internal.ExtendedGarbageCollectorMXBeanImpl",
			"java.lang",
			"test.18901.gc",
//			null,
			"type=GarbageCollector,name=global",
			ImmutableList.<String>of(),
			10
		);

		//System.out.println(query);
		//System.out.println(result);
		ZabbixWriter zabbixWriter = new ZabbixWriter(new JsonFactory(), ImmutableList.<String>of("name"), Boolean.FALSE, "${MBEAN} x ${TYPENAMES} x ${KEY}", "discoveryRule", "discoveryKey1", "discoveryValue1 ${MBEAN} x ${TYPENAMES} x ${KEY}", "discoveryKey2", "discoveryValue2 ${MBEAN} x ${TYPENAMES} x ${KEY}");

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		ByteArrayInputStream dataIn = new ByteArrayInputStream(new String("1234567890ABC{\"response\":\"success\",\"info\":\"processed: 2; failed: 0; total: 2; seconds spent: 0.000056\"}").getBytes());

		zabbixWriter.write(
			dataOut,
			dataIn,
			Charset.forName("UTF-8"),
			dummyServer(),
			query,
			ImmutableList.of(result)
		);

		String json = new String(dataOut.toByteArray());

		assertThat(json).startsWith("ZBXD");
		// Skip header
		json = json.substring(5+4+4);
		//System.out.println(json);

		assertThatJson(json)
			.node("request").isEqualTo("sender data");
		assertThatJson(json)
			.node("data").isArray().ofLength(2);
		assertThatJson(json)
			.node("data[0].host").isEqualTo("host.example.net")
			.node("data[0].key").isEqualTo("discoveryRule")
			;
		assertThatJson(json)
			.node("data[0].value").isStringEqualTo("[{\"{#discoveryKey1}\":\"discoveryValue1 test.18901.gc x global x CollectionCount\",\"{#discoveryKey2}\":\"discoveryValue2 test.18901.gc x global x CollectionCount\"}]");
		assertThatJson(json)
			.node("data[1].host").isEqualTo("host.example.net")
			.node("data[1].key").isEqualTo("test.18901.gc x global x CollectionCount")
			.node("data[1].value").isEqualTo("\"10\"")
			.node("data[1].clock").isEqualTo(0)
			;
	}

}
