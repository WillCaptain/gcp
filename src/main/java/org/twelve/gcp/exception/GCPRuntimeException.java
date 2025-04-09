package org.twelve.gcp.exception;

public class GCPRuntimeException extends RuntimeException{
    private final GCPErrCode errCode;
    private final String stackTrace;

    public GCPRuntimeException(GCPErrCode errCode){
        this(errCode,errCode.name());
    }
    GCPRuntimeException(GCPErrCode errCode, String stackTrace){
        this.errCode = errCode;
        this.stackTrace = stackTrace;
    }

    public GCPErrCode errCode() {
        return this.errCode;
    }
}
