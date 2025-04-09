package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPError;

public interface Interpreter {
    default <T>Result<T> visit(Node node){
        ErrorReporter.report(GCPErrCode.INTERPRETER_NOT_IMPLEMENTED);
        return null;
    }
}
