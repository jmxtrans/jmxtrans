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
package com.googlecode.jmxtrans;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationParser {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationParser.class);

	public ImmutableList parseServers(Iterable<File> jsonFiles, boolean continueOnJsonError) throws LifecycleException {
		ImmutableList serversList = ImmutableList.of();
		for (File jsonFile : jsonFiles) {
			try {
				JmxProcess process = JsonUtils.getJmxProcess(jsonFile);
				if (log.isDebugEnabled()) {
					log.debug("Loaded file: " + jsonFile.getAbsolutePath());
				}
				serversList = mergeServerLists(serversList, process.getServers());
			} catch (Exception ex) {
				String message = "Error parsing json: " + jsonFile;
				if (continueOnJsonError) {
					// error parsing one file should not prevent the startup of JMXTrans
					log.error(message, ex);
				} else {
					throw new LifecycleException(message, ex);
				}
			}
		}
		return serversList;
	}

	/**
	 * Merges two lists of servers (and their queries). Based on the equality of
	 * both sets of objects. Public for testing purposes.
	 * @param secondList
	 * @param firstList
	 */
	// FIXME: the params for this method should be Set<Server> as there are multiple assumptions that they are unique
	@CheckReturnValue
	@VisibleForTesting
	ImmutableList<Server> mergeServerLists(List<Server> firstList, List<Server> secondList) {
		ImmutableList.Builder<Server> results = ImmutableList.builder();
		List<Server> toProcess = new ArrayList<Server>(secondList);
		for (Server firstServer : firstList) {
			if (toProcess.contains(firstServer)) {
				Server found = toProcess.get(secondList.indexOf(firstServer));
				results.add(merge(firstServer, found));
				// remove server as it is already merged
				toProcess.remove(found);
			} else {
				results.add(firstServer);
			}
		}
		// add servers from the second list that are not in the first one
		results.addAll(toProcess);
		return results.build();
	}

	private Server merge(Server firstServer, Server secondServer) {
		return Server.builder(firstServer)
				.addQueries(secondServer.getQueries())
				.build();
	}
}
