package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;

public interface Interpreter {
    default <T>Result<T> visit(Node node){
        ErrorReporter.report(GCPErrCode.INTERPRETER_NOT_IMPLEMENTED);
        return null;
    }
}
