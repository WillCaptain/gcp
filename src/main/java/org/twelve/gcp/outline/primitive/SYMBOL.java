package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Symbol_;

public class SYMBOL extends Primitive {
    public SYMBOL(String name){
        super(new Symbol_(name));
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if(another==Nothing) return true;
        if(another instanceof SYMBOL){
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
        return this.buildIn.toString();
    }
}
