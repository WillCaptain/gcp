package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class UNIT extends BuildInOutline{
    public UNIT(AST ast) {
        super(ast);
    }

    @Override
    public long id() {
        return CONSTANTS.UNIT_INDEX;
    }

//    @Override
//    public boolean is(Outline another) {
//        return another instanceof UNIT;
//    }

    @Override
    public boolean tryIamYou(Outline another) {
        return another instanceof UNIT;
    }

    @Override
    public boolean equals(Outline another) {
        return another instanceof UNIT;
    }

    @Override
    public String toString() {
        return "()";
    }
    @Override
    public boolean beAssignedAble() {
        return false;
    }

}
