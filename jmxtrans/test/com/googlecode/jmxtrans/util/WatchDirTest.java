package com.googlecode.jmxtrans.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WatchDirTest {

	/**
	 * Wait time before assertions in milliseconds.
	 *
	 * Watch events can take some time to propagate. Synchronization is used to
	 * signal when verify operations can occur. But in case of a failed test,
	 * we will wait FILE_OPERATION_TIMEOUT before throwing an error.
	 **/
	private static final int FILE_OPERATION_TIMEOUT = 5000;

	@Rule
	public TemporaryFolder watchedDir = new TemporaryFolder();

	private WatchDir watchDir;

	private WatchedCallback callback;
	private Object synchro;

	@Before
	public void createWatchedDir() throws IOException {
		synchro = new Object();
		callback = spy(new MockWatchedCallback(synchro));
		watchDir = new WatchDir(watchedDir.getRoot(), callback);
		watchDir.start();
	}

	@Test
	public void createdFileIsDetected() throws Exception {
		File toCreate = watchedDir.newFile("created");
		waitForFileOperation();
		verify(callback).fileAdded(toCreate);
	}

	@Test
	public void deletedFileIsDetected() throws Exception {
		File toDelete = watchedDir.newFile("toDelete");
		waitForFileOperation();
		verify(callback).fileAdded(toDelete);

		toDelete.delete();
		waitForFileOperation();
		verify(callback).fileDeleted(toDelete);
	}

	@Test
	public void modifiedFileIsDetected() throws Exception {
		File toModify = watchedDir.newFile("toModify");
		waitForFileOperation();
		verify(callback).fileAdded(toModify);

		modifyFile(toModify);
		waitForFileOperation();
		verify(callback, atLeastOnce()).fileModified(toModify);
	}

	@Test(expected = IOException.class)
	public void watchingFileIsNotPossible() throws Exception {
		try {
			new WatchDir(watchedDir.newFile("toWatch"), callback);
		} catch (IOException ioe) {
			assertThat(ioe).hasMessageEndingWith("is not a directory");
			throw ioe;
		}
	}

	private void modifyFile(File toModify) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(toModify);
			out.write(1);
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

	private void waitForFileOperation() throws InterruptedException {
		synchronized (synchro) {
			synchro.wait(FILE_OPERATION_TIMEOUT);
		}
	}

	private static class MockWatchedCallback implements WatchedCallback {

		private final Object synchro;

		public MockWatchedCallback(Object synchro) {
			this.synchro = synchro;
		}

		@Override
		public void fileModified(File file) throws Exception {
			synchronized (synchro) {
				synchro.notifyAll();
			}
		}

		@Override
		public void fileDeleted(File file) throws Exception {
			synchronized (synchro) {
				synchro.notifyAll();
			}
		}

		@Override
		public void fileAdded(File file) throws Exception {
			synchronized (synchro) {
				synchro.notifyAll();
			}
		}
	}
}
