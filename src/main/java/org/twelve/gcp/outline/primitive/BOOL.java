package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.Bool_;

public class BOOL extends Primitive{
    private final static Bool_ int_ = new Bool_();

    public  BOOL(Node node){
        super(int_,node);
    }
}
