package org.twelve.gcp.exception;

public class GCPRuntimeException extends RuntimeException{
    private final GCPErrCode errCode;
    private final String stackTrace;

    public GCPRuntimeException(GCPErrCode errCode){
        this(errCode, errCode.name());
    }

    public GCPRuntimeException(GCPErrCode errCode, String message){
        super(message);
        this.errCode = errCode;
        this.stackTrace = message;
    }

    public GCPErrCode errCode() {
        return this.errCode;
    }
}
