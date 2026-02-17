package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.outline.builtin.UNKNOWN;

public abstract class Accessor extends Assignable {
    public Accessor(AST ast) {
        super(ast, null);
    }

    @Override
    public boolean inferred() {
        boolean result =  !(this.outline() instanceof UNKNOWN);
        if(!result) ast().missInferred().add(this);
        return result;
//        return !(this.outline() instanceof UNKNOWN);
    }
}
