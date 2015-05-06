package com.googlecode.jmxtrans.model.results;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CPrecisionValueTransformerTests {

	@Test
	public void valueAbovePrecisionIsNotTransformed() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(10);

		assertThat(transformed).isEqualTo(10);
	}

	@Test
	public void negativeValueAbovePrecisionIsNotTransformed() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(-10);

		assertThat(transformed).isEqualTo(-10);
	}

	@Test
	public void valueBelowPrecisionIsTransformedToZero() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(Double.MIN_VALUE);

		assertThat(transformed).isEqualTo(0);
	}

	@Test
	public void nonNumberIsReturnedUnmodified() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply("my value");

		assertThat(transformed).isEqualTo("my value");
	}

	@Test
	public void nullIsReturnedUnmodified() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(null);

		assertThat(transformed).isNull();
	}

}
