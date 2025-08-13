package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Any_;

public class ANY extends Primitive {
    private static Any_ any_ = new Any_();
   public ANY(AST ast){
        super(any_,null,ast);
    }
    public ANY(Node node){
       super(any_,node,node.ast());

    }
    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }

    @Override
    public String toString() {
        return "any";
    }
}
