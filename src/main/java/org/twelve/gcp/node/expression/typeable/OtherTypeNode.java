package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;

/**
 * ... don't need to be inferred
 */
public class OtherTypeNode extends TypeNode{
    public OtherTypeNode(AST ast) {
        super(ast);
    }

    @Override
    public String lexeme() {
        return "...";
    }
}
