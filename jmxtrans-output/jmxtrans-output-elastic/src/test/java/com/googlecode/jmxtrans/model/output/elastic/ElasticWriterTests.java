package com.googlecode.jmxtrans.model.output.elastic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticWriterTests {

	@Mock
	private JestClient mockClient;
	@Mock
	private JestResult jestResultTrue;
	@Mock
	private JestResult jestResultFalse;

	@Test
	public void sendMessageToElastic() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(1, "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		ImmutableList<String> typenames = ImmutableList.of();
		Map<String,Object> settings = new HashMap<String,Object>();

		String connectionUrl = "http://localhost";

		// return for call, does index exist
		when(jestResultFalse.isSucceeded()).thenReturn(Boolean.FALSE);
        when(mockClient.execute(Matchers.<IndicesExists>isA(IndicesExists.class))).thenReturn(jestResultFalse);

        // return for call, is index created
        when(jestResultTrue.isSucceeded()).thenReturn(Boolean.TRUE);
		when(mockClient.execute(Matchers.<PutMapping>isA(PutMapping.class))).thenReturn(jestResultTrue);

		ElasticWriter writer = new ElasticWriter(typenames, true, "rootPrefix", true, connectionUrl, settings);
		// client for testing
		writer.setJestClient(mockClient);

        // creates the index if needed
        writer.start();

		writer.doWrite(server, query, ImmutableList.of(result));

		Mockito.verify(mockClient, atLeast(2)).execute(Matchers.<Action<JestResult>>any());

	}

}
