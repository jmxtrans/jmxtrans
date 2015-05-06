package com.googlecode.jmxtrans.model.results;

import javax.annotation.Nullable;

public class BooleanAsNumberValueTransformer implements ValueTransformer {

	private final Number valueForTrue;
	private final Number valueForFalse;

	public BooleanAsNumberValueTransformer(Number valueForTrue, Number valueForFalse) {
		this.valueForTrue = valueForTrue;
		this.valueForFalse = valueForFalse;
	}

	@Nullable
	@Override
	public Object apply(@Nullable Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Boolean) {
			if ((Boolean) value) {
				return valueForTrue;
			} else {
				return valueForFalse;
			}
		}
		return value;
	}
}
