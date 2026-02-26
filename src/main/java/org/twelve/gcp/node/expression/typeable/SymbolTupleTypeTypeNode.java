package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class SymbolTupleTypeTypeNode extends TupleTypeNode implements SymbolTypeNode<TupleTypeNode> {
    private final SymbolIdentifier symbol;

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<ReferenceNode> refs, List<TypeNode> members) {
        super(refs,members);
        this.symbol = this.addNode(0,symbol);
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<TypeNode> members) {
        this(symbol,new ArrayList<>(),members);
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol) {
        super(symbol.ast());
        this.symbol = this.addNode(symbol);
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
    public TupleTypeNode data() {
        return this;
    }

    @Override
    public String lexeme() {
        return symbol.lexeme()+super.lexeme();
    }
}
