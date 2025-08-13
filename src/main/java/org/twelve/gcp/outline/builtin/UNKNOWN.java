package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.Primitive;

/**
 * outline before type inference
 */
public class UNKNOWN extends BuildInOutline {
    public UNKNOWN(AST ast) {
        super(ast);
    }

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public boolean beAssignedAble() {
        return false;
    }

    @Override
    public boolean containsUnknown() {
        return true;
    }

    @Override
    public boolean beAssignable() {
        return false;
    }

    @Override
    public long id() {
        return CONSTANTS.UNKNOWN_INDEX;
    }
    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return false;
    }
}
