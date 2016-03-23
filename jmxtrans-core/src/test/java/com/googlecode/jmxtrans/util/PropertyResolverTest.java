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
package com.googlecode.jmxtrans.util;

import com.google.common.io.Closer;
import com.googlecode.jmxtrans.test.ResetableSystemProperty;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * @author lanyonm
 */
public class PropertyResolverTest {

	private PropertyResolver propertyResolver;

	private Closer closer = Closer.create();

	@Before
	public void setSomeProperties() {

		closer.register(ResetableSystemProperty.setSystemProperty("myhost", "w2"));
		closer.register(ResetableSystemProperty.setSystemProperty("myport", "1099"));

		propertyResolver = new PropertyResolver();
	}

	@Test
	public void testProps() {
		String s1 = "${xxx} : ${yyy}";
		String s2 = propertyResolver.resolveProps(s1);
		Assert.assertEquals(s1, s2);

		s1 = "${myhost} : ${myport}";
		s2 = propertyResolver.resolveProps(s1);
		Assert.assertEquals("w2 : 1099", s2);

		s1 = "${myhost} : ${myaltport:2099}";
		s2 = propertyResolver.resolveProps(s1);
		Assert.assertEquals("w2 : 2099", s2);
		s1 = "${myhost}:2099";
		s2 = propertyResolver.resolveProps(s1);
		Assert.assertEquals("w2:2099", s2);

	}

	@After
	public void removeSystemProperties() throws IOException {
		closer.close();
	}

}
