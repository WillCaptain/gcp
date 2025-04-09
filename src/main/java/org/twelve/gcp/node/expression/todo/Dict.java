package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.node.expression.Expression;

public class Dict extends Expression {
    public Dict(OAST ast, Location loc) {
        super(ast, loc);
    }
}
