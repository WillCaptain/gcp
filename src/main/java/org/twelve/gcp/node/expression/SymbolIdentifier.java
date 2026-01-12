package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class SymbolIdentifier extends Identifier{
    public SymbolIdentifier(AST ast, Token<String> token) {
        super(ast, token);
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
