package com.googlecode.jmxtrans.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Zack Radick
 *         Date: 1/20/12
 */
public class JmxUtilsTests
{
    @Test
    public void testIsNumeric()
    {
        assertFalse( JmxUtils.isNumeric(null) );
        assertTrue( JmxUtils.isNumeric("") ); // this is "true" for historical reasons
        assertFalse( JmxUtils.isNumeric("  ") );
        assertTrue( JmxUtils.isNumeric("123") );
        assertFalse( JmxUtils.isNumeric("12 3") );
        assertFalse( JmxUtils.isNumeric("ab2c") );
        assertFalse( JmxUtils.isNumeric("12-3") );
        assertTrue( JmxUtils.isNumeric("12.3") );
        assertFalse( JmxUtils.isNumeric( "12.3.3.3" ) );
        assertTrue( JmxUtils.isNumeric( ".2" ) );
        assertFalse( JmxUtils.isNumeric( "." ) );
    }
}
