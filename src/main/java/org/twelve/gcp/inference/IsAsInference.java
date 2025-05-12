package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.IsAs;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.outline.Outline;

public class IsAsInference implements Inference<IsAs> {
    @Override
    public Outline infer(IsAs node, Inferences inferences) {
        Outline lhs = node.a().infer(inferences);
        if(!node.b().is(lhs)){
            ErrorReporter.report(node, GCPErrCode.TYPE_CAST_NEVER_SUCCED,node.a()+" will never be "+node.b());
        }
        if(node.parent() instanceof Arm){
            node.ast().symbolEnv().enter(((Arm) node.parent()).consequence());
            node.ast().symbolEnv().defineSymbol(node.c().token(),node.b(),false,node.c());
            node.ast().symbolEnv().exit();;
        }
        return Outline.Boolean;
    }
}
