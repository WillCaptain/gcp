package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.SimpleLocation;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

import java.util.stream.Collectors;

/**
 * Poly节点
 * 例如：1|“some”，代表了poly既是Number也是String
 */
public class PolyNode extends Expression{
    public PolyNode(Expression e,Expression ... expressions) {
        super(e.ast(), null);
        this.addNode(e);
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
        if(this.nodes().size()==1){
            return this.nodes().getFirst().lexeme();
        }else {
            return this.nodes().stream().map(Node::lexeme).collect(Collectors.joining("&"));
        }
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
