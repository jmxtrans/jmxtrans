package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetricSlope;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link GangliaWriter}.
 *
 * @author Zack Radick
 * @author Julien Nicoulaud <http://github.com/nicoulaj>
 */
public class GangliaWriterTests {

    /** Test validation when no parameter is set. */
    @Test(expected = NullPointerException.class)
    public void testValidationWithoutSettings() throws ValidationException {
		GangliaWriter.builder().build();
    }

    /** Test validation when only required parameters are set. */
    @Test
    public void testValidationMinimalSettings() throws ValidationException {
		GangliaWriter writer = GangliaWriter.builder().setHost("192.168.1.144").build();
        Query test = Query.builder()
				.setObj("test")
				.build();
		Server server = Server.builder().setHost("localhost").setPort("123").build();
		writer.validateSetup(server, test);
        assertEquals("192.168.1.144", writer.getHost());
        assertEquals(GangliaWriter.DEFAULT_PORT, writer.getPort());
        assertEquals(GangliaWriter.DEFAULT_ADDRESSING_MODE.name(), writer.getAddressingMode());
        assertEquals(GangliaWriter.DEFAULT_TTL, writer.getTtl());
        assertEquals(GangliaWriter.DEFAULT_V31, writer.isV31());
        assertEquals(GangliaWriter.DEFAULT_UNITS, writer.getUnits());
        assertEquals(GangliaWriter.DEFAULT_SLOPE, writer.getSlope());
        assertEquals(GangliaWriter.DEFAULT_TMAX, writer.getTmax());
        assertEquals(GangliaWriter.DEFAULT_DMAX, writer.getDmax());
        assertEquals(GangliaWriter.DEFAULT_GROUP_NAME, writer.getGroupName());
    }

    /** Test validation when all parameters are set. */
    @Test
    public void testValidationAllSettings() throws ValidationException {
		GangliaWriter writer = GangliaWriter.builder()
				.setHost("192.168.1.144")
				.setPort(25654)
				.setAddressingMode("MULTICAST")
				.setTtl(4)
				.setV31(false)
				.setUnits("km/h")
				.setSlope("NEGATIVE")
				.setTmax(354)
				.setDmax(24)
				.setGroupName("dummy")
				.build();

        Query test = Query.builder()
				.setObj("test")
				.build();
		Server server = Server.builder().setHost("localhost").setPort("123").build();
		writer.validateSetup(server, test);
        assertEquals("192.168.1.144", writer.getHost());
        assertEquals(25654, writer.getPort());
        assertEquals(GMetric.UDPAddressingMode.MULTICAST.name(), writer.getAddressingMode());
        assertEquals(4, writer.getTtl());
        assertEquals(false, writer.isV31());
        assertEquals("km/h", writer.getUnits());
        assertEquals(GMetricSlope.NEGATIVE, writer.getSlope());
        assertEquals(354, writer.getTmax());
        assertEquals(24, writer.getDmax());
        assertEquals("dummy", writer.getGroupName());
    }
}
