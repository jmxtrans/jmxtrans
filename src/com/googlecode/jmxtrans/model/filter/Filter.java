package com.googlecode.jmxtrans.model.filter;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * The Interface Filter.
 * 
 * @author marcos.lois
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface Filter {
	/**
	 * Gets the class names that the filter accepts.
	 *
	 * @return the class names
	 */
	@JsonIgnore
	public List<String> getClassNames();
	
	/**
	 * Do the filter.
	 *
	 * @param obj the obj
	 * @return the object
	 */
	public Object doFilter(Object obj);
}
