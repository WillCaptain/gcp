package org.twelve.gcp.outline.adt;

import org.twelve.gcp.outline.primitive.SYMBOL;

public class SymbolEntity extends Entity {
    public SymbolEntity(SYMBOL base, Entity entity) {
        super(entity.node(),entity.ast(),base,entity.members());
    }

    @Override
    public String toString() {
        return base + super.toString();
    }

    @Override
    public SYMBOL base() {
        return (SYMBOL) super.base();
    }

    @Override
    public String type() {
        return super.type()+base();
    }
}
