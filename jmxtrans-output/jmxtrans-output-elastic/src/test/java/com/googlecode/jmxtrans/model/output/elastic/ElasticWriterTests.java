/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
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
package com.googlecode.jmxtrans.model.output.elastic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticWriterTests {

    private static final String PREFIX = "rootPrefix";

    @Mock(name = "jestClient")
	private JestClient mockClient;
	@Mock
	private DocumentResult jestResultTrue;
	@Mock
	private JestResult jestResultFalse;

	@InjectMocks
	private ElasticWriter writer = createElasticWriter();

	private Server server;
	private Query query;
	private Result result;

	@Before
	public void initializeMocks() throws IOException {
		when(jestResultFalse.isSucceeded()).thenReturn(Boolean.FALSE);
		when(jestResultTrue.isSucceeded()).thenReturn(Boolean.TRUE);
		server = Server.builder().setHost("host").setPort("123").build();
		query = Query.builder().build();
		result = new Result(1, "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));
	}

	@Test
	public void sendMessageToElastic() throws Exception {

		// return for call, does index exist
		when(mockClient.execute(isA(IndicesExists.class))).thenReturn(jestResultFalse);

		// return for call, is index created
		when(mockClient.execute(isA(PutMapping.class))).thenReturn(jestResultTrue);

		// return for call, add index entry
		when(mockClient.execute(isA(Index.class))).thenReturn(jestResultTrue);

        // creates the index if needed
        writer.start();

        writer.doWrite(server, query, ImmutableList.of(result));

        writer.stop();

        Mockito.verify(mockClient, times(4)).execute(Matchers.<Action<JestResult>>any());

	}

	@Test
	public void sendNonNumericMessageToElastic() throws Exception {
		Result resultWithNonNumericValue = new Result(1, "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)"abc"));

		writer.doWrite(server, query, ImmutableList.of(resultWithNonNumericValue));

		// only one call is expected: the index check. No write is being made with non-numeric values.
		Mockito.verify(mockClient, times(0)).execute(Matchers.<Action<JestResult>>any());
	}

	@Test(expected = IOException.class)
	public void sendMessageToElasticWriteThrowsException() throws Exception {

		// return for call, is index created
		when(mockClient.execute(isA(Action.class))).thenThrow(new IOException("Failed to add index entry to elastic."));

		writer.doWrite(server, query, ImmutableList.of(result));

		// only one call is expected: the insert index entry. No write is being made with non-numeric values.
		Mockito.verify(mockClient, times(1)).execute(Matchers.<Action<JestResult>>any());
	}

	@Test(expected = ElasticWriterException.class)
	public void sendMessageToElasticWriteResultNotSucceeded() throws Exception {

		// return for call, is index created
		when(mockClient.execute(isA(Action.class))).thenReturn(jestResultFalse);
		when(jestResultFalse.getErrorMessage()).thenReturn("Failed to add index entry to elastic.");

		writer.doWrite(server, query, ImmutableList.of(result));

		// only one call is expected: the insert index entry.
		Mockito.verify(mockClient, times(1)).execute(Matchers.<Action<JestResult>>any());
	}

	@Test
    public void sendMessageToElasticAndVerify() throws Exception {

        String host = "myHost";
        String port = "56677";

		String serverAlias = "myAlias";
		Server serverWithKnownValues = Server.builder().setHost(host).setPort(port).setAlias(serverAlias).build();

        int epoch = 1123455;
        String attributeName = "attributeName123";
        String className = "className123";
        String objDomain = "objDomain123";
        String classNameAlias = "classNameAlias123";
        String typeName = "typeName123";
        String key = "myKey";
        int value = 1122;

        Result resultWithKnownValues = new Result(epoch, attributeName, className, objDomain, classNameAlias, typeName, ImmutableMap.of(key, (Object) value));

        ArgumentCaptor<Index> argument = ArgumentCaptor.forClass(Index.class);

		// return for call, add index entry
		when(mockClient.execute(isA(Index.class))).thenReturn(jestResultTrue);

        writer.doWrite(serverWithKnownValues, query, ImmutableList.of(resultWithKnownValues));

        verify(mockClient).execute(argument.capture());
        assertEquals(PREFIX + "_jmx-entries", argument.getValue().getIndex());

        Gson gson = new Gson();
        String data = argument.getValue().getData(gson);
        assertTrue("Contains host", data.contains(host));
        assertTrue("Contains port", data.contains(port));
        assertTrue("Contains attribute name", data.contains(attributeName));
        assertTrue("Contains class name", data.contains(className));
        assertTrue("Contains object domain", data.contains(objDomain));
        assertTrue("Contains classNameAlias", data.contains(classNameAlias));
        assertTrue("Contains type name", data.contains(typeName));
        assertTrue("Contains timestamp", data.contains(String.valueOf(epoch)));
        assertTrue("Contains key", data.contains(key));
        assertTrue("Contains value", data.contains(String.valueOf(value)));
		assertTrue("Contains serverAlias", data.contains(serverAlias));

    }

	@Test(expected = LifecycleException.class)
	public void indexCreateFailure() throws Exception {

		// return for call, does index exist
		when(mockClient.execute(isA(IndicesExists.class))).thenReturn(jestResultFalse);

        // return for call, is index created; return false
        when(mockClient.execute(isA(PutMapping.class))).thenReturn(jestResultFalse);

        // return error message
		when(jestResultFalse.getErrorMessage()).thenReturn("Unknown error creating index in elastic");

		// expected to throw an exception
		writer.start();

	}

	private ElasticWriter createElasticWriter() {
		ImmutableList<String> typenames = ImmutableList.of();
		Map<String,Object> settings = new HashMap<String,Object>();

		String connectionUrl = "http://localhost";

		ElasticWriter writer;

		try {
			writer = new ElasticWriter(typenames, true, PREFIX, true, connectionUrl, settings);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected failure to creare elastic writer for test", e);
		}
		return writer;
	}

	@Test
	public void checkToString() throws Exception {
		assertTrue(writer.toString().contains("ElasticWriter"));
	}

	@Test
	public void testValidateSetup() throws Exception {
		writer.validateSetup(server, query);
		// no exception expected
	}

}
