package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;

import java.util.stream.Collectors;

public class OptionTypeNode extends TypeNode {
    public OptionTypeNode(TypeNode ... types) {
        super(types[0].ast(), null);
        for (TypeNode expression : types) {
            this.addNode(expression);
        }
    }

    @Override
    public Location loc() {
        Long min = this.nodes().stream().map(m -> m.loc().start()).min((m1, m2) -> m1 < m2 ? -1 : 1).get();
        Long max = this.nodes().stream().map(m -> m.loc().start()).min((m1, m2) -> m1 < m2 ? -1 : 1).get();
        return new SimpleLocation(min, max);
    }

    @Override
    public String lexeme() {
        return this.nodes().stream().map(n->n.lexeme()).collect(Collectors.joining("|"));
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
}
