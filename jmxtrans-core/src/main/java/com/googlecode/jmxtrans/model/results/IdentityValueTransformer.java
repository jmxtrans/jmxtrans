package com.googlecode.jmxtrans.model.results;

import javax.annotation.Nullable;

public class IdentityValueTransformer implements ValueTransformer {
	@Nullable
	@Override
	public Object apply(@Nullable Object input) {
		return input;
	}
}
