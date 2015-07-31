package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class LibratoWriterTest {

	@Test
	public void httpUserAgentContainsAppropriateInformation() throws MalformedURLException {
		LibratoWriter writer = new LibratoWriter(
				ImmutableList.<String>of(),
				false,
				false,
				new URL(LibratoWriter.DEFAULT_LIBRATO_API_URL),
				1000,
				"username",
				"token",
				null,
				null,
				ImmutableMap.<String, Object>of()
		);

		Assertions.assertThat(writer.httpUserAgent)
				.startsWith("jmxtrans-standalone/")
				.contains(System.getProperty("os.name"))
				.contains(System.getProperty("os.arch"))
				.contains(System.getProperty("os.version"))
				.contains(System.getProperty("java.vm.name"))
				.contains(System.getProperty("java.version"));
	}
	

}
