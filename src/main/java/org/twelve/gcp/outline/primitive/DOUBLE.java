package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Double_;

public class DOUBLE extends NUMBER{
    private final static Double_ double_ = new Double_();
    public DOUBLE(Node node){
        this(double_,node);
    }
    protected DOUBLE(BuildInOutline buildInOutline, Node node) {
        super(buildInOutline,node);
    }
}
