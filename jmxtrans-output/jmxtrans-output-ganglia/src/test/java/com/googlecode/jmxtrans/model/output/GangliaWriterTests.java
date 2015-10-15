/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetricSlope;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("192.168.1.144", writer.getHost());
        Assert.assertEquals(GangliaWriter.DEFAULT_PORT, writer.getPort());
        Assert.assertEquals(GangliaWriter.DEFAULT_ADDRESSING_MODE.name(), writer.getAddressingMode());
        Assert.assertEquals(GangliaWriter.DEFAULT_TTL, writer.getTtl());
        Assert.assertEquals(GangliaWriter.DEFAULT_V31, writer.isV31());
        Assert.assertEquals(GangliaWriter.DEFAULT_UNITS, writer.getUnits());
        Assert.assertEquals(GangliaWriter.DEFAULT_SLOPE, writer.getSlope());
        Assert.assertEquals(GangliaWriter.DEFAULT_TMAX, writer.getTmax());
        Assert.assertEquals(GangliaWriter.DEFAULT_DMAX, writer.getDmax());
        Assert.assertEquals(GangliaWriter.DEFAULT_GROUP_NAME, writer.getGroupName());
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
        Assert.assertEquals("192.168.1.144", writer.getHost());
        Assert.assertEquals(25654, writer.getPort());
        Assert.assertEquals(GMetric.UDPAddressingMode.MULTICAST.name(), writer.getAddressingMode());
        Assert.assertEquals(4, writer.getTtl());
        Assert.assertEquals(false, writer.isV31());
        Assert.assertEquals("km/h", writer.getUnits());
        Assert.assertEquals(GMetricSlope.NEGATIVE, writer.getSlope());
        Assert.assertEquals(354, writer.getTmax());
        Assert.assertEquals(24, writer.getDmax());
        Assert.assertEquals("dummy", writer.getGroupName());
    }
}
