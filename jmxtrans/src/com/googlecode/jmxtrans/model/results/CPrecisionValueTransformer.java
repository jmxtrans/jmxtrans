package com.googlecode.jmxtrans.model.results;

import javax.annotation.Nullable;
import java.math.BigDecimal;

public class CPrecisionValueTransformer implements ValueTransformer {

	private static final BigDecimal C_PRECISION = new BigDecimal("1E-308");

	@Nullable
	@Override
	public Object apply(Object input) {
		if (input == null) return null;
		if (!Number.class.isAssignableFrom(input.getClass())) return input;

		BigDecimal inputNumber = new BigDecimal(input.toString());

		if (inputNumber.abs().compareTo(C_PRECISION) < 0) return 0;

		return input;
	}

}
