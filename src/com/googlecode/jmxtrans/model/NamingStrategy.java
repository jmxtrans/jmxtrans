package com.googlecode.jmxtrans.model;

/**
 * Strategy for naming metrics, tags, and the like given a result.
 */
public interface NamingStrategy {
	String formatName(Result result);
}
