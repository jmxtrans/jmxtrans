package com.googlecode.jmxtrans.jmx;

import javax.annotation.Nullable;

public class IdentityValueTransformer implements ValueTransformer {
	@Nullable
	@Override
	public Object apply(@Nullable Object value) {
		return value;
	}
}
