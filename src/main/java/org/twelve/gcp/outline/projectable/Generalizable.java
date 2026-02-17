package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.NOTHING;

/**
 * an outline can be generalized, including
 * Generiable, Returnable
 */
public interface Generalizable extends Projectable,Constrainable {
    Outline declaredToBe();

    Outline definedToBe();

    Outline extendToBe();

    Outline hasToBe();

    Outline min();

    Outline max();

    @Override
    default boolean emptyConstraint() {
        return (this.max() instanceof NOTHING) && (this.min() instanceof ANY);
    }

    @Override
    default boolean containsGeneric(){
        return true;
    }

    @Override
    default void updateThis(ProductADT me) {
        this.declaredToBe().updateThis(me);
        this.definedToBe().updateThis(me);
        this.extendToBe().updateThis(me);
        this.hasToBe().updateThis(me);
    }
}
