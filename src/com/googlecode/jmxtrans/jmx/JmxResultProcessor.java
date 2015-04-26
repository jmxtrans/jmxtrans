package com.googlecode.jmxtrans.jmx;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;

import javax.management.Attribute;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularDataSupport;
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
		Builder<Result> accumulator = ImmutableList.builder();
		for (Attribute attribute : attributes) {
			getResult(accumulator, attribute);
		}
		return accumulator.build();
	}

	/**
	 * Used when the object is effectively a java type
	 */
	private void getResult(Builder<Result> accumulator, Attribute attribute) {
		Object value = attribute.getValue();
		if (value == null) {
			return;
		}

		if (value instanceof CompositeData) {
			getResult(accumulator, attribute.getName(), (CompositeData) value);
		} else if (value instanceof CompositeData[]) {
			for (CompositeData cd : (CompositeData[]) value) {
				getResult(accumulator, attribute.getName(), cd);
			}
		} else if (value instanceof ObjectName[]) {
			Map<String, Object> values = newHashMap();
			for (ObjectName obj : (ObjectName[]) value) {
				values.put(obj.getCanonicalName(), obj.getKeyPropertyListString());
			}
			Result r = getNewResultObject(attribute.getName(), values);
			accumulator.add(r);
		} else if (value.getClass().isArray()) {
			// OMFG: this is nutty. some of the items in the array can be
			// primitive! great interview question!
			Map<String, Object> values = newHashMap();
			for (int i = 0; i < Array.getLength(value); i++) {
				Object val = Array.get(value, i);
				values.put(attribute.getName() + "." + i, val);
			}
			accumulator.add(getNewResultObject(attribute.getName(), values));
		} else if (value instanceof TabularDataSupport) {
			TabularDataSupport tds = (TabularDataSupport) value;
			Map<String, Object> values = Collections.emptyMap();
			Result r = getNewResultObject(attribute.getName(), values);
			processTabularDataSupport(accumulator, attribute.getName(), tds);
			accumulator.add(r);
		}  else if (value instanceof Map) {
			Result r = getNewResultObject(attribute.getName(), convertKeysToString((Map<Object, Object>) value));
			accumulator.add(r);
		} else {
			Map<String, Object> values = newHashMap();
			values.put(attribute.getName(), value);
			Result r = getNewResultObject(attribute.getName(), values);
			accumulator.add(r);
		}
	}

	private <K, V> ImmutableMap<String, V> convertKeysToString(Map<K, V> value) {
		ImmutableMap.Builder<String, V> values = ImmutableMap.builder();
		for (Map.Entry<K, V> entry : value.entrySet()) {
			values.put(entry.getKey().toString(), entry.getValue());
		}
		return values.build();
	}

	/**
	 * Populates the Result objects. This is a recursive function. Query
	 * contains the keys that we want to get the values of.
	 */
	private void getResult(Builder<Result> accumulator, String attributeName, CompositeData cds) {
		CompositeType t = cds.getCompositeType();

		Map<String, Object> values = newHashMap();

		Set<String> keys = t.keySet();
		for (String key : keys) {
			Object value = cds.get(key);
			if (value instanceof TabularDataSupport) {
				TabularDataSupport tds = (TabularDataSupport) value;
				processTabularDataSupport(accumulator, attributeName + "." + key, tds);
				values.put(key, value);
			} else if (value instanceof CompositeDataSupport) {
				// now recursively go through everything.
				CompositeDataSupport cds2 = (CompositeDataSupport) value;
				getResult(accumulator, attributeName, cds2);
				return; // because we don't want to add to the list yet.
			} else {
				values.put(key, value);
			}
		}
		Result r = getNewResultObject(attributeName, values);
		accumulator.add(r);
	}

	private void processTabularDataSupport(
			Builder<Result> accumulator, String attributeName,
			TabularDataSupport tds) {
		Set<Map.Entry<Object, Object>> entries = tds.entrySet();
		for (Map.Entry<Object, Object> entry : entries) {
			Object entryKeys = entry.getKey();
			if (entryKeys instanceof List) {
				// ie: attributeName=LastGcInfo.Par Survivor Space
				// i haven't seen this be smaller or larger than List<1>, but
				// might as well loop it.
				StringBuilder sb = new StringBuilder();
				for (Object entryKey : (List<?>) entryKeys) {
					sb.append(".");
					sb.append(entryKey);
				}
				String attributeName2 = sb.toString();
				Object entryValue = entry.getValue();
				if (entryValue instanceof CompositeDataSupport) {
					getResult(accumulator, attributeName + attributeName2, (CompositeDataSupport) entryValue);
				} else {
					throw new RuntimeException("!!!!!!!!!! Please file a bug: https://github.com/jmxtrans/jmxtrans/issues entryValue is: "
							+ entryValue.getClass().getCanonicalName());
				}
			} else {
				throw new RuntimeException("!!!!!!!!!! Please file a bug: https://github.com/jmxtrans/jmxtrans/issues entryKeys is: "
						+ entryKeys.getClass().getCanonicalName());
			}
		}
	}

	/**
	 * Builds up the base Result object
	 */
	private Result getNewResultObject(String attributeName, Map<String, Object> values) {
		return new Result(System.currentTimeMillis(), attributeName, className, objDomain, query.getResultAlias(), objectInstance.getObjectName().getCanonicalKeyPropertyListString(), values);
	}
}
