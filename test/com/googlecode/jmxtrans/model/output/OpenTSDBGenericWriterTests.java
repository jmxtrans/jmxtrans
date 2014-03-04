package com.googlecode.jmxtrans.model.output;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.LifecycleException;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for {@link OpenTSDBGenericWriter}.
 */
public class OpenTSDBGenericWriterTests {
	private static final Logger	LOG = LoggerFactory.getLogger(OpenTSDBGenericWriterTests.class);

	protected OpenTSDBGenericWriter	writer;
	protected Query			mockQuery;
	protected Result		mockResult;

	//
	// Interactions with the custom, test subclass of OpenTSDBGenericWriter.
	//
	protected boolean		tvAddHostnameTagDefault;
	protected boolean		prepareSenderCalled;
	protected boolean		shutdownSenderCalled;
	protected boolean		startOutputCalled;
	protected boolean		finishOutputCalled;
	protected List<String>		tvMetricLinesSent;

	@Before
	public void	setupTest () {
		this.mockQuery = mock(Query.class);
		this.mockResult = mock(Result.class);

			//
			// Setup test data
			//

		tvAddHostnameTagDefault = true;
		prepareSenderCalled = false;
		shutdownSenderCalled = false;
		startOutputCalled = false;
		finishOutputCalled = false;
		tvMetricLinesSent = new LinkedList<String>();

			// Prepare the object-under-test

		this.writer = this.createWriter();


			//
			// Setup common mock interactions.
			//

		when(this.mockResult.getValues()).thenReturn(createValueMap("x-att1-x", "120021"));
		when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		when(this.mockResult.getTypeName()).thenReturn("Type=x-type-x,Group=x-group-x,Other=x-other-x,Name=x-name-x");

		when(this.mockQuery.getResults()).thenReturn(Arrays.asList(new Result[] { this.mockResult }));

		this.writer.addSetting("typeNames", Arrays.asList(new String[] { "Type", "Group", "Name", "Missing" }));
		this.writer.addSetting("host", "localhost");
		this.writer.addSetting("port", Integer.valueOf(4242));
	}

	@After
	public void	cleanupTest () {
	}

	@Test
	public void	testMergedTypeNameValues1 () throws Exception {
		// Verify the default is the same as the TRUE path.
		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		validateMergedTypeNameValues(tvMetricLinesSent, true);
	}

	@Test
	public void	testMergedTypeNameValues2 () throws Exception {
		this.writer.addSetting("mergeTypeNamesTags", Boolean.TRUE);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		validateMergedTypeNameValues(tvMetricLinesSent, true);
	}

	@Test
	public void	testMergedTypeNameValues3 () throws Exception {
		// Verify the FALSE path.
		this.writer.addSetting("mergeTypeNamesTags", Boolean.FALSE);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		validateMergedTypeNameValues(tvMetricLinesSent, false);
	}

	@Test
	public void	testTagSetting () throws Exception {
		Map<String, String>	tagMap;

		when(this.mockResult.getValues()).thenReturn(createValueMap("X-ATT-X", "120021"));

		// Verify empty tag map.
		tagMap = new HashMap<String, String>();
		this.writer.addSetting("tags", tagMap);
		this.writer.setTypeNames(new LinkedList());

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertTrue(this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021 host=[^ ]*$"));

		this.tvMetricLinesSent.clear();

		// Verify tag map with multiple values.
		tagMap = new HashMap<String, String>();
		tagMap.put("x-tag1-x", "x-tag1val-x");
		tagMap.put("x-tag2-x", "x-tag2val-x");
		tagMap.put("x-tag3-x", "x-tag3val-x");
		this.writer.addSetting("tags", tagMap);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertTrue(this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*host=.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag1-x=x-tag1val-x\\b.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag2-x=x-tag2val-x\\b.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag3-x=x-tag3val-x\\b.*"));
	}

	@Test
	public void	testEmptyResultValues () throws Exception {
		when(this.mockResult.getValues()).thenReturn(null);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertEquals(0, this.tvMetricLinesSent.size());
	}

	protected OpenTSDBGenericWriter	createWriter () {
		OpenTSDBGenericWriter	result;

		result = new OpenTSDBGenericWriter () {
			protected void	prepareSender() throws LifecycleException {
				prepareSenderCalled = true;
			}
			protected void	shutdownSender() throws LifecycleException {
				shutdownSenderCalled = true;
			}
			protected void	startOutput() throws IOException {
				startOutputCalled = true;
			}
			protected void	finishOutput() throws IOException {
				finishOutputCalled = true;
			}
			protected boolean	getAddHostnameTagDefault () {
				return	tvAddHostnameTagDefault;
			}
			protected void	sendOutput (String metricLine) {
				tvMetricLinesSent.add(metricLine);
			}
		} ;

		return	result;
	}

	protected void	validateMergedTypeNameValues (List<String> result, boolean mergedInd) {
		LOG.info("result string = {}", result.get(0));
		if ( mergedInd ) {
			assertEquals(1, result.size());
			assertTrue(result.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
			assertTrue(result.get(0).matches(".*\\btype=x-att1-x\\b.*"));
			assertTrue(result.get(0).matches(".*\\bTypeGroupNameMissing=x-type-x_x-group-x_x-name-x\\b.*"));
		}
		else {
			assertEquals(1, result.size());
			assertTrue(result.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
			assertTrue(result.get(0).matches(".*\\btype=x-att1-x\\b.*"));
			assertTrue(result.get(0).matches(".*\\bType=x-type-x\\b.*"));
			assertTrue(result.get(0).matches(".*\\bGroup=x-group-x\\b.*"));
			assertTrue(result.get(0).matches(".*\\bName=x-name-x\\b.*"));
			assertTrue(result.get(0).matches(".*\\bMissing=(\\s.*|$)"));
		}
	}

	protected Map<String, Object>	createValueMap (Object... keysAndValues) {
		Map<String, Object>	result;
		int			iter;

		result = new HashMap<String, Object>();
		iter = 0;
		while ( iter < keysAndValues.length ) {
			if ( iter < ( keysAndValues.length - 1 ) ) {
				result.put(keysAndValues[iter].toString(), keysAndValues[iter + 1]);
				iter += 2;
			}
			else {
				result.put(keysAndValues[iter].toString(), null);
				iter++;
			}
		}

		return	result;
	}
}
