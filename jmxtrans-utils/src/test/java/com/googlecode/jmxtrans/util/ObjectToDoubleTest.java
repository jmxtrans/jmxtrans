package com.googlecode.jmxtrans.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectToDoubleTest {

    private ObjectToDouble converter = new ObjectToDouble();

    @Test
    public void doubleReturnedAsItself() {
        Double input = 0.1;
        Double output = converter.apply(input);

        assertThat(output).isEqualTo(0.1);
    }

    @Test
    public void integerIsConvertedToDouble() {
        Integer input = 1;
        Double output = converter.apply(input);

        assertThat(output).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringIsNotConverted() {
        converter.apply("");
    }

}
