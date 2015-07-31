package com.googlecode.jmxtrans.util;

import com.google.common.base.Function;

import static java.lang.String.format;

public class ObjectToDouble implements Function<Object, Double> {

	@Override
	public Double apply(Object input) {
		if (input instanceof Double) return (Double) input;
		if (input instanceof Number) return ((Number) input).doubleValue();

		throw new IllegalArgumentException(format("There is no converter from [%s] to Double ", input.getClass().getName()));
	}
}
