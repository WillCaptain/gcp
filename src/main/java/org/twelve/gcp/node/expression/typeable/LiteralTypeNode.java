package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.Literal;

/**
 * literal value as type
 */
public class LiteralTypeNode extends TypeNode {

    private final ValueNode typeNode;

    public LiteralTypeNode(ValueNode node) {
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
            return this.typeNode.infer(inferencer);
//            return this.typeNode.accept(inferences);
        } else {
            return this.outline();
        }
    }

}
