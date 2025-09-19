package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;

public interface Interpreter {
    default <T>Result<T> visit(AbstractNode node){
        GCPErrorReporter.report(GCPErrCode.INTERPRETER_NOT_IMPLEMENTED);
        return null;
    }
}
