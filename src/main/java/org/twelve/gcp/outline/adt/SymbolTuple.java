package org.twelve.gcp.outline.adt;

import org.twelve.gcp.outline.primitive.SYMBOL;

public class SymbolTuple extends Tuple {
    public SymbolTuple(SYMBOL symbol, Tuple tuple) {
        super(tuple);
        this.base = symbol;
    }

    @Override
    public String toString() {
        return base + super.toString().replace("()", "");
    }

    @Override
    public SYMBOL base() {
        return (SYMBOL) super.base();
    }

    @Override
    public String type() {
        return super.type() + base();
    }
}
