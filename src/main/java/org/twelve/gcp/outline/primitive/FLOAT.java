package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.builtin.BuildInOutline;
import org.twelve.gcp.outline.builtin.Float_;

public class FLOAT extends DECIMAL{
    private final static Float_ float_ = new Float_();
    protected FLOAT(BuildInOutline buildInOutline, Node node, AST ast) {
        super(buildInOutline,node,ast);
    }

    public FLOAT(Node node){
        super(float_,node,node.ast());
        this.loadBuiltInMethods();
    }
    public FLOAT(AST ast){
        super(float_,null,ast);
    }
}
