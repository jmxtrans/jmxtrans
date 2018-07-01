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
package com.googlecode.jmxtrans.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.googlecode.jmxtrans.exceptions.LifecycleException;

import java.util.Map;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

import javax.annotation.Nonnull;

/**
 * Interface which defines a writer for taking jmx data and writing it out in
 * whatever format you want.
 *
 * Note that this class uses a feature of Jackson to serialize anything that
 * implements this as a "@class". That way, when Jackson deserializes
 * implementations of this interface, it is done with new objects that implement
 * this interface.
 *
 * @author jon
 */
@JsonSerialize(include = NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface OutputWriter extends AutoCloseable {

	void start() throws LifecycleException;

	@Override
	void close() throws LifecycleException;

	void doWrite(@Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws Exception;

	/**
	 * Settings allow you to configure your Writers with whatever they might
	 * need.
	 * @deprecated Don't use the settings Map, please extract necessary bits at construction time.
	 */
	@Deprecated
	Map<String, Object> getSettings();

	/**
	 * This is run when the object is instantiated. You want to get the settings
	 * and validate them.
	 */
	void validateSetup(Server server, Query query) throws ValidationException;

}
