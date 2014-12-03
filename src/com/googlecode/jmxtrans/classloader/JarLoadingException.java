package com.googlecode.jmxtrans.classloader;

public class JarLoadingException extends RuntimeException {
	public JarLoadingException(Exception cause) {
		super("Could not load additional jar", cause);
	}
}
