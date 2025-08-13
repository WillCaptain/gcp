package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.String_;

public class STRING extends Primitive{
    private final static String_ string_ = new String_();
    public STRING(Node node){
        super(string_,node,node.ast());
        this.loadMethods();
    }
    public STRING(AST ast){
        super(string_,null,ast);
    }
}
