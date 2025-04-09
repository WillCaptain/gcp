package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Assignable;

public abstract class Accessor extends Assignable {
    public Accessor(AST ast) {
        super(ast, null);
    }
}
