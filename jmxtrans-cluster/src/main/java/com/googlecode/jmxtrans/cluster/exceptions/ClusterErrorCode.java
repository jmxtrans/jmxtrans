package com.googlecode.jmxtrans.cluster.exceptions;

public enum ClusterErrorCode implements ErrorCode {

    ZOKKEPER_NOR_AVAILABLE(101),
    CONNECTION_TIMEOUT(102),
    MISSING_CONFIGURATION(103);

    private final int number;

    private ClusterErrorCode(int number) {
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

}