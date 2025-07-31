package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;

public class MapNode  extends Expression {
    public MapNode(AST ast, TupleNode... tuples) {
        super(ast, null);
    }
}
