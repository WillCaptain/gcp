package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Symbol_;

public class SYMBOL extends Primitive {
    public SYMBOL(SymbolIdentifier symbol) {
        super(new Symbol_(symbol.name()), symbol, symbol.ast());
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (another == another.ast().Nothing) return true;
        if (another instanceof SYMBOL) {
            return this.toString().equals(another.toString());
        }
        return false;
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
