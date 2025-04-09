package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;

/**
 * including array accessor and list accessor
 * map accessor will be put in member accessor
 * a[index]
 */
public class ArrayAccessor extends Expression {
    public ArrayAccessor(AST ast, Location loc) {
        super(ast, loc);
    }
}
