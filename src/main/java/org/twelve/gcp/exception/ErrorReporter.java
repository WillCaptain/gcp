package org.twelve.gcp.exception;

import org.twelve.gcp.ast.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Central error reporting facility for the compiler/interpreter.
 *
 * <p>Handles both:
 * <ul>
 *   <li>Node-specific errors (attached to AST nodes)</li>
 *   <li>Global runtime errors (thrown as exceptions)</li>
 * </ul>
 *
 * @author huizi 2025
 */
public class ErrorReporter {
    /**
     * Reports a node-specific error and attaches it to the node's AST.
     *
     * @param node    The AST node where the error occurred (non-null)
     * @param errCode The error code (non-null)
     */
    public static void report(Node node, GCPErrCode errCode,String message) {
        GCPError err = node.ast().addError(new GCPError(node, errCode, message));
        if(err!=null){
            System.out.println(err);
        }
    }
    public static void report(Node node, GCPErrCode errCode) {
        report(node,errCode,"");
    }

    /**
     * Reports a global runtime error by throwing an exception.
     *
     * @param errCode The error code (non-null)
     * @throws GCPRuntimeException  always
     * @throws NullPointerException if error code is null
     */
    public static void report(GCPErrCode errCode) {
        throw new GCPRuntimeException(errCode);

    }

    /**
     * Reports a global runtime error with additional debug information.
     *
     * @param errCode   The error code (non-null)
     * @param debugInfo Additional debug context (may be null)
     * @throws GCPRuntimeException  always
     */
    public static void report(GCPErrCode errCode, String debugInfo) {
        throw new GCPRuntimeException(errCode, debugInfo);
    }
}
