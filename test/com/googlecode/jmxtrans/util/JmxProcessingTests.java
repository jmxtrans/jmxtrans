package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.management.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JmxProcessingTests {

	public static final String MBEAN_NAME = "domain:type=SomeType";
	private MBeanServer server;

	@Before
	public void startMBeanServer() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
		server = MBeanServerFactory.createMBeanServer();
		server.registerMBean(new TestBean(), new ObjectName(MBEAN_NAME));
	}

	@Test
	public void querySimpleAttribute() throws Exception {
		ArgumentCaptor<Query> argument = ArgumentCaptor.forClass(Query.class);
		OutputWriter outputWriter = mock(OutputWriter.class);
		Query query = new Query(MBEAN_NAME);
		query.addAttr("DummyValue");
		query.addOutputWriter(outputWriter);

		JmxUtils.processQuery(server, query);

		verify(outputWriter).doWrite(argument.capture());

		Query processedQuery = argument.getValue();
		assertThat(processedQuery.getResults()).hasSize(1);

		Result result = processedQuery.getResults().get(0);
		assertThat(result.getValues().get("DummyValue")).isEqualTo(123);
	}



	public interface TestMXBean {
		int getDummyValue();
	}

	public class TestBean implements TestMXBean {
		@Override
		public int getDummyValue() {
			return 123;
		}
	}

}
