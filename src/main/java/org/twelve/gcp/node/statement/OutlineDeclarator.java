package org.twelve.gcp.node.statement;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.outline.Outline;

import java.util.List;

public class OutlineDeclarator extends Statement {
    private final List<OutlineDefinition> definitions;

    public OutlineDeclarator(List<OutlineDefinition> outlineDefinitions) {
        super(outlineDefinitions.getFirst().ast());
        definitions = outlineDefinitions;
        for (OutlineDefinition definition : definitions) {
            this.addNode(definition);
        }
    }

    public List<OutlineDefinition> definitions(){
        return this.definitions;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder("outline ");
        for (OutlineDefinition definition : definitions) {
            sb.append(definition.lexeme()).append(", ");
        }
        return sb.substring(0,sb.length()-2)+";";
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
