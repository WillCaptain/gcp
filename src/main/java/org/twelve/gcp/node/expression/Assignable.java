package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.OperateAble;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.outline.Outline.*;

public abstract class Assignable extends Expression {
    public Assignable(AST ast, Location loc) {
        super(ast, loc);
    }

//    public abstract EnvSymbol symbol();

    public void assign(LocalSymbolEnvironment env, Outline inferred) {
//        if (inferred == Unknown || inferred == Ignore || inferred == Unit) {
//            ErrorReporter.report(this, GCPErrCode.OUTLINE_MISMATCH, inferred.name() + " can't be assigned");
//            return;
//        }

        //generic处理
        if (this.outline instanceof Generic) {
            Generic me = ((Generic) this.outline);
            if (inferred instanceof Generic) {
                if (me.node().index() > ((Generic) inferred).node().index()) {//bigger index argument refer smaller
                    me.addExtendToBe(inferred);
                }
            } else {
                ((Generic) this.outline).addExtendToBe(inferred);
            }
        }

        if (inferred.canBe(this.outline)) return;

        Node target = inferred.node() == null ? this : inferred.node();
        final String FAIL_SUM = "fail to sum in ";
        //option处理
        if (this.outline instanceof Option) {
            if (!((Option) this.outline).sum(inferred)) {
                ErrorReporter.report(target, GCPErrCode.OUTLINE_MISMATCH, FAIL_SUM + this.outline);
            }
            return;
        }
        //poly处理
        if (this.outline instanceof Poly) {
            //此处sum只有在函数参数f(x:Poly)情况下有效
            if (!((Poly) this.outline).sum(inferred)) {
                ErrorReporter.report(target, GCPErrCode.OUTLINE_MISMATCH, FAIL_SUM + this.outline);
            }
            return;
        }
        if (inferred instanceof OperateAble) {
            OperateAble you = (OperateAble) inferred;
            if (this.outline instanceof Generic) {
                if (((Generic) this.outline).node().index() < you.node().index()) {
                    you.addHasToBe(this.outline);
                }
            } else {
                ((Generic) inferred).addHasToBe(this.outline);
            }
        }
        if (!inferred.is(this.outline)) {
            ErrorReporter.report(target, GCPErrCode.OUTLINE_MISMATCH, " with " + this.outline);
        }
    }
}
