package com.googlecode.jmxtrans.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WatchDirTest {

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
        Thread.sleep(1);
        Mockito.verify(callback).fileAdded(toCreate);
    }

    @Test
    public void deletedFileIsDetected() throws Exception {
        File toDelete = watchedDir.newFile("toDelete");
        toDelete.delete();
        Thread.sleep(1);
        Mockito.verify(callback).fileAdded(toDelete);
        Mockito.verify(callback).fileDeleted(toDelete);
    }

    @Test
    public void modifiedFileIsDetected() throws Exception {
        File toModify = watchedDir.newFile("toModify");
        OutputStream out = null;
        try {
            out = new FileOutputStream(toModify);
            out.write(1);
        } finally {
            if (out != null) {
                out.close();
            }
        }
        Thread.sleep(1);
        Mockito.verify(callback).fileAdded(toModify);
        Mockito.verify(callback, Mockito.atLeastOnce()).fileModified(toModify);
    }

    @After
    public void destroyWatchedDir() throws IOException {
        watchDir.stopService();
    }

}
