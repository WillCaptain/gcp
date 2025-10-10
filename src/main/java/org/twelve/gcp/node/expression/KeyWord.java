package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;

public abstract class KeyWord extends Identifier {
    public KeyWord(AST ast, Token<String> token) {
        super(ast, token);
    }
}
