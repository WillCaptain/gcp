package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.Integer_;

public class INTEGER extends LONG {
    private final static Integer_ int_ = new Integer_();

    public INTEGER(Node node) {
        super(int_, node);
    }
}
