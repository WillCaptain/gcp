package org.twelve.gcp.node.statement;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.outline.Outline;

import java.util.List;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

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
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }

}
