package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.ValueNode;
import org.twelve.gcp.outline.Outline;
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
        return new Literal(typeNode,typeNode.outline(),ast());
    }

    @Override
    public Outline accept(Inferences inferences) {
//        return this.typeNode.accept(inferences);
       return this.outline();
    }
}
