package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.String_;

public class STRING extends Primitive{
    private final static String_ string = new String_();
    public static STRING create(Node node){
        STRING str = new STRING(node);
        str.init();
        return str;
    }
    private STRING(Node node){
        super(string,node);
    }
    public STRING(){
        this(null);
    }
}
