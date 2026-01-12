package org.twelve.gcp.node.expression;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.typeable.TupleTypeNode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.outline.Outline;

import java.util.List;

public class SymbolTupleTypeTypeNode extends TupleTypeNode implements SymbolTypeNode<TupleTypeNode> {
    private final SymbolIdentifier symbol;

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<TypeNode> members) {
        super(members);
        this.symbol = this.addNode(0,symbol);
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol) {
        super(symbol.ast());
        this.symbol = this.addNode(symbol);
    }


    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
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
