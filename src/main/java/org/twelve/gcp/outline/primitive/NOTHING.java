package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Nothing_;

/**
 * represent null
 */
public class NOTHING extends Primitive {
    private static Nothing_ nothing_ = new Nothing_();
    public NOTHING(AST ast){
        super(nothing_,null, ast);
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return true;
    }

    @Override
    public boolean beAssignable() {
        return false;
    }

    @Override
    public String toString() {
        return "null";
    }
}
