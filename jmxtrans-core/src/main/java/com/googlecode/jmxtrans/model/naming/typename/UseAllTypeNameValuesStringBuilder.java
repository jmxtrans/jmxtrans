package com.googlecode.jmxtrans.model.naming.typename;


import java.util.ArrayList;
import java.util.List;

public class UseAllTypeNameValuesStringBuilder extends TypeNameValuesStringBuilder {

	public UseAllTypeNameValuesStringBuilder(String separator) {
		super(separator);
	}

	@Override
	public String build(List<String> typeNames, String typeNameStr) {
		List<String> allTypeNames = new ArrayList<String>();
		for (TypeNameValue typeNameValue : TypeNameValue.extract(typeNameStr)){
			if (typeNameValue.getValue() != null && !typeNameValue.getValue().isEmpty()) {
				allTypeNames.add(typeNameValue.getKey());
			}
		}
		return doBuild(allTypeNames, typeNameStr);
	}
}
