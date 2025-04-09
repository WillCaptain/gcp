package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.Expression;

public abstract class Accessor extends Assignable {
    public Accessor(OAST ast) {
        super(ast, null);
    }
}
