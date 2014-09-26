package com.googlecode.jmxtrans.util;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
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

	/**
     * 
     */
	public WatchDir(File dir, WatchedCallback watched) throws IOException {
		this.watched = watched;
		this.keys = newHashMap();
		watchService = FileSystems.getDefault().newWatchService();
		Path watchedPath = Paths.get(dir.getAbsolutePath());
		WatchKey signalledKey = watchedPath.register(watchService, StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_DELETE, StandardWatchEventKind.ENTRY_MODIFY);
		// Store the path that we're watching, so we can later retrieve it and build a proper path to the file
		keys.put(signalledKey, watchedPath);
	}

	/** */
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
				log.debug("Watch service closed, terminating.");
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
					if (e.kind() == StandardWatchEventKind.ENTRY_CREATE) {
						Path context = (Path) e.context();
						Path fullPath = dir.resolve(context);
						watched.fileAdded(new File(fullPath.toString()));
					} else if (e.kind() == StandardWatchEventKind.ENTRY_DELETE) {
						Path context = (Path) e.context();
						Path fullPath = dir.resolve(context);
						watched.fileDeleted(new File(fullPath.toString()));
					} else if (e.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
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

	/** */
	public void stopService() throws IOException {
		watchService.close();
	}

}
