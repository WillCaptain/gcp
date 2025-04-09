package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.adt.ProductADT;

public abstract class Predicate extends Expression {
    public Predicate(AST ast) {
        super(ast, null);
    }

    @Override
    public ProductADT outline() {
        return ProductADT.Boolean;
    }
}
