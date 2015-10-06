package com.googlecode.jmxtrans.model.naming.typename;

import java.util.ArrayList;
import java.util.List;

public class PrependingTypeNameValuesStringBuilder extends TypeNameValuesStringBuilder {

	private final List<String> prependedTypeNames;

	public PrependingTypeNameValuesStringBuilder(String separator, List<String> prependedTypeNames) {
		super(separator);
		this.prependedTypeNames = prependedTypeNames;
	}

	@Override
	public String build(List<String> typeNames, String typeNameStr) {
		List<String> resultingTypeNames = new ArrayList<String>(prependedTypeNames);
		for (String name : typeNames) {
			if (!resultingTypeNames.contains(name)) {
				resultingTypeNames.add(name);
			}
		}
		return doBuild(resultingTypeNames, typeNameStr);
	}
}
