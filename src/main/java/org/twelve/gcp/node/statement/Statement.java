package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;

public abstract class Statement extends Node {
    public Statement(AST ast) {
        super(ast);
    }
}
