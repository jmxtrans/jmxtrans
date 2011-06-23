package com.googlecode.jmxtrans.util;

public class LifecycleException extends Exception {

    public LifecycleException(String msg) {
        super(msg);
    }

    public LifecycleException(String msg, Exception ex) {
        super(msg, ex);
    }
}
