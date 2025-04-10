package org.twelve.gcp.exception;

import org.twelve.gcp.ast.Node;

public class GCPError {
    private final GCPErrCode errorCode;
    private final Node node;
    private final String message;

    public GCPError(Node node, GCPErrCode errorCode, String message) {
        this.errorCode = errorCode;
        this.node = node;
        this.message = message;
    }

    public Node node() {
        return this.node;
    }

    public GCPErrCode errorCode() {
        return this.errorCode;
    }

    @Override
    public String toString() {
        return this.node.outline()+" "+this.errorCode().toString() + (message.isEmpty() ?"":(": " + message));
    }
}
