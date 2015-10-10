package com.googlecode.jmxtrans.model.naming.typename;

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class TypeNameValuesStringBuilderTest {

	@Test
	public void testBuilds() {
		TypeNameValuesStringBuilder builder = new TypeNameValuesStringBuilder();
		assertThat(builder.build(null, "a=1")).isNull();
		assertThat(builder.build(Collections.<String>emptyList(), "a=1")).isNull();
		assertThat(builder.build(asList("x-key1-x"), "")).isEmpty();
		assertThat(builder.build(asList("x-key1-x", "y-key2-y"), "")).isEmpty();
		assertThat(builder.build(asList("x-key1-x"), "x-key1-x=x-value1-x")).isEqualTo("x-value1-x");
		assertThat(builder.build(asList("x-key1-x"), "y-key2-y=y-value2-y")).isEmpty();
		assertThat(builder.build(asList("x-key1-x", "y-key2-y"), "x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo("x-value1-x_y-value2-y");
	}
}
