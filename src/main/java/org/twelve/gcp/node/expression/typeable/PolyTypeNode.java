package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

import java.util.stream.Collectors;

public class PolyTypeNode extends OptionTypeNode{
    public PolyTypeNode(TypeNode ... types) {
        super(types);
    }

    @Override
    public String lexeme() {
        return this.nodes().stream().map(n->n.lexeme()).collect(Collectors.joining("&"));
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
