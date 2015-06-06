package com.googlecode.jmxtrans.model.output;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert; 
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;

public class KafkaWriterTests {
	
	@Test
	public void KafkaWriterNotNull() throws Exception {
		Assert.assertNotNull(getTestKafkaWriter());
	}

	@Test
	public void LocalWriteShouldFail() throws Exception {
			Server server = Server.builder().setHost("host").setPort("123").build();
			Query query = Query.builder().build();
			Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));
		
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			KafkaWriter writer = getTestKafkaWriter();
			writer.setTest(true);
			writer.doWrite(server, query, ImmutableList.of(result));
			// This write should fail because there is no Kafka instance running locally
			
	}
	
	public static KafkaWriter getTestKafkaWriter() {
		Server server = Server.builder().setHost("host").setPort("123").build();
		ImmutableList typenames = ImmutableList.of();
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("zk.connect", "host:2181");
		settings.put("metadata.broker.list", "10.231.1.1:9180");
		settings.put("serializer.class", "kafka.serializer.StringEncoder");
		settings.put("debug", false);
		settings.put("booleanAsNumber", true);
		settings.put("topics", "cloudwatch_ec2");
		KafkaWriter kw = new KafkaWriter(typenames,true,"rootPrefix",true,settings);
		return kw;
	}
	
}
