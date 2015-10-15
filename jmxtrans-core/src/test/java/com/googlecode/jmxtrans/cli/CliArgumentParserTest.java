/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.cli;

import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class CliArgumentParserTest {

	@Rule
	public TemporaryFolder mockConfigurationDirectory = new TemporaryFolder();
	public File mockConfigurationFile;

	@Before
	public void createMockConfigurationFile() throws IOException {
		mockConfigurationFile = mockConfigurationDirectory.newFile("config.json");
	}

	@Test
	public void noExceptionThrownWhenHelpIsAsked() throws OptionsException, ParseException {
		parseConfiguration(new String[]{"-h"});
	}

	@Test(expected = OptionsException.class)
	public void jsonDirectoryOrJsonFileIsRequired() throws OptionsException, ParseException {
		try {
			parseConfiguration(new String[]{""});
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), is("Please specify either the -f or -j option."));
			throw oe;
		}
	}

	@Test(expected = OptionsException.class)
	@Ignore("Waiting for clarification of specs. Current behavior is to ignore the first option given, " +
			"probably not what is expected by users.")
	public void cannotGiveBothJsonFileAndJsonDir() throws OptionsException, ParseException {
		try {
			parseConfiguration(new String[]{
					"-f", mockConfigurationFile.getAbsolutePath(),
					"-j", mockConfigurationDirectory.getRoot().getAbsolutePath()
			});
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), is("You cannot give both a JSON file and a directory for configuration"));
			throw oe;
		}
	}

	@Test
	public void continueOnJsonErrorIsFalseByDefault() throws OptionsException, ParseException {
		JmxTransConfiguration configuration = parseConfiguration(requiredOptions());
		assertThat(configuration.isContinueOnJsonError(), is(false));
	}

	@Test
	public void continueOnJsonErrorIsCanBeSetToTrueOrFalse() throws OptionsException, ParseException {
		JmxTransConfiguration configuration = parseConfiguration(requiredOptionsAnd("-c", "true"));
		assertThat(configuration.isContinueOnJsonError(), is(true));

		configuration = parseConfiguration(requiredOptionsAnd("-c", "false"));
		assertThat(configuration.isContinueOnJsonError(), is(false));
	}

	@Test(expected = OptionsException.class)
	public void jsonConfigDirectoryCannotBeAFile() throws OptionsException, ParseException {
		try {
			parseConfiguration(new String[]{
					"-j", mockConfigurationFile.getAbsolutePath()
			});
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Path to json directory is invalid"));
			throw oe;
		}
	}

	@Test(expected = OptionsException.class)
	public void jsonConfigDirectoryMustExist() throws OptionsException, ParseException {
		try {
			parseConfiguration(new String[]{
					"-j", new File(mockConfigurationDirectory.getRoot(), "non-existing").getAbsolutePath()
			});
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Path to json directory is invalid"));
			throw oe;
		}
	}

	@Test(expected = OptionsException.class)
	public void jsonConfigFileCannotBeADirectory() throws OptionsException, ParseException {
		try {
			parseConfiguration(new String[]{
					"-f", mockConfigurationDirectory.getRoot().getAbsolutePath()
			});
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Path to json file is invalid"));
			throw oe;
		}
	}

	@Test(expected = OptionsException.class)
	public void jsonConfigFileMustExist() throws OptionsException, ParseException {
		try {
			parseConfiguration(new String[]{
					"-f", new File(mockConfigurationDirectory.getRoot(), "non-existing").getAbsolutePath()
			});
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Path to json file is invalid"));
			throw oe;
		}
	}

	@Test(expected = OptionsException.class)
	public void quartzConfigFileCannotBeADirectory() throws OptionsException, ParseException {
		try {
			parseConfiguration(requiredOptionsAnd(
					"-q", mockConfigurationDirectory.getRoot().getAbsolutePath()
			));
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Could not find path to the quartz properties file"));
			throw oe;
		}
	}

	@Test(expected = OptionsException.class)
	public void quartzConfigFileMustExist() throws OptionsException, ParseException {
		try {
			parseConfiguration(requiredOptionsAnd(
					"-q", new File(mockConfigurationDirectory.getRoot(), "non-existing").getAbsolutePath()
			));
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Could not find path to the quartz properties file"));
			throw oe;
		}
	}

	@Test
	public void canParseRunInterval() throws OptionsException, ParseException {
		JmxTransConfiguration configuration = parseConfiguration(requiredOptionsAnd(
				"-s", "20"
		));
		assertThat(configuration.getRunPeriod(), is(20));
	}

	@Test(expected = OptionsException.class)
	public void runIntervalMustBeInteger() throws OptionsException, ParseException {
		try {
			parseConfiguration(requiredOptionsAnd(
					"-s", "abc"
			));
		} catch (OptionsException oe) {
			assertThat(oe.getMessage(), startsWith("Seconds between server job runs must be an integer"));
			throw oe;
		}
	}

	private String[] requiredOptionsAnd(String... args) {
		List<String> arguments = new ArrayList<String>();
		arguments.addAll(asList(requiredOptions()));
		arguments.addAll(asList(args));
		return arguments.toArray(new String[arguments.size()]);
	}

	private JmxTransConfiguration parseConfiguration(String[] args) throws OptionsException, ParseException {
		return new CliArgumentParser().parseOptions(args);
	}

	private String[] requiredOptions() {
		return new String[]{"-f", mockConfigurationFile.getAbsolutePath()};
	}
}
