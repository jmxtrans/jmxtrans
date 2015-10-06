package com.googlecode.jmxtrans.model.naming;

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Zack Radick Date: 1/20/12
 * @author Arthur Naseef Date: 02/23/2014
 */
public class KeyUtilsTests {

	@Test
	public void testGetConcatenatedTypeNameValues() {
		assertThat(KeyUtils.getConcatedTypeNameValues(null, "a=1")).isNull();
		assertThat(KeyUtils.getConcatedTypeNameValues(Collections.<String>emptyList(), "a=1")).isNull();
		assertThat(KeyUtils.getConcatedTypeNameValues(asList("x-key1-x"), "")).isEmpty();
		assertThat(KeyUtils.getConcatedTypeNameValues(asList("x-key1-x", "y-key2-y"), "")).isEmpty();
		assertThat(KeyUtils.getConcatedTypeNameValues(asList("x-key1-x"), "x-key1-x=x-value1-x")).isEqualTo("x-value1-x");
		assertThat(KeyUtils.getConcatedTypeNameValues(asList("x-key1-x"), "y-key2-y=y-value2-y")).isEmpty();
		assertThat(KeyUtils.getConcatedTypeNameValues(asList("x-key1-x", "y-key2-y"), "x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo("x-value1-x_y-value2-y");
	}
}
