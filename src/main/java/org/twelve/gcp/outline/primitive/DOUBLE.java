package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Double_;

public class DOUBLE extends NUMBER{
    private final static Double_ double_ = new Double_();

    public DOUBLE(Node node){
        super(double_,node,node.ast());
        this.loadMethods();
    }
    protected DOUBLE(BuildInOutline buildInOutline, Node node,AST ast) {
        super(buildInOutline,node,ast);
    }
    public DOUBLE(AST ast){
        super(double_,null,ast);
    }
}
