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
import io.searchbox.core.Index;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticWriterTests {

    private static final String PREFIX = "rootPrefix";
    @Mock
	private JestClient mockClient;
	@Mock
	private JestResult jestResultTrue;
	@Mock
	private JestResult jestResultFalse;

	@Before
	public void initializeMocks() {
		when(jestResultFalse.isSucceeded()).thenReturn(Boolean.FALSE);
		when(jestResultTrue.isSucceeded()).thenReturn(Boolean.TRUE);
	}

	@Test
	public void sendMessageToElastic() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(1, "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		ElasticWriter writer =  createElasticWriter();

		// return for call, does index exist
		when(mockClient.execute(Matchers.isA(IndicesExists.class))).thenReturn(jestResultFalse);

		// return for call, is index created
		when(mockClient.execute(Matchers.isA(PutMapping.class))).thenReturn(jestResultTrue);

        ArgumentCaptor<Index> argument = ArgumentCaptor.forClass(Index.class);

        // client for testing
        writer.setJestClient(mockClient);

        // creates the index if needed
        writer.start();

        writer.doWrite(server, query, ImmutableList.of(result));

        writer.stop();

        Mockito.verify(mockClient, times(4)).execute(Matchers.<Action<JestResult>>any());

	}

    @Test
    public void sendMessageToElasticAndVerify() throws Exception {

        String host = "myHost";
        String port = "56677";

        Server server = Server.builder().setHost(host).setPort(port).build();
        Query query = Query.builder().build();

        int epoch = 1123455;
        String attributeName = "attributeName123";
        String className = "className123";
        String objDomain = "objDomain123";
        String classNameAlias = "classNameAlias123";
        String typeName = "typeName123";
        String key = "myKey";
        int value = 1122;

        Result result = new Result(epoch, attributeName, className, objDomain, classNameAlias, typeName, ImmutableMap.of(key, (Object) value));

        ElasticWriter writer =  createElasticWriter();

        ArgumentCaptor<Index> argument = ArgumentCaptor.forClass(Index.class);

        // client for testing
        writer.setJestClient(mockClient);

        writer.doWrite(server, query, ImmutableList.of(result));

        verify(mockClient).execute(argument.capture());
        assertEquals(PREFIX + "_jmx-entries", argument.getValue().getIndex());

        // some values are not included, if these are necessary we should include them
        Gson gson = new Gson();
        String data = argument.getValue().getData(gson);
        assertTrue("Contains host", data.contains(host));
        assertTrue("Contains port", data.contains(port));
        assertTrue("Contains attribute name", data.contains(attributeName));
        //assertTrue("Contains class name", data.contains(className));
        //assertTrue("Contains object domain", data.contains(objDomain));
        assertTrue("Contains classNameAlias", data.contains(classNameAlias));
        //assertTrue("Contains type name", data.contains(typeName));
        assertTrue("Contains timestamp", data.contains(String.valueOf(epoch)));
        assertTrue("Contains key", data.contains(key));
        assertTrue("Contains value", data.contains(String.valueOf(value)));

    }

    @Test(expected = LifecycleException.class)
	public void indexCreateFailure() throws Exception {

		ElasticWriter writer = createElasticWriter();

		// return for call, does index exist
		when(mockClient.execute(Matchers.isA(IndicesExists.class))).thenReturn(jestResultFalse);

        // return for call, is index created; return false
        when(mockClient.execute(Matchers.isA(PutMapping.class))).thenReturn(jestResultFalse);

        // return error message
		when(jestResultFalse.getErrorMessage()).thenReturn("Unknown error creating index in elastic");

		// client for testing
		writer.setJestClient(mockClient);

		// creates the index if needed
		writer.start();

	}

	private ElasticWriter createElasticWriter() throws IOException {
		ImmutableList<String> typenames = ImmutableList.of();
		Map<String,Object> settings = new HashMap<String,Object>();

		String connectionUrl = "http://localhost";

		return new ElasticWriter(typenames, true, PREFIX, true, connectionUrl, settings);
	}

}
