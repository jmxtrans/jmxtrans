package com.googlecode.jmxtrans.jmx;

import javax.management.Attribute;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularDataSupport;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;

public class JmxResultProcessor {

	private final Query query;
	private final ObjectInstance objectInstance;
	private final String className;
	private final List<Attribute> attributes;

	public JmxResultProcessor(Query query, ObjectInstance objectInstance, List<Attribute> attributes, String className) {
		this.query = query;
		this.objectInstance = objectInstance;
		this.className = className;
		this.attributes = attributes;
	}

	public List<Result> getResults() {
		List<Result> accumulator = new ArrayList<Result>();
		for (Attribute attribute : attributes) {
			getResult(accumulator, attribute);
		}
		return accumulator;
	}

	/**
	 * Used when the object is effectively a java type
	 */
	private void getResult(List<Result> accumulator, Attribute attribute) {
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
			Result r = getNewResultObject(attribute.getName());
			for (ObjectName obj : (ObjectName[]) value) {
				r.addValue(obj.getCanonicalName(), obj.getKeyPropertyListString());
			}
			accumulator.add(r);
		} else if (value.getClass().isArray()) {
			// OMFG: this is nutty. some of the items in the array can be
			// primitive! great interview question!
			Result r = getNewResultObject(attribute.getName());
			for (int i = 0; i < Array.getLength(value); i++) {
				Object val = Array.get(value, i);
				r.addValue(attribute.getName() + "." + i, val);
			}
			accumulator.add(r);
		} else if (value instanceof TabularDataSupport) {
			TabularDataSupport tds = (TabularDataSupport) value;
			Result r = getNewResultObject(attribute.getName());
			processTabularDataSupport(accumulator, attribute.getName(), tds);
			accumulator.add(r);
		} else {
			Result r = getNewResultObject(attribute.getName());
			r.addValue(attribute.getName(), value);
			accumulator.add(r);
		}
	}

	/**
	 * Populates the Result objects. This is a recursive function. Query
	 * contains the keys that we want to get the values of.
	 */
	private void getResult(List<Result> accumulator, String attributeName, CompositeData cds) {
		CompositeType t = cds.getCompositeType();

		Result r = getNewResultObject(attributeName);

		Set<String> keys = t.keySet();
		for (String key : keys) {
			Object value = cds.get(key);
			if (value instanceof TabularDataSupport) {
				TabularDataSupport tds = (TabularDataSupport) value;
				processTabularDataSupport(accumulator, attributeName + "." + key, tds);
				r.addValue(key, value);
			} else if (value instanceof CompositeDataSupport) {
				// now recursively go through everything.
				CompositeDataSupport cds2 = (CompositeDataSupport) value;
				getResult(accumulator, attributeName, cds2);
				return; // because we don't want to add to the list yet.
			} else {
				r.addValue(key, value);
			}
		}
		accumulator.add(r);
	}

	private void processTabularDataSupport(
			List<Result> accumulator, String attributeName,
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
	private Result getNewResultObject(String attributeName) {
		Result r = new Result(attributeName);
		r.setQuery(this.query);
		r.setClassName(className);
		r.setTypeName(objectInstance.getObjectName().getCanonicalKeyPropertyListString());
		return r;
	}
}
