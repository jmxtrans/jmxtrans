package com.googlecode.jmxtrans.model.results;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityValueTransformerTests {

	@Test
	public void identityTransformerDoesNothing() {
		Object in = new Object();
		Object out = new IdentityValueTransformer().apply(in);

		assertThat(out).isSameAs(in);
	}

}
