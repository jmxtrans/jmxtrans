package com.googlecode.jmxtrans.model.output;

import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetricSlope;
import org.junit.Test;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

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
		new GangliaWriter().validateSetup(null, test);
    }

    /** Test validation when only required parameters are set. */
    @Test
    public void testValidationMinimalSettings() throws ValidationException {
        GangliaWriter writer = new GangliaWriter();
        writer.addSetting(GangliaWriter.HOST, "192.168.1.144");
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
        final GangliaWriter writer = new GangliaWriter();
        writer.addSetting(GangliaWriter.HOST, "192.168.1.144");
        writer.addSetting(GangliaWriter.PORT, "25654");
        writer.addSetting(GangliaWriter.ADDRESSING_MODE, "MULTICAST");
        writer.addSetting(GangliaWriter.TTL, "4");
        writer.addSetting(GangliaWriter.V31, "false");
        writer.addSetting(GangliaWriter.UNITS, "km/h");
        writer.addSetting(GangliaWriter.SLOPE, "NEGATIVE");
        writer.addSetting(GangliaWriter.TMAX, "354");
        writer.addSetting(GangliaWriter.DMAX, "24");
        writer.addSetting(GangliaWriter.GROUP_NAME, "dummy");
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
