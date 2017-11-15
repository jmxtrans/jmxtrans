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
				ImmutableList.<String>of(),
				true);
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
				ImmutableList.<String>of(),
				false);
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
				ImmutableList.<String>of(),
				numericValue);
	}

	public static Result numericResultWithColon() {
		return numericResultWithColon(10);
	}

	public static Result numericResultWithColon(Object numericValue) {
		return new Result(
				0,
				"Count",
				"com.yammer.metrics.reporting.JmxReporter$Meter",
				"fakehostname.example.com-org.openrepose.core",
				null,
				"type=\"ResponseCode\",scope=\"127.0.0.1:8008\",name=\"4XX\"",
				ImmutableList.<String>of(),
				numericValue);
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
			ImmutableList.<String>of("ObjectPendingFinalizationCount"),
			value);
	}

	private static Result nonHeapMemoryResult(String valuePath, int value) {
		return new Result(
				0,
				"NonHeapMemoryUsage",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"MemoryAlias",
				"type=Memory",
				ImmutableList.<String>of(valuePath),
				value);
	}

	public static ImmutableList<Result> hashResults() {
		return ImmutableList.of(
				nonHeapMemoryResult("committed", 12345),
				nonHeapMemoryResult("init", 23456),
				nonHeapMemoryResult("max", -1),
				nonHeapMemoryResult("used", 45678));
	}

	public static Result numericBelowCPrecisionResult() {
		return new Result(
				0,
				"ObjectPendingFinalizationCount",
				"sun.management.MemoryImpl",
				"ObjectDomainName",
				"MemoryAlias",
				"type=Memory",
				ImmutableList.<String>of(),
				Double.MIN_VALUE);
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
				ImmutableList.<String>of(),
				10);
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

	public static ImmutableList<Result> dummyResultWithColon() {
		return ImmutableList.of(
				numericResultWithColon(),
				booleanTrueResult());
	}


	public static ImmutableList<Result> singleResult(Result result) {
		return ImmutableList.of(result);
	}
}
