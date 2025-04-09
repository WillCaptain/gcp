package org.twelve.gcp.node;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.node.expression.Expression;

public abstract class ValueNode<T extends ValueNode<T>> extends Expression {
    public ValueNode(OAST ast, Location loc) {
        super(ast, loc);
    }

    public abstract boolean isSame(T obj);
}
