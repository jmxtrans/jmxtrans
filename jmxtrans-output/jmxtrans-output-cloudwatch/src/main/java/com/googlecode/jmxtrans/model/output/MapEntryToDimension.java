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

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static java.lang.String.format;

@ThreadSafe
class MapEntryToDimension implements Function<Map<String, Object>, Dimension> {
	private static final Logger log = LoggerFactory.getLogger(MapEntryToDimension.class);
	private static final String NAME = "name";
	private static final String VALUE = "value";

	@Nullable
	@Override
	public Dimension apply(Map<String, Object> dimension) {
		String name = null;
		String value = null;

		if (dimension.containsKey(NAME)) {
			name = String.valueOf(dimension.get(NAME));
		}
		if (dimension.containsKey(VALUE)) {
			value = String.valueOf(dimension.get(VALUE));
		}

		if (name == null || value == null) {
			throw new IllegalArgumentException(format("Incomplete dimension: Missing non-null '%s' and '%s' in '%s'", NAME, VALUE, dimension.toString()));
		}

		if (value.startsWith("$")) {
			try {
				Method m = EC2MetadataUtils.class.getMethod("get" + value.substring(1));
				value = String.valueOf(m.invoke(null));
			} catch (NoSuchMethodException e) {
				log.warn("Could not resolve {} via a getters on {}!", value, EC2MetadataUtils.class.getName(), e);
			} catch (IllegalAccessException e) {
				log.warn("Could not load {} via a getters on {}!", value, EC2MetadataUtils.class.getName(), e);
			} catch (InvocationTargetException e) {
				log.warn("Could not retrieve {} via a getters on {}!", value, EC2MetadataUtils.class.getName(), e);
			}
		}
		return new Dimension().withName(name).withValue(value);
	}
}
