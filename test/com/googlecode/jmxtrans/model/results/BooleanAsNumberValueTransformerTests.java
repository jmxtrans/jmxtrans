package com.googlecode.jmxtrans.model.results;

import org.junit.Test;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

public class BooleanAsNumberValueTransformerTests {

	@Test
	public void integerIsNotModified() {
		Integer in = 10;
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isSameAs(in);
	}

	@Test
	public void stringIsNotModified() {
		String in = "";
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isSameAs(in);
	}

	@Test
	public void trueIsConvertedToNumber() {
		Boolean in = TRUE;
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isEqualTo(0);
	}

	@Test
	public void falseIsConvertedToNumber() {
		Boolean in = FALSE;
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isEqualTo(1);
	}

}
