package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;

public class UnderLineNode extends KeyWord{
    public UnderLineNode(AST ast, Token<String> token) {
        super(ast, token);
        this.outline = ast.Any;
    }
}
