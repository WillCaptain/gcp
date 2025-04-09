package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;

public class Dict extends Expression {
    public Dict(AST ast, Location loc) {
        super(ast, loc);
    }
}
