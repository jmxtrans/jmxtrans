package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Tests for {@link com.googlecode.jmxtrans.model.output.CloudWatchWriter}.
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
public class CloudWatchWriterTests {

	/** Test validation when no parameter is set. */
	@Test(expected = ValidationException.class)
	public void testValidationWithoutSettings() throws ValidationException {
		CloudWatchWriter writer = createCloudWatchWriter();
		Query query = Query.builder()
				.setObj("test")
				.build();
		
		Server server = Server.builder().setHost("localhost").setPort("123").build();
		writer.validateSetup(server, query);
	}

	@Test(expected = IllegalArgumentException.class)
	public void noRegionIsNotValid() throws IOException {
		BufferedReader in = Mockito.mock(BufferedReader.class);
		Mockito.when(in.readLine())
				.thenReturn("nothing interesting")
				.thenReturn("still nothing")
				.thenReturn(null);

		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		cloudWatchWriter.parseRegion(in);
	}

	@Test
	public void regionIsParsed() throws IOException {
		BufferedReader in = Mockito.mock(BufferedReader.class);
		Mockito.when(in.readLine())
				.thenReturn("nothing interesting")
				.thenReturn("region:my-region")
				.thenReturn(null);

		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		String region = cloudWatchWriter.parseRegion(in);

		Assertions.assertThat(region).isEqualTo("my-region");
	}

	@Test
	public void quotesAreIgnoredWhenParsingRegion() throws IOException {
		BufferedReader in = Mockito.mock(BufferedReader.class);
		Mockito.when(in.readLine())
				.thenReturn("region:\"my-region\"")
				.thenReturn(null);

		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		String region = cloudWatchWriter.parseRegion(in);

		Assertions.assertThat(region).isEqualTo("my-region");
	}

	@Test
	public void doubleIsConvertedToItself() {
		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		Assertions.assertThat(cloudWatchWriter.convertToDouble(1.0d)).isEqualTo(1.0d);
	}

	@Test
	public void integerIsConvertedToDouble() {
		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		Assertions.assertThat(cloudWatchWriter.convertToDouble(1)).isEqualTo(1.0d, Assertions.offset(0.01));
	}

	@Test
	public void floatIsConvertedToDouble() {
		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		Assertions.assertThat(cloudWatchWriter.convertToDouble(1f)).isEqualTo(1.0d, Assertions.offset(0.01));
	}

	@Test
	public void longIsConvertedToDouble() {
		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		Assertions.assertThat(cloudWatchWriter.convertToDouble(1L)).isEqualTo(1.0d, Assertions.offset(0.01));
	}

	@Test
	public void stringCannotBeConvertedToDouble() {
		CloudWatchWriter cloudWatchWriter = createCloudWatchWriter();
		Assertions.assertThat(cloudWatchWriter.convertToDouble("string")).isNull();
	}


	private CloudWatchWriter createCloudWatchWriter() {
		return CloudWatchWriter.builder().setNamespace("testNS").build();
	}


}
