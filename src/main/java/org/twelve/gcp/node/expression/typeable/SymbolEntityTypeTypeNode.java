package org.twelve.gcp.node.expression.typeable;


import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.outline.Outline;

import java.util.List;

public class SymbolEntityTypeTypeNode extends EntityTypeNode implements SymbolTypeNode<EntityTypeNode> {


    private final SymbolIdentifier symbol;

    public SymbolEntityTypeTypeNode(SymbolIdentifier symbol, List<Variable> members) {
        super(members);
        this.symbol = this.addNode(0, symbol);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    @Override
    public SymbolIdentifier symbol() {
        return this.symbol;
    }

    @Override
    public EntityTypeNode data() {
        return this;
    }

    @Override
    public String lexeme() {
        return symbol.lexeme() + super.lexeme();
    }
}
