package org.twelve.gcp.node;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

import java.util.stream.Collectors;

public class LiteralUnionNode extends Expression {
    public LiteralUnionNode(OAST ast, Expression ... expressions) {
        super(ast, null);
        for (Expression expression : expressions) {
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
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
