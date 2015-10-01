package com.googlecode.jmxtrans.model.naming;

import java.util.Iterator;

class TypeNameValue {
	private String key;
	private String value;

	public TypeNameValue(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public static Iterable<TypeNameValue> extract(final String typeNameStr) {
		return new Iterable<TypeNameValue>(){
			@Override
			public Iterator<TypeNameValue> iterator() {
				return new TypeNameValuesIterator(typeNameStr);
			}
		};
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
			TypeNameValue result = new TypeNameValue(keyVal[0], keyVal.length > 1 ? keyVal[1] : "");
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
