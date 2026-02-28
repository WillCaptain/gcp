package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.Literal;

/**
 * literal value as type — wraps any expression node (string, number, entity, tuple, function …).
 */
public class LiteralTypeNode extends TypeNode {

    private final Node typeNode;

    public LiteralTypeNode(Node node) {
        super(node.ast());
        this.addNode(node);
        this.typeNode = node;
    }

    @Override
    public String lexeme() {
        return this.nodes().getFirst().lexeme();
    }

    @Override
    public Outline outline() {
        return new Literal(typeNode, typeNode.outline(), ast());
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        if (this.typeNode.outline() instanceof UNKNOWN) {
            // Inner node not yet inferred — infer it now, then immediately wrap in Literal.
            // This ensures the return type is always Literal from the very first pass,
            // which is required for entity/tuple literal types (their inner nodes start as UNKNOWN).
            Outline inner = this.typeNode.infer(inferencer);
            return new Literal(typeNode, inner, ast());
        } else {
            return this.outline();
        }
    }

}
