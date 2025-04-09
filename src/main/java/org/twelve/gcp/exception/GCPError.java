package org.twelve.gcp.exception;

import org.twelve.gcp.ast.Node;

public class GCPError {
    private final GCPErrCode errorCode;
    private final Node node;

    GCPError(Node node, GCPErrCode errorCode) {
        this.errorCode = errorCode;
        this.node = node;
    }

    public Node node(){
        return this.node;
    }

    public GCPErrCode errorCode(){
        return this.errorCode;
    }

    @Override
    public String toString() {
        return this.errorCode().toString();
    }
}
