package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.Outline;

public interface OperateAble<T extends ONode> {
    void addDefinedToBe(Outline outline);

    T node();

    void addHasToBe(Outline outline);
}
