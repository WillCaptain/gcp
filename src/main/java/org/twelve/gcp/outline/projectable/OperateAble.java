package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;

public interface OperateAble<T extends Node> {
    boolean addDefinedToBe(Outline outline);

    T node();

    void addHasToBe(Outline outline);

}
