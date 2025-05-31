package org.twelve.gcp.node.expression.referable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

/**
 * traditional generic type T in <T>
 */
public class ReferenceNode extends Identifier {
    private final TypeNode declared;
    public ReferenceNode(Identifier ref, TypeNode declared) {
        super(ref.ast(), ref.token());
        this.declared = declared;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    public TypeNode declared(){
        return declared;
    }
}
