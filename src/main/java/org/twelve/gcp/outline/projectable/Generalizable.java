package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.NOTHING;

public interface Generalizable extends Projectable {
    Outline declaredToBe();

    Outline definedToBe();

    Outline extendToBe();

    Outline hasToBe();

    Outline min();

    Outline max();

    void addDefinedToBe(Outline defined);

    void addExtendToBe(Outline outline);

    void addHasToBe(Outline outline);

    @Override
    default boolean emptyConstraint() {
        return (this.max() instanceof NOTHING) && (this.min() instanceof ANY);
    }
}
