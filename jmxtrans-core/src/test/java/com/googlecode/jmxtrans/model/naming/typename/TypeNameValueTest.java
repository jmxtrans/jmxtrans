package com.googlecode.jmxtrans.model.naming.typename;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;

public class TypeNameValueTest {

    private void assertExtractsTypeNameValues(String typeNameStr, TypeNameValue... expectedValues) {
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

    @Test
    public void testExtractMap() {
        assertThat(TypeNameValue.extractMap(null)).isEmpty();
        assertThat(TypeNameValue.extractMap("")).isEmpty();
        assertThat(TypeNameValue.extractMap("x-key1-x")).isEqualTo(makeMap("x-key1-x", ""));
        assertThat(TypeNameValue.extractMap("x-key1-x,x-key2-x")).isEqualTo(makeMap("x-key1-x", "", "x-key2-x", ""));
        assertThat(TypeNameValue.extractMap("x-key1-x=x-value1-x")).isEqualTo(makeMap("x-key1-x", "x-value1-x"));

        assertThat(TypeNameValue.extractMap("x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
                .isEqualTo(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "y-value2-y"));
        assertThat(TypeNameValue.extractMap("x-key1-x=x-value1-x,y-key2-y=y-value2-y,z-key3-z=z-value3-z"))
                .isEqualTo(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "y-value2-y", "z-key3-z", "z-value3-z"));
        assertThat(TypeNameValue.extractMap("x-key1-x=x-value1-x,y-key2-y,yy-key2.5-yy=a=1,z-key3-z=z-value3-z"))
                .isEqualTo(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "", "yy-key2.5-yy", "a=1", "z-key3-z", "z-value3-z"));
    }

    /** Convenience method for creating a Map for comparison. */
    private Map<String, String> makeMap(String... keysAndValues) {
        Map<String, String> result;
        int cur;

        result = newHashMap();

        cur = 0;
        while (cur < keysAndValues.length) {
            if (cur < keysAndValues.length - 1) {
                result.put(keysAndValues[cur], keysAndValues[cur + 1]);
                cur += 2;
            } else {
                result.put(keysAndValues[cur], "");
                cur++;
            }
        }

        return result;
    }
}
