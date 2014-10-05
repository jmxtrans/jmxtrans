package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetricSlope;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link GangliaWriter}.
 *
 * @author Zack Radick
 * @author Julien Nicoulaud <http://github.com/nicoulaj>
 */
public class GangliaWriterTests {

    /** Test validation when no parameter is set. */
    @Test(expected = ValidationException.class)
    public void testValidationWithoutSettings() throws ValidationException {
		Query test = Query.builder()
				.setObj("test")
				.build();
		new GangliaWriter(ImmutableList.<String>of(), false, Collections.<String, Object>emptyMap())
				.validateSetup(null, test);
    }

    /** Test validation when only required parameters are set. */
    @Test
    public void testValidationMinimalSettings() throws ValidationException {
		Map<String, Object> settings = newHashMap();
		settings.put(GangliaWriter.HOST, "192.168.1.144");

		GangliaWriter writer = new GangliaWriter(ImmutableList.<String>of(), false, settings);
        Query test = Query.builder()
				.setObj("test")
				.build();
		Server server = Server.builder().setHost("localhost").setPort("123").build();
		writer.validateSetup(server, test);
        assertEquals("192.168.1.144", writer.host);
        assertEquals(GangliaWriter.DEFAULT_PORT, writer.port);
        assertEquals(GangliaWriter.DEFAULT_ADDRESSING_MODE, writer.addressingMode);
        assertEquals(GangliaWriter.DEFAULT_TTL, writer.ttl);
        assertEquals(GangliaWriter.DEFAULT_V31, writer.v31);
        assertEquals(GangliaWriter.DEFAULT_UNITS, writer.units);
        assertEquals(GangliaWriter.DEFAULT_SLOPE, writer.slope);
        assertEquals(GangliaWriter.DEFAULT_TMAX, writer.tmax);
        assertEquals(GangliaWriter.DEFAULT_DMAX, writer.dmax);
        assertEquals(GangliaWriter.DEFAULT_GROUP_NAME, writer.groupName);
    }

    /** Test validation when all parameters are set. */
    @Test
    public void testValidationAllSettings() throws ValidationException {
		Map<String, Object> settings = newHashMap();
		settings.put(GangliaWriter.HOST, "192.168.1.144");
		settings.put(GangliaWriter.PORT, "25654");
		settings.put(GangliaWriter.ADDRESSING_MODE, "MULTICAST");
		settings.put(GangliaWriter.TTL, "4");
		settings.put(GangliaWriter.V31, "false");
		settings.put(GangliaWriter.UNITS, "km/h");
		settings.put(GangliaWriter.SLOPE, "NEGATIVE");
		settings.put(GangliaWriter.TMAX, "354");
		settings.put(GangliaWriter.DMAX, "24");
		settings.put(GangliaWriter.GROUP_NAME, "dummy");

		final GangliaWriter writer = new GangliaWriter(ImmutableList.<String>of(), false, settings);
        Query test = Query.builder()
				.setObj("test")
				.build();
		Server server = Server.builder().setHost("localhost").setPort("123").build();
		writer.validateSetup(server, test);
        assertEquals("192.168.1.144", writer.host);
        assertEquals(25654, writer.port);
        assertEquals(GMetric.UDPAddressingMode.MULTICAST, writer.addressingMode);
        assertEquals(4, writer.ttl);
        assertEquals(false, writer.v31);
        assertEquals("km/h", writer.units);
        assertEquals(GMetricSlope.NEGATIVE, writer.slope);
        assertEquals(354, writer.tmax);
        assertEquals(24, writer.dmax);
        assertEquals("dummy", writer.groupName);
    }
}
