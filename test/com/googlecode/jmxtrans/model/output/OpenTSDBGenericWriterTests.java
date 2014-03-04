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
import com.googlecode.jmxtrans.util.ValidationException;

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
		when(this.mockResult.getTypeName()).
			thenReturn("Type=x-type-x,Group=x-group-x,Other=x-other-x,Name=x-name-x");

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

		assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), true);
	}

	@Test
	public void	testMergedTypeNameValues2 () throws Exception {
		this.writer.addSetting("mergeTypeNamesTags", Boolean.TRUE);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), true);
	}

	@Test
	public void	testMergedTypeNameValues3 () throws Exception {
		// Verify the FALSE path.
		this.writer.addSetting("mergeTypeNamesTags", Boolean.FALSE);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), false);
	}

	@Test
	public void	testEmptyTagSetting () throws Exception {
		Map<String, String>	tagMap;

		when(this.mockResult.getValues()).thenReturn(createValueMap("X-ATT-X", "120021"));

		// Verify empty tag map.
		tagMap = new HashMap<String, String>();
		this.writer.addSetting("tags", tagMap);
		this.writer.setTypeNames(new LinkedList());

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertTrue(
			this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021 host=[^ ]*$"));
	}

	@Test
	public void	testTagSetting () throws Exception {
		Map<String, String>	tagMap;

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
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag1-x=x-tag1val-x\\b.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag2-x=x-tag2val-x\\b.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag3-x=x-tag3val-x\\b.*"));
	}

	@Test
	public void	testAddHostnameTag () throws Exception {
		this.writer.addSetting("mergeTypeNamesTags", Boolean.TRUE);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), true);
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*host=.*"));
	}

	@Test
	public void	testDontAddHostnameTag () throws Exception {
		this.writer.addSetting("addHostnameTag", false);
		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertFalse(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));
	}

	@Test
	public void	testEmptyResultValues () throws Exception {
		when(this.mockResult.getValues()).thenReturn(null);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertEquals(0, this.tvMetricLinesSent.size());
	}

	@Test
	public void	testOneValueMatchingAttribute () throws Exception {
		when(this.mockResult.getValues()).thenReturn(createValueMap("X-ATT-X", "120021"));

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertTrue(this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));
		assertFalse(this.tvMetricLinesSent.get(0).matches(".*\\btype=.*"));
	}

	@Test
	public void	testMultipleValuesWithMatchingAttribute () throws Exception {
		String	xLine;
		String	xxLine;

		when(this.mockResult.getValues()).
			thenReturn(createValueMap("X-ATT-X", "120021", "XX-ATT-XX", "210012"));

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();
		assertEquals(2, this.tvMetricLinesSent.size());

		if ( this.tvMetricLinesSent.get(0).contains("XX-ATT-XX") ) {
			xxLine = this.tvMetricLinesSent.get(0);
			xLine = this.tvMetricLinesSent.get(1);
		} else {
			xLine = this.tvMetricLinesSent.get(0);
			xxLine = this.tvMetricLinesSent.get(1);
		}

		assertTrue(xLine.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		assertTrue(xLine.matches(".*\\btype=X-ATT-X\\b.*"));

		assertTrue(xxLine.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 210012.*"));
		assertTrue(xxLine.matches(".*\\btype=XX-ATT-XX\\b.*"));
	}

	@Test
	public void	testNonNumericValue () throws Exception {
		String	xLine;
		String	xxLine;

		when(this.mockResult.getValues()).thenReturn(createValueMap("X-ATT-X", "THIS-IS-NOT-A-NUMBER"));

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertEquals(0, this.tvMetricLinesSent.size());
	}

	@Test
	public void	testJexlNaming () throws Exception {
		this.writer.addSetting("metricNamingExpression", "'xx-jexl-constant-name-xx'");

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();

		assertTrue(this.tvMetricLinesSent.get(0).matches("^xx-jexl-constant-name-xx 0 120021.*"));
	}

	@Test(expected = LifecycleException.class)
	public void	testInvalidJexlNaming () throws Exception {
		this.writer.addSetting("metricNamingExpression", "invalid expression here");

		this.writer.start();
	}

	@Test
	public void	testDebugOuptutResultString () throws Exception {
		String	xLine;
		String	xxLine;

		this.writer.addSetting("debug", true);

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();
	}

	@Test(expected = ValidationException.class)
	public void	testValidateNullHost () throws Exception {
		String	xLine;
		String	xxLine;

		this.writer.addSetting("host", null);
		this.writer.addSetting("port", 4242);

		this.writer.start();
		this.writer.validateSetup(this.mockQuery);
	}

	@Test(expected = ValidationException.class)
	public void	testValidateNullPort () throws Exception {
		String	xLine;
		String	xxLine;

		this.writer.addSetting("host", "localhost");
		this.writer.addSetting("port", null);

		this.writer.start();
		this.writer.validateSetup(this.mockQuery);
	}

	@Test
	public void	testValidateValidHostPort () throws Exception {
		String	xLine;
		String	xxLine;

		this.writer.addSetting("host", "localhost");
		this.writer.addSetting("port", 4242);

		this.writer.start();
		this.writer.validateSetup(this.mockQuery);
	}

	@Test
	public void	testDefaultHookMethods () throws Exception {
		this.writer = createMinimalWriter();

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();
	}

	@Test
	public void	testHooksCalled () throws Exception {
		this.writer.start();
		assertTrue(prepareSenderCalled);
		assertFalse(shutdownSenderCalled);
		assertFalse(startOutputCalled);
		assertFalse(finishOutputCalled);

		this.writer.doWrite(this.mockQuery);
		assertTrue(prepareSenderCalled);
		assertFalse(shutdownSenderCalled);
		assertTrue(startOutputCalled);
		assertTrue(finishOutputCalled);

		this.writer.stop();
		assertTrue(prepareSenderCalled);
		assertTrue(shutdownSenderCalled);
		assertTrue(startOutputCalled);
		assertTrue(finishOutputCalled);

	}

	protected OpenTSDBGenericWriter	createWriter () {
		OpenTSDBGenericWriter	result;

		result = new OpenTSDBGenericWriter () {
			protected void	prepareSender() throws LifecycleException {
				OpenTSDBGenericWriterTests.this.prepareSenderCalled = true;
			}
			protected void	shutdownSender() throws LifecycleException {
				OpenTSDBGenericWriterTests.this.shutdownSenderCalled = true;
			}
			protected void	startOutput() throws IOException {
				OpenTSDBGenericWriterTests.this.startOutputCalled = true;
			}
			protected void	finishOutput() throws IOException {
				OpenTSDBGenericWriterTests.this.finishOutputCalled = true;
			}
			protected boolean	getAddHostnameTagDefault () {
				return	tvAddHostnameTagDefault;
			}
			protected void	sendOutput (String metricLine) {
				OpenTSDBGenericWriterTests.this.tvMetricLinesSent.add(metricLine);
			}
		} ;

		return	result;
	}

	protected OpenTSDBGenericWriter	createMinimalWriter () {
		OpenTSDBGenericWriter	result;

		result = new OpenTSDBGenericWriter () {
			protected boolean	getAddHostnameTagDefault () {
				return	tvAddHostnameTagDefault;
			}
			protected void	sendOutput (String metricLine) {
				tvMetricLinesSent.add(metricLine);
			}
		} ;

		return	result;
	}

	protected void	validateMergedTypeNameValues (String resultString, boolean mergedInd) {
		if ( mergedInd ) {
			assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
			assertTrue(resultString.matches(".*\\btype=x-att1-x\\b.*"));
			assertTrue(resultString.matches(".*\\bTypeGroupNameMissing=x-type-x_x-group-x_x-name-x\\b.*"));
		}
		else {
			assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
			assertTrue(resultString.matches(".*\\btype=x-att1-x\\b.*"));
			assertTrue(resultString.matches(".*\\bType=x-type-x\\b.*"));
			assertTrue(resultString.matches(".*\\bGroup=x-group-x\\b.*"));
			assertTrue(resultString.matches(".*\\bName=x-name-x\\b.*"));
			assertTrue(resultString.matches(".*\\bMissing=(\\s.*|$)"));
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
