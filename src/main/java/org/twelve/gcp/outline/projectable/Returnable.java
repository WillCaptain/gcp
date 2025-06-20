package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

public interface Returnable extends Outline, Generalizable {
    void setArgument(Outline argument);

    Outline guess();

    Outline supposedToBe();

    boolean addReturn(Outline ret);
}
