package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.model.Query;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Zack Radick
 *         Date: 1/20/12
 */
public class BaseOutputWriterTests
{
    @Test
    public void testBaseOutputWriterSettingsBoolean()
    {
        final BaseOutputWriter outputWriter =  new TestBaseOuputWriter();
        final Map<String,Object> settingsMap = new HashMap<String, Object>(  );
        outputWriter.setSettings( settingsMap );
        // Test the unset case
        assertEquals( Boolean.FALSE, outputWriter.getBooleanSetting( "bool", false ) );
        assertNull( outputWriter.getBooleanSetting( "bool", null ) );
        // Test Boolean value
        settingsMap.put( "bool", true );
        assertEquals( Boolean.TRUE, outputWriter.getBooleanSetting( "bool", null ) );
        // Test good coercion from String values
        settingsMap.put( "bool", "TRUE" );
        assertEquals( Boolean.TRUE, outputWriter.getBooleanSetting( "bool", null ) );
        settingsMap.put( "bool", "true" );
        assertEquals( Boolean.TRUE, outputWriter.getBooleanSetting( "bool", false ) );
        settingsMap.put( "bool", "false" );
        assertEquals( Boolean.FALSE, outputWriter.getBooleanSetting( "bool", true ) );
        // Test bad coercion from String values (see Boolean.valueOf())
        settingsMap.put( "bool", "Truue" );
        assertEquals( Boolean.FALSE, outputWriter.getBooleanSetting( "bool", null ) );
        settingsMap.put( "bool", "F" );
        assertEquals( Boolean.FALSE, outputWriter.getBooleanSetting( "bool", null ) );
        settingsMap.put( "bool", "1" );
        assertEquals( Boolean.FALSE, outputWriter.getBooleanSetting( "bool", null ) );
        // Test bad type for value
        settingsMap.put( "bool", 1L );
        assertNull( outputWriter.getBooleanSetting( "bool", null ) );
        // Test null value
        settingsMap.put( "bool", null );
        assertEquals( Boolean.TRUE, outputWriter.getBooleanSetting( "bool", true ) );
    }
    
    @Test
    public void testBaseOutputWriterSettingsInteger()
    {
        final BaseOutputWriter outputWriter = new TestBaseOuputWriter();
        final Map<String,Object> settingsMap = new HashMap<String, Object>(  );
        outputWriter.setSettings( settingsMap );
        // Test the unset case
        assertEquals( 1, outputWriter.getIntegerSetting( "int", 1 ).intValue() );
        assertNull( outputWriter.getIntegerSetting( "int", null ) );
        // Test Integer value
        settingsMap.put( "int", 102308 );
        assertEquals( 102308, outputWriter.getIntegerSetting( "int", 8 ).intValue() );
        // Test Double value (Number conversion)
        settingsMap.put( "int", 1023.08d );
        assertEquals( 1023, outputWriter.getIntegerSetting( "int", null ).intValue() );
        // Test good coercion from String values
        settingsMap.put( "int", "102710" );
        assertEquals( 102710, outputWriter.getIntegerSetting( "int", null ).intValue() );
        settingsMap.put( "int", "112378" );
        assertEquals( 112378, outputWriter.getIntegerSetting( "int", 9 ).intValue() );
        // Test failed coercion
        settingsMap.put( "int", "NOT_AN_INT" );
        assertNull( outputWriter.getIntegerSetting( "int", null ) );
        settingsMap.put( "int", "802.11" );
        assertNull( outputWriter.getIntegerSetting( "int", null ) );
        settingsMap.put( "int", "" );
        assertNull( outputWriter.getIntegerSetting( "int", null ) );
        // Test null value
        settingsMap.put( "int", null );
        assertEquals( 102710, outputWriter.getIntegerSetting( "int", 102710 ).intValue() );
    }
    
    @Test
    public void testBaseOutputWriterSettingsString()
    {
        final BaseOutputWriter outputWriter = new TestBaseOuputWriter();
        final Map<String,Object> settingsMap = new HashMap<String, Object>(  );
        outputWriter.setSettings( settingsMap );
        // Test the unset case
        assertEquals( "NOT_SET", outputWriter.getStringSetting( "str", "NOT_SET" ) );
        assertEquals( null, outputWriter.getStringSetting( "str", null ) );
        // Test String value
        settingsMap.put( "str", "GOOD" );
        assertEquals( "GOOD", outputWriter.getStringSetting( "str", null ) );
        // Test non-String value coercion
        settingsMap.put( "str", 1L );
        assertEquals( "1", outputWriter.getStringSetting( "str", null ) );
        // Test null value
        settingsMap.put( "str", null );
        assertEquals( "MISSING", outputWriter.getStringSetting( "str", "MISSING" ) );
    }
    
    private class TestBaseOuputWriter
        extends BaseOutputWriter
    {
        @Override
        public void doWrite( Query query )
            throws Exception
        {
            throw new UnsupportedOperationException( "doWrite() not implemented for TestBaseOutputWriter." );
        }

        @Override
        public void validateSetup( Query query )
            throws ValidationException
        {
            throw new UnsupportedOperationException( "validateSetup() not implemented for TestBaseOutputWriter." );
        }
    }
}
