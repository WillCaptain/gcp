package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.OperateAble;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

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
            if (inferred instanceof Genericable<?,?>) {
                if (inferred.node()!=null &&
                        me.node().nodeIndex() > inferred.node().nodeIndex()) {//bigger index argument refer smaller
                    me.addExtendToBe(inferred);
                }
            } else {
                me.addExtendToBe(inferred);
            }
        }

//        if(!(inferred instanceof Projectable && ((Projectable) inferred).emptyConstraint())
//                && inferred.canBe(this.outline)) return;

        if (inferred instanceof OperateAble) {
            OperateAble<?> you = (OperateAble<?>) inferred;
            if (!(this.outline instanceof Genericable<?, ?>) ||
                    this.outline.node().nodeIndex() < you.node().nodeIndex()) {
                you.addHasToBe(this.outline);
            }
        }
        if (!inferred.is(this.outline)) {
            ErrorReporter.report(this.parent(), GCPErrCode.OUTLINE_MISMATCH,
                    inferred.node() + CONSTANTS.MISMATCH_STR + this);
        }
    }
}
