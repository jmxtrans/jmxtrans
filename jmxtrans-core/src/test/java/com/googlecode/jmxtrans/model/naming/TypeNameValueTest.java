package com.googlecode.jmxtrans.model.naming;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

public class TypeNameValueTest {

    private void assertExtractsTypeNameValues(String typeNameStr, TypeNameValue ... expectedValues) {
        Assert.assertArrayEquals(expectedValues,
                Iterables.toArray(TypeNameValue.extract(typeNameStr), TypeNameValue.class));
    }

    @Test
    public void testExtractsEmpty() {
        assertExtractsTypeNameValues("");
        assertExtractsTypeNameValues(",,,");
    }

    @Test
    public void testExtractsSingle() {
        assertExtractsTypeNameValues("key=value", new TypeNameValue("key", "value"));
    }

    @Test
    public void testExtractsMultiple() {
        assertExtractsTypeNameValues("key1=value1,key2=value2",
                new TypeNameValue("key1", "value1"),
                new TypeNameValue("key2", "value2"));
        // reorder
        assertExtractsTypeNameValues("key2=value2,key1=value1",
                new TypeNameValue("key2", "value2"),
                new TypeNameValue("key1", "value1"));
    }

    @Test
    public void testExtractsWithoutValue() {
        assertExtractsTypeNameValues("key",
                new TypeNameValue("key"));
        assertExtractsTypeNameValues("key1,key2",
                new TypeNameValue("key1"),
                new TypeNameValue("key2"));
    }

    @Test
    public void testExtractsMixedCases() {
        assertExtractsTypeNameValues("key1=value1,,key2=value2,key3",
                new TypeNameValue("key1", "value1"),
                new TypeNameValue("key2", "value2"),
                new TypeNameValue("key3"));
        assertExtractsTypeNameValues("key1=value1,key2,,key3=value3",
                new TypeNameValue("key1", "value1"),
                new TypeNameValue("key2"),
                new TypeNameValue("key3", "value3"));
        assertExtractsTypeNameValues(",key1,key2=value2,key3=value3",
                new TypeNameValue("key1"),
                new TypeNameValue("key2", "value2"),
                new TypeNameValue("key3", "value3"));
        assertExtractsTypeNameValues("key1,key2=value2,key3,",
                new TypeNameValue("key1"),
                new TypeNameValue("key2", "value2"),
                new TypeNameValue("key3"));
    }
}
