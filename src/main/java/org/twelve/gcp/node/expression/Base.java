package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class Base  extends Identifier{
    public Base(AST ast, Token token) {
        super(ast, token);
    }

    @Override
    public String lexeme() {
        return "base";
    }
    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        ErrorReporter.report(this, GCPErrCode.THIS_IS_NOT_ASSIGNABLE);
    }
}