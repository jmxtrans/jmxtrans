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
package com.googlecode.jmxtrans.model;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.management.Attribute;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.Builder;
import static com.google.common.collect.Maps.newHashMap;

public class JmxResultProcessor {

	private final Query query;
	private final ObjectInstance objectInstance;
	private final String className;
	private final String objDomain;
	private final List<Attribute> attributes;

	public JmxResultProcessor(Query query, ObjectInstance objectInstance, List<Attribute> attributes, String className, String objDomain) {
		this.query = query;
		this.objectInstance = objectInstance;
		this.className = className;
		this.objDomain = objDomain;
		this.attributes = attributes;
	}

	public ImmutableList<Result> getResults() {
		ResultsBuilder builder = new ResultsBuilder();
		for (Attribute attribute : attributes) {
			builder.add(attribute.getName(), attribute.getValue());
		}
		return builder.build();
	}

	/**
	 * Result list builders.
	 * Recursively walks in the value to add results.
	 */
	private class ResultsBuilder {
		private final Builder<Result> accumulator = ImmutableList.builder();
		private final long epoch = System.currentTimeMillis();

		/**
		 * Add one or more results from a value of any type.
		 * This is a recursive function.
         */
		private void add(String name, Object value) {
			if (value == null) {
				return;
			}
			if (value instanceof CompositeData) {
				add(name, (CompositeData) value);
			} else if (value instanceof CompositeData[]) {
				for (CompositeData cd : (CompositeData[]) value) {
					add(name, cd);
				}
			} else if (value instanceof ObjectName[]) {
				add(name, (ObjectName[]) value);
			} else if (value.getClass().isArray()) {
				// OMFG: this is nutty. some of the items in the array can be
				// primitive! great interview question!
				Map<String, Object> values = newHashMap();
				for (int i = 0; i < Array.getLength(value); i++) {
					Object val = Array.get(value, i);
					values.put(name + "." + i, val);
				}
				addNew(name, values);
			} else if (value instanceof TabularData) {
				add(name, (TabularData) value);
			}  else if (value instanceof Map) {
				add(name, (Map<Object, Object>) value);
			} else {
				addNew(name, Collections.singletonMap(name, value));
			}
		}

		/**
		 * Add results from a value of type map.
		 */
		private void add(String attributeName, Map<Object, Object> map) {
			ImmutableMap.Builder<String, Object> values = ImmutableMap.builder();
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				values.put(entry.getKey().toString(), entry.getValue());
			}
			addNew(attributeName, values.build());
		}

		/**
		 * Add results from a value of type objet name array.
		 */
		private void add(String attributeName, ObjectName[] objs) {
			ImmutableMap.Builder<String, Object> values = ImmutableMap.builder();
			for (ObjectName obj : objs) {
				values.put(obj.getCanonicalName(), obj.getKeyPropertyListString());
			}
			addNew(attributeName, values.build());
		}

		/**
		 * Add results from a value of type composite data.
		 * This is a recursive function.
		 */
		private void add(String attributeName, CompositeData cds) {
			CompositeType t = cds.getCompositeType();

			Map<String, Object> values = newHashMap();

			Set<String> keys = t.keySet();
			for (String key : keys) {
				Object value = cds.get(key);
				if (value instanceof TabularData) {
					TabularData tds = (TabularData) value;
					add(attributeName + "." + key, tds);
					// continue because we added tabular contents within above, but need primitives at this level
				} else if (value instanceof CompositeData) {
					// now recursively go through everything.
					CompositeData cds2 = (CompositeData) value;
					add(attributeName, cds2);
					return; // because we don't want to add to the list yet.
				} else {
					values.put(key, value);
				}
			}
			addNew(attributeName, values);
		}

		/**
		 * Add results from a value of type tabular data.
		 * This is a recursive function.
		 */
		private void add(String attributeName, TabularData tds) {
			// @see TabularData#keySet JavaDoc:
			// "Set<List<?>>" but is declared as a {@code Set<?>} for
			// compatibility reasons. The returned set can be used to iterate
			// over the keys."
			Set<List<?>> keys = (Set<List<?>>) tds.keySet();
			for (List<?> key: keys) {
				// ie: attributeName=LastGcInfo.Par Survivor Space
				// i haven't seen this be smaller or larger than List<1>, but
				// might as well loop it.
				CompositeData compositeData = tds.get(key.toArray());
				String attributeName2 = Joiner.on('.').join(key);
				add(attributeName + "." + attributeName2, compositeData);
			}
		}
		/**
		 * Create and add a new result.
		 */
		private void addNew(String attributeName, Map<String, Object> values) {
			accumulator.add(new Result(epoch, attributeName, className, objDomain, query.getResultAlias(), objectInstance.getObjectName().getKeyPropertyListString(), values));
		}

		/**
		 * Return the built list
         */
		public ImmutableList<Result> build() {
			return accumulator.build();
		}
	}

}
