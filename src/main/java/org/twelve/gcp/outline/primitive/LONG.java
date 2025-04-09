package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Long_;

public class LONG extends FLOAT {
    private final static Long_ long_ = new Long_();

    protected LONG(BuildInOutline buildInOutline, ONode node) {
        super(buildInOutline, node);
    }

    public LONG(ONode node) {
        this(long_,node);
    }
}
