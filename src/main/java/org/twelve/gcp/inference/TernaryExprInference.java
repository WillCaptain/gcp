package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.TernaryExpression;
import org.twelve.gcp.outline.Outline;

public class TernaryExprInference implements Inference<TernaryExpression>{
    @Override
    public Outline infer(TernaryExpression node, Inferences inferences) {
        if(node.condition().infer(inferences)!= node.ast().Boolean){
            GCPErrorReporter.report(node.condition(), GCPErrCode.CONDITION_IS_NOT_BOOL);
        }
        Outline a = node.trueBranch().infer(inferences);
        Outline b = node.falseBranch().infer(inferences);
        if(a.is(b)) return a;
        if(b.is(a)) return b;
        GCPErrorReporter.report(node.falseBranch(), GCPErrCode.OUTLINE_MISMATCH);
        return node.ast().Error;
    }
}
