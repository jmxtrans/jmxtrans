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
package com.googlecode.jmxtrans.webapp;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JCommanderArgumentParser;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Servlet listener used to start and stop JMXTrans
 */
public class JmxTransContextListener implements ServletContextListener {
	private static final Logger log = LoggerFactory.getLogger(JmxTransContextListener.class);

	private JmxTransformer jmxTransformer;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		try {
			JmxTransConfiguration configuration = loadConfiguration(servletContextEvent);
			jmxTransformer = JmxTransformer.create(configuration);
			jmxTransformer.start();
			log.info("JMXTrans is running");
		} catch (IOException | LifecycleException e) {
			throw new JmxTransContextException("Failed to initialize JMXTrans context", e);
		}
	}

	private JmxTransConfiguration loadConfiguration(ServletContextEvent servletContextEvent) throws IOException {
		String configFile = System.getProperty("jmxtrans.config.file");
		if (configFile == null) {
			configFile = servletContextEvent.getServletContext().getInitParameter("configFile");
		}
		if (configFile == null) {
			configFile = "/etc/jmxtrans/jmxtrans.properties";
		}
		log.info("Starting JMXTrans with config file {}", configFile);
		Properties configProps = new Properties();
		try (InputStream configIS = new FileInputStream(configFile)) {
			configProps.load(configIS);
		}
		List<String> args = new ArrayList<>();
		args.add("--config");
		args.add(configFile);
		String jsonDir = configProps.getProperty("json.dir");
		if (jsonDir != null) {
			args.add("--json-directory");
			args.add(jsonDir);
		}
		String jsonFile = configProps.getProperty("json.file");
		if (jsonFile != null) {
			args.add("--json-file");
			args.add(jsonFile);
		}
		return new JCommanderArgumentParser().parseOptions(args.toArray(new String[0]));
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		if (jmxTransformer != null) {
			try {
				log.info("Stopping JMXTrans");
				jmxTransformer.stop();
			} catch (LifecycleException e) {
				throw new JmxTransContextException("Failed to destroy JMXTrans context", e);
			}
		}
	}
}
