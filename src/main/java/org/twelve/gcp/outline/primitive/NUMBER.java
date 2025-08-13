package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Number_;

public class NUMBER extends Primitive {
    private static Number_ number_ = new Number_();
    protected NUMBER(BuildInOutline buildInOutline, Node node,AST ast){
        super(buildInOutline,node,ast);
    }
    public  NUMBER(AST ast){
        super(number_,null,ast);
    }

}
