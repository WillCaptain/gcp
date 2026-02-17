package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;

public interface Returnable extends Outline, Generalizable {
    void setArgument(Long argument);

    Outline guess();

    Outline supposedToBe();

    boolean addReturn(Outline ret);

    @Override
    default void updateThis(ProductADT me) {
        Generalizable.super.updateThis(me);
        this.supposedToBe().updateThis(me);
    }
}
