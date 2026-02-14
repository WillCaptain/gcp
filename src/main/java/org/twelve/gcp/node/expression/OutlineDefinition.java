package org.twelve.gcp.node.expression;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

public class OutlineDefinition extends Expression {
    private final SymbolIdentifier symbolNode;
    private final TypeNode typeNode;

    public OutlineDefinition(SymbolIdentifier symbolNode, TypeNode typeNode) {
        super(symbolNode.ast(),null);
        this.symbolNode = this.addNode(symbolNode);
        this.typeNode = this.addNode(typeNode);
    }

    public SymbolIdentifier symbolNode(){
        return this.symbolNode;
    }

    public TypeNode typeNode(){
        return this.typeNode;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        return symbolNode().lexeme()+" = "+typeNode.lexeme();
    }
}
