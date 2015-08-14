package com.googlecode.jmxtrans.model.output.elastic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticWriterTests {

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

		// client for testing
		writer.setJestClient(mockClient);

		// creates the index if needed
		writer.start();

		writer.doWrite(server, query, ImmutableList.of(result));

		writer.stop();

		Mockito.verify(mockClient, atLeast(2)).execute(Matchers.<Action<JestResult>>any());

	}

	@Test
	public void indexCreateFailure() throws Exception {

		ElasticWriter writer = createElasticWriter();

		// return for call, does index exist
		when(mockClient.execute(Matchers.isA(IndicesExists.class))).thenReturn(jestResultFalse);

		// return for call, is index created: return false
		when(jestResultFalse.getErrorMessage()).thenReturn("Unknown error creating index in elastic");

		// client for testing
		writer.setJestClient(mockClient);

		// creates the index if needed
		writer.start();

		writer.stop();

		Mockito.verify(mockClient, atLeast(2)).execute(Matchers.<Action<JestResult>>any());

	}

	private ElasticWriter createElasticWriter() throws IOException {
		ImmutableList<String> typenames = ImmutableList.of();
		Map<String,Object> settings = new HashMap<String,Object>();

		String connectionUrl = "http://localhost";

		return new ElasticWriter(typenames, true, "rootPrefix", true, connectionUrl, settings);
	}

}
