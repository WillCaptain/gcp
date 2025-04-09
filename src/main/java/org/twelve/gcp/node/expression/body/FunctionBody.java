package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class FunctionBody extends Body {
    public FunctionBody(OAST ast) {
        super(ast);
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

}
