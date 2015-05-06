package com.googlecode.jmxtrans.model.naming;

import com.googlecode.jmxtrans.model.NamingStrategy;
import com.googlecode.jmxtrans.model.Result;

/**
 * Strategy for naming metrics, tags, and the like given a result.
 */
public class ClassAttributeNamingStrategy implements NamingStrategy {
	protected String delimiter = ".";

	public void setDelimiter(String delim) {
		this.delimiter = delim;
	}

	public String getDelimiter() {
		return this.delimiter;
	}

	@Override
	public String formatName(Result result) {
		StringBuilder formatted = new StringBuilder();
		String attName = result.getAttributeName();
		String className = result.getKeyAlias();

		if (className == null)
			className = result.getClassName();

		formatted.append(className);
		formatted.append(delimiter);
		formatted.append(attName);

		return formatted.toString();
	}
}
