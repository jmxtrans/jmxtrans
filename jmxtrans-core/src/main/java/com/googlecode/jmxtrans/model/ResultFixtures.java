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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class ResultFixtures {
	private ResultFixtures() {}

	public static Result booleanTrueResult() {
		return new Result(
				0,
				"Verbose",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"VerboseMemory",
				"type=Memory",
				ImmutableMap.<String, Object>of("Verbose", true));
	}

	public static ImmutableList<Result> singleTrueResult() {
		return ImmutableList.of(booleanTrueResult());
	}

	public static Result booleanFalseResult() {
		return new Result(
				0,
				"Verbose",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"VerboseMemory",
				"type=Memory",
				ImmutableMap.<String, Object>of("Verbose", false));
	}

	public static Result numericResult() {
		return numericResult(10);
	}
	public static Result numericResult(Object numericValue) {
		return new Result(
				0,
				"ObjectPendingFinalizationCount",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"MemoryAlias",
				"type=Memory",
				ImmutableMap.<String, Object>of("ObjectPendingFinalizationCount", numericValue));
	}

	public static Result stringResult() {
		return stringResult("value is a string");
	}

	public static Result stringResult(String value) {
		return new Result(
			0,
			"NonHeapMemoryUsage",
			"sun.management.MemoryImpl",
			"ObjectDomainName",
			"MemoryAlias",
			"type=Memory",
			ImmutableMap.<String, Object>of("ObjectPendingFinalizationCount", value));
	}

	public static Result hashResult() {
		return new Result(
				0,
				"NonHeapMemoryUsage",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"MemoryAlias",
				"type=Memory",
				ImmutableMap.<String, Object>of("committed", 12345, "init", 23456, "max", -1, "used", 45678));
	}

	public static Result numericBelowCPrecisionResult() {
		return new Result(
				0,
				"ObjectPendingFinalizationCount",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"MemoryAlias",
				"type=Memory",
				ImmutableMap.<String, Object>of("ObjectPendingFinalizationCount", Double.MIN_VALUE));
	}

	public static Iterable<Result> singleNumericBelowCPrecisionResult() {
		return ImmutableList.of(numericBelowCPrecisionResult());
	}

	public static Result numericResultWithTypenames(String typeName) {
		return new Result(
				0,
				"ObjectPendingFinalizationCount",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"ObjectPendingFinalizationCount",
				typeName,
				ImmutableMap.<String, Object>of("ObjectPendingFinalizationCount", 10));
	}

	public static ImmutableList<Result> singleFalseResult() {
		return ImmutableList.of(booleanFalseResult());
	}

	public static ImmutableList<Result> singleNumericResult() {
		return ImmutableList.of(numericResult());
	}

	public static ImmutableList<Result> dummyResults() {
		return ImmutableList.of(
				numericResult(),
				booleanTrueResult(),
				booleanFalseResult());
	}

	public static ImmutableList<Result> singleResult(Result result) {
		return ImmutableList.of(result);
	}
}
