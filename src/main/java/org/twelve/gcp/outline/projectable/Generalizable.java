package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.NOTHING;

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
}
