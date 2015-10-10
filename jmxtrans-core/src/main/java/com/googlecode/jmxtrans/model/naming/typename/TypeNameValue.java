package com.googlecode.jmxtrans.model.naming.typename;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class TypeNameValue {
	private String key;
	private String value;

	public TypeNameValue(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public TypeNameValue(String key) {
		this(key, "");
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o.getClass() != this.getClass()) {
			return false;
		}

		if (!(o instanceof TypeNameValue)) {
			return false;
		}

		TypeNameValue other = (TypeNameValue) o;

		return new EqualsBuilder()
				.append(this.getKey(), other.getKey())
				.append(this.getValue(), other.getValue())
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(37, 89)
				.append(this.getKey())
				.append(this.getValue())
				.toHashCode();
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

		public TypeNameValuesIterator(String typeNameStr) {
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
