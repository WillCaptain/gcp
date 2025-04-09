package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.String_;

public class STRING extends Primitive{
    private final static String_ string = new String_();

    public STRING(ONode node){
        super(string,node);
    }
}
