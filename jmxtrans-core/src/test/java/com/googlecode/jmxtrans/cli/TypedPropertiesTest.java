/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
package com.googlecode.jmxtrans.cli;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

public class TypedPropertiesTest {
	private TypedProperties typedProperties;

	@Before
	public void setUp() throws IOException {
		try (InputStream inputStream = getClass().getResourceAsStream("/example-typed.properties")) {
			Properties properties = new Properties();
			properties.load(inputStream);
			typedProperties = new TypedProperties(properties);
		}
	}

	@Test
	public void testGetString() {
		assertThat(typedProperties.getTypedProperty("string", String.class)).isEqualTo("Hello JMXTrans");
	}

	@Test
	public void testGetInteger() {
		assertThat(typedProperties.getTypedProperty("integer", Integer.class)).isEqualTo(1234);
	}

	@Test
	public void testGetBoolean() {
		assertThat(typedProperties.getTypedProperty("boolean", Boolean.class)).isTrue();
	}

	@Test
	public void testGetMulti() {
		assertThat(typedProperties.getTypedProperties("multi", String.class)).containsExactly("one", "two", "three", "four");
	}
}
