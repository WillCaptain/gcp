package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.TernaryExpression;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

public class TernaryExprInference implements Inference<TernaryExpression>{
    @Override
    public Outline infer(TernaryExpression node, Inferences inferences) {
        if(node.condition().infer(inferences)!= ProductADT.Boolean){
            ErrorReporter.report(node.condition(), GCPErrCode.CONDITION_IS_NOT_BOOL);
        }
        Outline a = node.trueBranch().infer(inferences);
        Outline b = node.falseBranch().infer(inferences);
        if(a.is(b)) return a;
        if(b.is(a)) return b;
        ErrorReporter.report(node.falseBranch(), GCPErrCode.OUTLINE_MISMATCH);
        return ProductADT.Error;
    }
}
