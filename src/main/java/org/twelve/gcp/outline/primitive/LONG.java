package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Long_;

public class LONG extends FLOAT {
    private final static Long_ long_ = new Long_();
    public static LONG create(Node node){
        LONG l = new LONG(node);
        l.init();
        return l;
    }
    protected LONG(BuildInOutline buildInOutline, Node node) {
        super(buildInOutline, node);
    }

    private LONG(Node node) {
        this(long_,node);
    }
    public LONG(){
        this(null);
    }
}
