package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

public class IGNORE extends BuildInOutline {
    public IGNORE(AST ast) {
        super(ast);
    }

    @Override
    public long id() {
        return CONSTANTS.IGNORE_INDEX;
    }

    @Override
    public boolean is(Outline another) {
        return false;
    }

    @Override
    public boolean equals(Outline another) {
        return another instanceof IGNORE;
    }

    @Override
    public boolean beAssignedAble() {
        return false;
    }

    @Override
    public String toString() {
        return "-";
    }
}
