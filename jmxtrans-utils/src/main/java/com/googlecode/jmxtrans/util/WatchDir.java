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
package com.googlecode.jmxtrans.util;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Watch a directory (or tree) for changes to files.
 */
public class WatchDir extends Thread {
	private static final Logger log = LoggerFactory.getLogger(WatchDir.class);

	private final WatchService watchService;
	private final WatchedCallback watched;
	private final Map<WatchKey,Path> keys;

	public WatchDir(File dir, WatchedCallback watched) throws IOException {
		this.watched = watched;
		this.keys = newHashMap();
		watchService = FileSystems.getDefault().newWatchService();
		Path watchedPath = Paths.get(dir.getAbsolutePath());
		WatchKey signalledKey = watchedPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		// Store the path that we're watching, so we can later retrieve it and build a proper path to the file
		keys.put(signalledKey, watchedPath);
	}

	@Override
	public void run() {
		for (;;) {
			// take() will block until a file has been created/deleted
			WatchKey signalledKey;
			try {
				signalledKey = watchService.take();
			} catch (InterruptedException ix) {
				// we'll ignore being interrupted
				continue;
			} catch (ClosedWatchServiceException cwse) {
				// other thread closed watch service
				log.debug("Watch service closed, terminating.", cwse);
				break;
			}

			// get list of events from key
			List<WatchEvent<?>> list = signalledKey.pollEvents();

			// VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
			// key to be reported again by the watch service
			signalledKey.reset();

			// Retrieve the path for the files from the key created above; this is used by the
			// resolver below.
			Path dir = keys.get(signalledKey);
			try {
				for (WatchEvent<?> e : list) {
					if (e.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						Path context = (Path) e.context();
						Path fullPath = dir.resolve(context);
						watched.fileAdded(new File(fullPath.toString()));
					} else if (e.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
						Path context = (Path) e.context();
						Path fullPath = dir.resolve(context);
						watched.fileDeleted(new File(fullPath.toString()));
					} else if (e.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
						Path context = (Path) e.context();
						Path fullPath = dir.resolve(context);
						watched.fileModified(new File(fullPath.toString()));
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}
	}

	public void stopService() throws IOException {
		watchService.close();
	}

}
