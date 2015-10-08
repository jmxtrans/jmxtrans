package com.googlecode.jmxtrans.model.naming.typename;

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class UseAllTypeNameValuesStringBuilderTest {

	@Test
	public void testBuilds() {
		UseAllTypeNameValuesStringBuilder builder = new UseAllTypeNameValuesStringBuilder(
				TypeNameValuesStringBuilder.DEFAULT_SEPARATOR);
		assertThat(builder.build(null, "a=1")).isEqualTo("1");
		assertThat(builder.build(Collections.<String>emptyList(), "a=1")).isEqualTo("1");
		assertThat(builder.build(asList("x-key1-x"), "x-key1-x=x-value1-x")).isEqualTo("x-value1-x");
		assertThat(builder.build(asList("x-key1-x"), "y-key2-y=y-value2-y")).isEqualTo("y-value2-y");
		assertThat(builder.build(asList("y-key2-y", "x-key1-x"), "x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo("x-value1-x_y-value2-y");
	}
}
