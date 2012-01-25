package com.googlecode.jmxtrans.model.output;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Zack Radick
 *         Date: 1/25/12
 */
public class GangliaWriterTests {
	
	@Test
	public void testBufferOverflowOnLongString() {
		final TestGangliaWriter writer = new TestGangliaWriter();
		final int dataSize = 99999;
		final byte[] overflow = new byte[dataSize]; // Buffer size is 1500, overflow by a large margin
		for( int i = 0; i < dataSize; i++ ) {
			overflow[i] = 'z';
		}
		writer.xdr_string( new String( overflow ) );
		final byte[] buffer = writer.getBuffer();
		// The first four bytes are the int value of the String length, which should be equal to the buffer size - 4
		int valueLength = 0;
		for( int i = 0; i < 4; i++ ) {
			valueLength += ( buffer[ i ] & 0xff ) << ( 8 * ( 3 - i ) );
		}
		assertEquals( buffer.length - 4, valueLength );
		// The remaining bytes in the buffer should be equal to the characters put into the byte array
		// with the final three characters being an ellipsis
		for( int i = 4; i < buffer.length; i++ ) {
			assertEquals( i < buffer.length - 3 ? 'z' : '.', buffer[ i ] );
		}
	}
	
	private class TestGangliaWriter extends GangliaWriter {
		public byte[] getBuffer() {
			return buffer;
		}
	}
}
