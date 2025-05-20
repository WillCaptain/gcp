package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.OperateAble;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.outline.Outline.*;

public abstract class Assignable extends Expression {
    public Assignable(AST ast, Location loc) {
        super(ast, loc);
    }

    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if(!inferred.beAssignedAble()){
            ErrorReporter.report(GCPErrCode.NOT_BE_ASSIGNEDABLE);
            return;
        }
        //generic处理
        if (this.outline instanceof Genericable) {
            Genericable<?,?> me = ((Genericable<?,?>) this.outline);
            if (inferred instanceof Generic) {
                if (me.node().index() > ((Generic) inferred).node().index()) {//bigger index argument refer smaller
                    me.addExtendToBe(inferred);
                }
            } else {
                me.addExtendToBe(inferred);
            }
        }

        if (inferred.canBe(this.outline)) return;

        if (inferred instanceof OperateAble) {
            OperateAble<?> you = (OperateAble<?>) inferred;
            if (this.outline instanceof Generic) {
                if (((Generic) this.outline).node().index() < you.node().index()) {
                    you.addHasToBe(this.outline);
                }
            } else {
                ((Generic) inferred).addHasToBe(this.outline);
            }
        }
        if (!inferred.is(this.outline)) {
            ErrorReporter.report(this.parent(), GCPErrCode.OUTLINE_MISMATCH,
                    inferred.node() + CONSTANTS.MISMATCH_STR + this);
        }
    }
}
