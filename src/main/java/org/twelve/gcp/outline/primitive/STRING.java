package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.outline.builtin.String_;

public class STRING extends Primitive{
    private final static String_ string_ = new String_();
    public STRING(AbstractNode node){
        super(string_,node,node.ast());
        this.loadBuiltInMethods();
    }
    public STRING(AST ast){
        super(string_,null,ast);
    }
}
