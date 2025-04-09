package org.twelve.gcp.node;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;

public abstract class ValueNode<T extends ValueNode<T>> extends Expression {
    public ValueNode(AST ast, Location loc) {
        super(ast, loc);
    }

    public abstract boolean isSame(T obj);
}
