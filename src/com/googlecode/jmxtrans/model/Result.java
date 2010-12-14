package com.googlecode.jmxtrans.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of a query.
 * 
 * @author jon
 */
public class Result {
    private String attributeName;
    private String className;
    private String typeName;
    private String description;
    private Map<String, Object> values;

    public Result() { }

    public Result(String attributeName) {
        this.setAttributeName(attributeName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public Map<String, Object> getValues() {
        return values;
    }
    
    public void addValue(String key, Object value) {
        if (this.values == null) {
            values = new HashMap<String, Object>();
        }
        
        values.put(key, value);
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public String toString() {
        return "Result [attributeName=" + attributeName + ", className=" + className + ", typeName=" + typeName + ", description=" + description + ", values=" + values + "]";
    }

}
