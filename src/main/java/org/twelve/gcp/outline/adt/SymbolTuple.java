package org.twelve.gcp.outline.adt;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.SYMBOL;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

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

    @Override
    public Outline guess() {
        return new SymbolTuple(base.copy(), cast(super.guess()));
    }

    @Override
    public Entity copy(Map<Outline, Outline> cache) {
        return new SymbolTuple(this.base.copy(), cast(super.copy(cache)));
    }

    @Override
    public boolean equals(Outline another) {
        if(!(another instanceof SymbolTuple)) return false;
        SymbolTuple you = (SymbolTuple) another;
        if(!this.base().equals(you.base())) return false;
        return super.equals(another);
    }
}
