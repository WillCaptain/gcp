package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class SymbolTupleTypeTypeNode extends TupleTypeNode implements SymbolTypeNode<TupleTypeNode> {
    private final SymbolIdentifier symbol;
    private final boolean missingSingleItemTupleComma;

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<ReferenceNode> refs, List<TypeNode> members) {
        this(symbol, refs, members, false);
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<ReferenceNode> refs, List<TypeNode> members,
                                   boolean missingSingleItemTupleComma) {
        super(refs,members);
        this.symbol = this.addNode(0,symbol);
        this.missingSingleItemTupleComma = missingSingleItemTupleComma;
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<TypeNode> members) {
        this(symbol,new ArrayList<>(),members);
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol, List<TypeNode> members, boolean missingSingleItemTupleComma) {
        this(symbol, new ArrayList<>(), members, missingSingleItemTupleComma);
    }

    public SymbolTupleTypeTypeNode(SymbolIdentifier symbol) {
        super(symbol.ast());
        this.symbol = this.addNode(symbol);
        this.missingSingleItemTupleComma = false;
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

    public boolean missingSingleItemTupleComma() {
        return this.missingSingleItemTupleComma;
    }

    @Override
    public String lexeme() {
        return symbol.lexeme()+super.lexeme();
    }
}
