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
package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.util.List;
import java.util.Map;

/**
 * This class isn't finished yet.
 *
 * @author jon
 */
public class VelocityWriter extends BaseOutputWriter {

	@JsonCreator
	public VelocityWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) {
		// TODO Auto-generated method stub
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
		// TODO
	}

	/**
	 * Uses velocity to generate output for a List of JmxProcess.
	 */
	protected void genVelocityOutput(List<JmxProcess> processes) {

		// Generate settings XML using Velocity

		// for (JmxProcess process : processes) {
		// for (Server server : process.getServers()) {

		// VelocityEngine ve = getVelocityEngine();
		// VelocityContext context = new VelocityContext();
		// context.put("results", );
		// StringWriter writer = new StringWriter();
		// ve.mergeTemplate("conf.zend/crawler_settings.xml", "UTF-8", context,
		// writer);
		// }
		// }
	}

	/**
	 * Sets velocity up to load resources from a list of paths.
	 */
	protected VelocityEngine getVelocityEngine(List<String> paths) {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
		ve.setProperty("cp.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		ve.setProperty("cp.resource.loader.cache", "true");
		ve.setProperty("cp.resource.loader.path", StringUtils.join(paths, ","));
		ve.setProperty("cp.resource.loader.modificationCheckInterval ", "10");
		ve.setProperty("input.encoding", "UTF-8");
		ve.setProperty("output.encoding", "UTF-8");
		ve.setProperty("runtime.log", "");
		return ve;
	}
}
