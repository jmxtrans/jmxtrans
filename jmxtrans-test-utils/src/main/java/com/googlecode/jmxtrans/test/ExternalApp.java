/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
package com.googlecode.jmxtrans.test;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.junit.rules.ExternalResource;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

public class ExternalApp extends ExternalResource {

	private final String[] appArgs;
	private Process app;
	private final Class<?> appClass;
	private final Properties properties = new Properties();
	private File outputFile;

	/**
	 * @param appClass Main class to run
	 * @param args Program arguments
     */
	public ExternalApp(Class<?> appClass, String ... args) {
		this.appClass = appClass;
		this.appArgs = args;
	}

	/**
	 * Enable JMX server in app
     */
	public ExternalApp enableJmx(int jmxPort) {
		properties.put("com.sun.management.jmxremote.authenticate", "false");
		properties.put("com.sun.management.jmxremote.ssl", "false");
		properties.put("com.sun.management.jmxremote.port", Integer.toString(jmxPort));
		return this;
	}

	public ExternalApp redirectOutputToFile(File outputFile) {
		this.outputFile = outputFile;
		return this;
	}

	private String filePath(String filename) {
		URL fileUrl = getClass().getClassLoader().getResource(filename);
		try {
			return new File(fileUrl.toURI()).getAbsolutePath();
		} catch (URISyntaxException e) {
			return fileUrl.getPath();
		}
	}

	public ExternalApp enableSsl(String keyStore, String keyStorePassword) {
		if (properties.get("com.sun.management.jmxremote.port") != null) {
			properties.put("com.sun.management.jmxremote.ssl", "true");
			properties.put("com.sun.management.jmxremote.registry.ssl", "true");
		}
		//properties.put("javax.net.debug", "ssl");
		String keyStorePath = filePath(keyStore);
		properties.put("javax.net.ssl.keyStore", keyStorePath);
		properties.put("javax.net.ssl.keyStorePassword", keyStorePassword);
		properties.put("javax.net.ssl.trustStore", keyStorePath);
		properties.put("javax.net.ssl.trustStorePassword", keyStorePassword);
		return this;
	}

	@Override
	@IgnoreJRERequirement // ProcessBuilder.inheritIO() was introduced in Java 7. As this is only used in test, let's ignore it.
	protected void before() throws Throwable {
		List<String> command = new ArrayList<String>(asList("java", "-cp", getCurrentClasspath()));
		for(Map.Entry<Object, Object> property: properties.entrySet()) {
			command.add("-D"+property.getKey()+"="+property.getValue());
		}
		command.add(appClass.getCanonicalName());
		if (appArgs != null) {
			command.addAll(asList(appArgs));
		}
		ProcessBuilder processBuilder = new ProcessBuilder().command(command)
			.inheritIO();
		if (outputFile != null) {
			processBuilder.redirectOutput(outputFile);
		}
		app = processBuilder.start();
	}

	public InputStream getInputStream() {
		return app.getInputStream();
	}

	private String getCurrentClasspath() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader)cl).getURLs();

		return  Joiner.on(File.pathSeparator)
				.join(
						from(asList(urls))
								.transform(new Function<URL, String>() {
									@Nullable
									@Override
									public String apply(URL input) {
										try {
											return new File(input.toURI()).getPath();
										} catch(URISyntaxException e) {
											return input.getPath();
										}
									}
								}));
	}

	@Override
	protected void after() {
		app.destroy();
	}
}
