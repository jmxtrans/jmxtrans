package com.googlecode.jmxtrans.classloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderEnricher {

	public void add(File file) throws MalformedURLException, FileNotFoundException {
		if (!file.exists()) {
			throw new FileNotFoundException("Additional jar [" + file.getAbsolutePath() + "] does not exist");
		}
		add(file.toURI().toURL());
	}

	/**
	 * Add the given URL to the system class loader.
	 *
	 * Note that this method uses reflection to change the visibility of the URLClassLoader.addURL() method. This will
	 * fail if a security manager forbids it.
	 *
	 * @param url
	 */
	public void add(URL url) {
		URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class sysClass = URLClassLoader.class;

		try {
			Method method = sysClass.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(sysLoader, new Object[]{ url });
		} catch (InvocationTargetException e) {
			throw new JarLoadingException(e);
		} catch (NoSuchMethodException e) {
			throw new JarLoadingException(e);
		} catch (IllegalAccessException e) {
			throw new JarLoadingException(e);
		}
	}

}
