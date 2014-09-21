package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.util.List;

/**
 * This class isn't finished yet.
 * 
 * @author jon
 */
public class VelocityWriter extends BaseOutputWriter {

	public VelocityWriter() {
	}

	public void doWrite(Query query) throws Exception {
		// TODO Auto-generated method stub

	}

	public void validateSetup(Query query) throws ValidationException {
		// TODO
	}

	/**
	 * Uses velocity to generate output for a List of JmxProcess.
	 */
	protected void genVelocityOutput(List<JmxProcess> processes) throws Exception {

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
	protected VelocityEngine getVelocityEngine(List<String> paths) throws Exception {
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
