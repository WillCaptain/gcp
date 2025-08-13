package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Decimal_;

public class DECIMAL extends DOUBLE {
    private final static Decimal_ decimal_ = new Decimal_();

    public DECIMAL(Node node){
        super(decimal_,node,node.ast());
        this.loadMethods();;
    }
    protected DECIMAL(BuildInOutline buildInOutline, Node node,AST ast) {
        super(buildInOutline,node,ast);
    }
    public  DECIMAL(AST ast){
        super(decimal_,null,ast);
    }


}

