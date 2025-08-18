package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.BOOL;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class BoolNode  extends Identifier {
    public BoolNode(AST ast, Token<String> token) {
        super(ast, token);
        this.outline = new BOOL(this);
    }

    @Override
    public String lexeme() {
        return this.token().lexeme();
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        GCPErrorReporter.report(this, GCPErrCode.THIS_IS_NOT_ASSIGNABLE);
    }
}
