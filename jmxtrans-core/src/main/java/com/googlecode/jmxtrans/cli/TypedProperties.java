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

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.DefaultConverterFactory;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

class TypedProperties {

	@Nonnull
	private final Properties properties;
	private final IStringConverterFactory stringConverterFactory = new DefaultConverterFactory();

	TypedProperties(@Nonnull Properties properties) {
		this.properties = properties;
	}

	<T> T getTypedProperty(String key, Class<T> type) {
		String value = properties.getProperty(key);
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}
		return convert(key, value, type);
	}

	<T> List<T> getTypedProperties(final String key, final Class<T> type) {
		String value = properties.getProperty(key);
		if (Strings.isNullOrEmpty(value)) {
			return emptyList();
		}

		return Lists.transform(asList(value.split("\\s*,\\s*")), new Function<String, T>() {
			public T apply(String input) {
				return convert(key, input, type);
			}
		});
	}

	private <T> T convert(String key, String value, Class<T> type) {
		try {
			Class<? extends IStringConverter<T>> converterClass = stringConverterFactory.getConverter(type);
			IStringConverter<T> converter;
			try {
				converter = converterClass.getConstructor(String.class).newInstance(key);
			} catch (NoSuchMethodException e) {
				converter = converterClass.newInstance();
			}
			return converter.convert(value);
		} catch (ReflectiveOperationException e) {
			throw new ParameterException("Failed to convert " + key + " to " + type, e);
		}
	}
}
