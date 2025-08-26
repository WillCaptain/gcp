package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.outline.projectable.Generic;

public class Question extends TypeNode {
    private final Token<String> token;

    public Question(AST ast) {
        this(ast,null);
    }
    public Question(AST ast, Token<String> token) {
        super(ast);
        this.outline = Generic.from(this,null);
        this.token = token;
    }

    @Override
    public Location loc() {
        return token.loc();
    }

    @Override
    public String lexeme(){
        return "?";
    }
}
