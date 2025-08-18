package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;

public interface Interpreter {
    default <T>Result<T> visit(Node node){
        GCPErrorReporter.report(GCPErrCode.INTERPRETER_NOT_IMPLEMENTED);
        return null;
    }
}
