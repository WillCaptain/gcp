package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.SumADT;
import org.twelve.gcp.outline.adt.SymbolTuple;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class SymbolTupleUnpackNode extends TupleUnpackNode implements SymbolUnpackNode<TupleUnpackNode> {
    private final SymbolIdentifier symbol;

    public SymbolTupleUnpackNode(AST ast, SymbolIdentifier symbol, TupleUnpackNode tupleUnpackNode) {
        super(ast, tupleUnpackNode.begins, tupleUnpackNode.ends);
        this.symbol = this.addNode(symbol);
//        this.outline = new Unpack(this, new SYMBOL(symbol));
    }

    @Override
    public SymbolIdentifier symbol() {
        return this.symbol;
    }

    @Override
    public TupleUnpackNode unpackNode() {
        return this;
    }

    @Override
    public String toString() {
        return this.symbol.toString() + super.toString();
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        // When the match subject is a Generic with a declared union type (e.g. person: Human),
        // unwrap the declared type to find the SymbolTuple variant matching this symbol.
        Outline effective = inferred;
        if (inferred instanceof Genericable<?, ?> g && !(inferred instanceof SumADT)) {
            Outline declared = g.declaredToBe();
            if (declared instanceof SumADT) {
                effective = declared;
            }
        }
        Outline resolved = inferred;
        if (effective instanceof SumADT sum) {
            resolved = sum.options().stream()
                    .filter(o -> o instanceof SymbolTuple st
                            && symbol.name().equals(st.base().toString()))
                    .findFirst()
                    .orElse(inferred);
        }
        super.assign(env, resolved);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        this.outline = super.acceptInfer(inferencer);
        return inferencer.visit(this);
    }
}