package com.googlecode.jmxtrans.jmx;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JmxProcessingTests {

	public static final String MBEAN_NAME = "domain:type=SomeType";
	private MBeanServer server;

	@Captor
	private ArgumentCaptor<Query> queryCaptor;
	@Captor
	private ArgumentCaptor<ImmutableList<Result>> resultsCaptor;

	@Before
	public void startMBeanServer() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
		server = MBeanServerFactory.createMBeanServer();
		server.registerMBean(new TestBean(), new ObjectName(MBEAN_NAME));
	}

	@Test
	public void querySimpleAttribute() throws Exception {
		OutputWriter outputWriter = mock(OutputWriter.class);
		Query query = Query.builder()
				.setObj(MBEAN_NAME)
				.addAttr("DummyValue")
				.addOutputWriter(outputWriter)
				.build();

		new JmxQueryProcessor().processQuery(server, null, query);

		verify(outputWriter).doWrite(any(Server.class), queryCaptor.capture(), resultsCaptor.capture());

		assertThat(queryCaptor.getValue()).isEqualTo(query);

		List<Result> results = resultsCaptor.getValue();
		assertThat(results).hasSize(1);

		Result result = results.get(0);
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
