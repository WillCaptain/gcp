package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;

public abstract class Statement extends AbstractNode {
    public Statement(AST ast) {
        super(ast);
    }
}
