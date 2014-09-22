package com.googlecode.jmxtrans.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class WatchDirTest {

	/**
	 * Sleep time before assertions.
	 *
	 * Watch events can take some time to propagate, we need to wait a bit not to have false negatives. We could
	 * probably implement a signal / wait in the callbacks to reduce waiting to the minimum, but the readability would
	 * suffer.
	 **/
	private static final int SLEEP_DURATION = 10;

	@Rule
	public TemporaryFolder watchedDir = new TemporaryFolder();

	private WatchDir watchDir;

	@Mock
	private WatchedCallback callback;

	@Before
	public void createWatchedDir() throws IOException {
		MockitoAnnotations.initMocks(this);
		watchDir = new WatchDir(watchedDir.getRoot(), callback);
		watchDir.start();
	}

	@Test
	public void createdFileIsDetected() throws Exception {
		File toCreate = watchedDir.newFile("created");
		Thread.sleep(SLEEP_DURATION);
		verify(callback).fileAdded(toCreate);
	}

	@Test
	public void deletedFileIsDetected() throws Exception {
		File toDelete = watchedDir.newFile("toDelete");
		toDelete.delete();
		Thread.sleep(SLEEP_DURATION);
		verify(callback).fileAdded(toDelete);
		verify(callback).fileDeleted(toDelete);
	}

	@Test
	public void modifiedFileIsDetected() throws Exception {
		File toModify = watchedDir.newFile("toModify");
		modifyFile(toModify);
		Thread.sleep(SLEEP_DURATION);
		verify(callback).fileAdded(toModify);
		verify(callback, atLeastOnce()).fileModified(toModify);
	}

	@Test
	public void watchingFileAlsoWorks() throws Exception {
		File toModify = watchedDir.newFile("toModify");

		WatchDir watchFile = new WatchDir(watchedDir.getRoot(), callback);
		try {
			watchFile.start();
			modifyFile(toModify);
			Thread.sleep(SLEEP_DURATION);
			verify(callback, atLeastOnce()).fileModified(toModify);
		} finally {
			watchFile.stopService();
		}
	}

	private void modifyFile(File toModify) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(toModify);
			out.write(SLEEP_DURATION);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	@After
	public void destroyWatchedDir() throws IOException {
		watchDir.stopService();
	}

}
