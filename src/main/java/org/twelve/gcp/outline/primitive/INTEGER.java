package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.Integer_;

public class INTEGER extends LONG {
    private final static Integer_ int_ = new Integer_();

    public static INTEGER create(Node node){
        INTEGER i = new INTEGER(node);
        i.init();
        return i;
    }

    private INTEGER(Node node) {
        super(int_, node);
    }

    public INTEGER(){
        this(null);
    }
}
