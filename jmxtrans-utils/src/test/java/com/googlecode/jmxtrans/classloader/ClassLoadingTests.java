package com.googlecode.jmxtrans.classloader;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class ClassLoadingTests {

	@Test(expected = ClassNotFoundException.class)
	public void classCannotBeAccessedIfJarIsNotLoaded() throws ClassNotFoundException {
		Class.forName("dummy.Dummy");
	}

	/**
	 * This test modify the class loader. This makes it hard to isolate, so let's just run it manually.
	 */
	@Test
	@Ignore("Manual test")
	public void loadedJarCanBeAccessed() throws ClassNotFoundException, MalformedURLException, FileNotFoundException, URISyntaxException {
		File jarFile = new File(ClassLoadingTests.class.getResource("/dummy.jar").toURI());

		new ClassLoaderEnricher().add(jarFile);
		Class.forName("dummy.Dummy");
	}

	@Test(expected = FileNotFoundException.class)
	public void loadingNonExistingFileThrowsException() throws MalformedURLException, FileNotFoundException {
		new ClassLoaderEnricher().add(new File("/nonexising"));
	}

}
