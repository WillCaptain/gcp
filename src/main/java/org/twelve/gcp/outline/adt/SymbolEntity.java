package org.twelve.gcp.outline.adt;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.SYMBOL;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

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

    @Override
    public Outline guess() {
        return new SymbolEntity(base.copy(), cast(super.guess()));
    }

    @Override
    public Entity copy(Map<Outline, Outline> cache) {
        return new SymbolEntity(base.copy(),cast(super.copy(cache)));
    }

    @Override
    public boolean equals(Outline another) {
        if(!(another instanceof SymbolEntity)) return false;
        SymbolEntity you = (SymbolEntity) another;
        if(!this.base().equals(you.base())) return false;
        return super.equals(another);
    }
}
