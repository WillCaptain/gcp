package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.IsAs;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Genericable;

public class IsAsInference implements Inference<IsAs> {
    @Override
    public Outline infer(IsAs node, Inferencer inferencer) {
        Outline lhs = node.a().infer(inferencer);
        Outline b = node.b().infer(inferencer);
        if (!b.canBe(lhs)) {
            GCPErrorReporter.report(node, GCPErrCode.TYPE_CAST_NEVER_SUCCEED, node.a() + " will never be " + node.b());
        } else {
            if (lhs instanceof Genericable<?, ?>) {
//                ((Genericable<?,?>) lhs).addCouldBe(node.b());//its a hint to indicate it is possible to be
            }
        }
        //if(node.parent() instanceof Arm){
        //node.ast().symbolEnv().enter(((Arm) node.parent()).consequence());
        node.ast().symbolEnv().defineSymbol(node.c().name(), b, false, node.c());
        node.c().infer(inferencer);
        //node.ast().symbolEnv().exit();;
        //}
        return node.ast().Boolean;
    }
}
