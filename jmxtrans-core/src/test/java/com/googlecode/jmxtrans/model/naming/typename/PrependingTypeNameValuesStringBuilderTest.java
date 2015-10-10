package com.googlecode.jmxtrans.model.naming.typename;

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PrependingTypeNameValuesStringBuilderTest {

	@Test
	public void testBuilds() {
		PrependingTypeNameValuesStringBuilder builder = new PrependingTypeNameValuesStringBuilder(
				TypeNameValuesStringBuilder.DEFAULT_SEPARATOR, asList("p-key1-p", "q-key1-q"));
		assertThat(builder.build(null, "p-key1-p=p-value1-p,q-key1-q=q-value1-q")).isEqualTo("p-value1-p_q-value1-q");
		assertThat(builder.build(Collections.<String>emptyList(), "p-key1-p=p-value1-p")).isEqualTo("p-value1-p");
		assertThat(builder.build(asList("x-key1-x"), "")).isEmpty();
		assertThat(builder.build(asList("x-key1-x"), "p-key1-p=p-value1-p")).isEqualTo("p-value1-p");
		assertThat(builder.build(asList("x-key1-x", "y-key2-y"), "")).isEmpty();
		assertThat(builder.build(asList("x-key1-x"), "x-key1-x=x-value1-x")).isEqualTo("x-value1-x");
		assertThat(builder.build(asList("x-key1-x"), "x-key1-x=x-value1-x,q-key1-q=q-value1-q"))
				.isEqualTo("q-value1-q_x-value1-x");
		assertThat(builder.build(asList("x-key1-x"), "y-key2-y=y-value2-y")).isEmpty();
		assertThat(builder.build(asList("x-key1-x", "y-key2-y"), "x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo("x-value1-x_y-value2-y");
		assertThat(builder.build(
					asList("x-key1-x", "y-key2-y"),
					"x-key1-x=x-value1-x,q-key1-q=q-value1-q,y-key2-y=y-value2-y,p-key1-p=p-value1-p"))
				.isEqualTo("p-value1-p_q-value1-q_x-value1-x_y-value2-y");
	}
}
