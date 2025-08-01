package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.outline.projectable.Generic;

public class Question extends TypeNode {
    public Question(AST ast) {
        super(ast);
        this.outline = Generic.from(this,null);
    }

    @Override
    public String lexeme(){
        return "?";
    }
}
