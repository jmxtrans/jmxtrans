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
package com.googlecode.jmxtrans.model.naming.typename;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@EqualsAndHashCode
public class TypeNameValue {
	@Getter private String key;
	@Getter private String value;

	public TypeNameValue(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public TypeNameValue(String key) {
		this(key, "");
	}

	public static Iterable<TypeNameValue> extract(final String typeNameStr) {
		return new Iterable<TypeNameValue>(){
			@Override
			public Iterator<TypeNameValue> iterator() {
				return new TypeNameValuesIterator(typeNameStr);
			}
		};
	}

	/**
	 * Given a typeNameStr string, create a Map with every key and value in the typeNameStr.
	 * For example:
	 * <p/>
	 * typeNameStr=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * Returns a Map with the following key/value pairs (excluding the quotes):
	 * <p/>
	 * "name"  =>  "PS Eden Space"
	 * "type"  =>  "MemoryPool"
	 *
	 * @param typeNameStr the type name str
	 * @return Map<String, String> of type-name-key / value pairs.
	 */
	public static Map<String, String> extractMap(String typeNameStr) {
		if (typeNameStr == null) {
			return Collections.emptyMap();
		}

		Map<String, String> result = newHashMap();
		for (TypeNameValue typeNameValue : extract(typeNameStr)) {
			result.put(typeNameValue.getKey(), typeNameValue.getValue());
		}
		return result;
	}

	private static class TypeNameValuesIterator implements Iterator<TypeNameValue> {

		private String[] tokens;
		private int iterator;

		TypeNameValuesIterator(String typeNameStr) {
			this.tokens = typeNameStr.split(",");
			this.iterator = 0;
			skipEmpty();
		}

		@Override
		public boolean hasNext() {
			return iterator < tokens.length;
		}

		@Override
		public TypeNameValue next() {
			String[] keyVal = tokens[iterator].split("=", 2);
			TypeNameValue result;
			if (keyVal.length > 1) {
				result = new TypeNameValue(keyVal[0], keyVal[1]);
			} else {
				result = new TypeNameValue(keyVal[0]);
			}
			++iterator;
			skipEmpty();
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		private void skipEmpty() {
			while (iterator < tokens.length && tokens[iterator].isEmpty()) {
				++iterator;
			}
		}
	}
}
