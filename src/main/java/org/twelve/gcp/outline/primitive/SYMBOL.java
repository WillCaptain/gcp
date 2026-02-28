package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Symbol_;

public class SYMBOL extends Primitive {
    public SYMBOL(SymbolIdentifier symbol) {
        super(new Symbol_(symbol.name()), symbol, symbol.ast());
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another == another.ast().Nothing) return true;
        return this.toString().equals(another.toString());
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        return this.toString().equals(another.toString());
    }

    @Override
    public boolean is(Outline another) {
        // Bypass maybe() check: SYMBOL is a symbolic reference matched purely by name.
        if (this.toString().equals(another.toString())) return true;
        return super.is(another);
    }

    @Override
    public long id() {
        return this.buildIn.hashCode();
    }


    @Override
    public String toString() {
        return this.buildIn.name();
    }

    @Override
    public boolean equals(Outline another) {
        if(another instanceof SYMBOL) {
            return this.buildIn.name().equals(((SYMBOL) another).buildIn.name());
        }else{
            return false;
        }
    }
}
