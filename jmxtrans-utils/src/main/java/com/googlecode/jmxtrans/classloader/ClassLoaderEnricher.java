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
