package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

public class KeyValueResultSerializerTest {

	@Test
	public void serialize() {
		KeyValueResultSerializer serializer = KeyValueResultSerializer.createDefault(ImmutableList.<String>of());
		String s = serializer.serialize(dummyServer(), dummyQuery(), numericResult());
		assertThat(s).isEqualTo("MemoryAlias.ObjectPendingFinalizationCount=10");
	}

}
