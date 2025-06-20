package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

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
}
