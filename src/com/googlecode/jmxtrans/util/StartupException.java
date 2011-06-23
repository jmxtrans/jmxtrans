package com.googlecode.jmxtrans.util;

public class StartupException extends Exception {

    public StartupException(String msg) {
        super(msg);
    }

    public StartupException(String msg, Exception ex) {
        super(msg, ex);
    }
}
