package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;

/**
 * Represents a nullable type {@code T?}, which is syntactic sugar for {@code T|Nothing}.
 * The inner type node is stored as the sole child node; inference wraps it with {@code Nothing}.
 */
public class NullableTypeNode extends TypeNode {
    private final TypeNode inner;

    public NullableTypeNode(TypeNode inner) {
        super(inner.ast(), null);
        this.inner = inner;
        this.addNode(inner);
    }

    public TypeNode inner() { return inner; }

    @Override
    public String lexeme() { return inner.lexeme() + "?"; }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        Outline innerType = inner.infer(inferencer);
        return Option.from(this, innerType, this.ast().Nothing);
    }
}
