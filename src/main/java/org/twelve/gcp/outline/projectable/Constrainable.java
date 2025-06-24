package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

/**
 * an outline can add constraints
 * add defined to be
 * add extend to be
 * add has to be
 */
public interface Constrainable {
    boolean addDefinedToBe(Outline defined);

    void addExtendToBe(Outline extend);

    void addHasToBe(Outline hasTo);

}
