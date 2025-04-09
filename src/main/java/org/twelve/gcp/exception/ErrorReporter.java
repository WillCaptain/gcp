package org.twelve.gcp.exception;

import org.twelve.gcp.ast.Node;

import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {
    private List<GCPError> errors = new ArrayList<>();
    public static void report(Node node, GCPErrCode errCode){
        node.ast().addError(new GCPError(node,errCode));
    }
    public static void report(GCPErrCode errCode){
        throw new GCPRuntimeException(errCode);

    }
    public static void report(GCPErrCode errCode,String stackTrace){
        throw new GCPRuntimeException(errCode,stackTrace);
    }
}
