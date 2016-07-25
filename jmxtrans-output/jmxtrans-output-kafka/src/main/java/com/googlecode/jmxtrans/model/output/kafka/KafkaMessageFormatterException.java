package com.googlecode.jmxtrans.model.output.kafka;

/**
 * Created by elludo, seuf on 25/07/2016.
 */
public class KafkaMessageFormatterException extends RuntimeException {

    public KafkaMessageFormatterException() {
        super();
    }

    public KafkaMessageFormatterException(String message) {
        super(message);
    }

    public KafkaMessageFormatterException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaMessageFormatterException(Throwable cause) {
        super(cause);
    }

    protected KafkaMessageFormatterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
