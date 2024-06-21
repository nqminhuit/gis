package org.nqm.exception;

public class GisException extends RuntimeException {

    public GisException(String msg) {
        super(msg, null, false, false);
    }

}
