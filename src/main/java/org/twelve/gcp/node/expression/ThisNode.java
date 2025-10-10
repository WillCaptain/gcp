package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class ThisNode extends KeyWord {
    public ThisNode(AST ast, Token<String> token) {
        super(ast, token);
    }

    @Override
    public String lexeme() {
        return "this";
    }
    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        GCPErrorReporter.report(this, GCPErrCode.THIS_IS_NOT_ASSIGNABLE);
    }
}
