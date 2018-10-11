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

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Collection;

public class FileConfiguration implements IDefaultProvider {

	@Nonnull
	private final JmxTransConfiguration configuration;

	public FileConfiguration(@Nonnull JmxTransConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getDefaultValueFor(String optionName) {
		for (Field field : JmxTransConfiguration.class.getDeclaredFields()) {
			Parameter parameterAnnot = field.getAnnotation(Parameter.class);
			if (parameterAnnot == null) {
				continue;
			}
			for (String name : parameterAnnot.names()) {
				try {
					if (name.equals(optionName)) {
						Object value = getField(field, configuration);
						return toString(value);
					}
				} catch (IllegalAccessException e) {
					throw new ParameterException("Invalid option " + optionName);
				}
			}
		}
		throw new ParameterException("Unsupported option " + optionName);
	}

	private static String toString(Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof Collection) {
			Collection collection = (Collection) value;
			if (collection.isEmpty()) {
				return null;
			}
			return Joiner.on(',').join(collection);
		}
		return value.toString();
	}

	private static Object getField(Field field, Object target) throws IllegalAccessException {
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		return field.get(target);
	}
}
