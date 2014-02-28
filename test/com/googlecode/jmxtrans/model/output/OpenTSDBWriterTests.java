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

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for {@link OpenTSDBWriter}.
 *
 * TODO:
 *	- Stop using a real socket for testing purposes.  Some machines may not allow for the socket creation.
 */
public class OpenTSDBWriterTests {
	private static final Logger	LOG = LoggerFactory.getLogger(OpenTSDBWriterTests.class);

	protected OpenTSDBWriter	writer;
	protected Query			mockQuery;
	protected Result		mockResult;
	protected ServerSocket		loopbackSocket;

	@Before
	public void	setupTest () {
		this.mockQuery = mock(Query.class);
		this.mockResult = mock(Result.class);

		this.writer = new OpenTSDBWriter();

		this.startLoopbackSocket();

			//
			// Setup common mock interactions.
			//

		when(this.mockResult.getValues()).thenReturn(createValueMap("x-att1-x", "120021"));
		when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		when(this.mockResult.getTypeName()).thenReturn("Type=x-type-x,Group=x-group-x,Other=x-other-x,Name=x-name-x");

		this.writer.addSetting("typeNames", Arrays.asList(new String[] { "Type", "Group", "Name", "Missing" }));
		this.writer.addSetting("host", "localhost");
		this.writer.addSetting("port", Integer.valueOf(4242));
	}

	@After
	public void	cleanupTest () {
		this.stopLoopbackSocket();
	}

	@Test
	public void	testMergedTypeNameValues1 () throws Exception {
		List<String>	result;

		// Verify the default is the same as the TRUE path.
		this.writer.start();
		result = this.writer.resultParser(this.mockResult);
		this.writer.stop();

		validateMergedTypeNameValues(result, true);
	}

	@Test
	public void	testMergedTypeNameValues2 () throws Exception {
		List<String>	result;

		this.writer.addSetting("mergeTypeNamesTags", Boolean.TRUE);

		this.writer.start();
		result = this.writer.resultParser(this.mockResult);
		this.writer.stop();

		validateMergedTypeNameValues(result, true);
	}

	@Test
	public void	testMergedTypeNameValues3 () throws Exception {
		List<String>	result;

		// Verify the FALSE path.
		this.writer.addSetting("mergeTypeNamesTags", Boolean.FALSE);

		this.writer.start();
		result = this.writer.resultParser(this.mockResult);
		this.writer.stop();

		validateMergedTypeNameValues(result, false);
	}

	@Test
	public void	testTagSetting () throws Exception {
		List<String>		result;
		Map<String, String>	tagMap;

		when(this.mockResult.getValues()).thenReturn(createValueMap("X-ATT-X", "120021"));

		// Verify empty tag map.
		tagMap = new HashMap<String, String>();
		this.writer.addSetting("tags", tagMap);
		this.writer.setTypeNames(new LinkedList());

		this.writer.start();
		result = this.writer.resultParser(this.mockResult);
		this.writer.stop();

		assertTrue(result.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021 host=[^ ]*$"));


		// Verify tag map with multiple values.
		tagMap = new HashMap<String, String>();
		tagMap.put("x-tag1-x", "x-tag1val-x");
		tagMap.put("x-tag2-x", "x-tag2val-x");
		tagMap.put("x-tag3-x", "x-tag3val-x");
		this.writer.addSetting("tags", tagMap);

		this.writer.start();
		result = this.writer.resultParser(this.mockResult);
		this.writer.stop();

		assertTrue(result.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		assertTrue(result.get(0).matches(".*host=.*"));
		assertTrue(result.get(0).matches(".*\\bx-tag1-x=x-tag1val-x\\b.*"));
		assertTrue(result.get(0).matches(".*\\bx-tag2-x=x-tag2val-x\\b.*"));
		assertTrue(result.get(0).matches(".*\\bx-tag3-x=x-tag3val-x\\b.*"));
	}

	@Test
	public void	testEmptyResultValues () throws Exception {
		List<String>		result;

		when(this.mockResult.getValues()).thenReturn(null);

		this.writer.start();
		result = this.writer.resultParser(this.mockResult);
		this.writer.stop();

		assertEquals(0, result.size());
	}

	protected void	startLoopbackSocket() {
		try {
			this.loopbackSocket = new ServerSocket(4242);
		}
		catch ( IOException io_exc ) {
			LOG.warn("Failed to setup test server socket on port 4242", io_exc);
		}
	}

	protected void	stopLoopbackSocket() {
		try {
			if ( this.loopbackSocket != null ) {
				this.loopbackSocket.close();
				this.loopbackSocket = null;
			}
		}
		catch ( IOException io_exc ) {
			LOG.warn("Failed to close test server socket on port 4242", io_exc);
		}
	}

	protected void	validateMergedTypeNameValues (List<String> result, boolean mergedInd) {
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
