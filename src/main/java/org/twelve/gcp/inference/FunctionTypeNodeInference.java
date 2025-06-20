package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.FunctionTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.HigherOrderFunction;
import org.twelve.gcp.outline.projectable.Reference;
import org.twelve.gcp.outline.projectable.Return;
import org.twelve.gcp.outline.projectable.Returnable;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionTypeNodeInference implements Inference<FunctionTypeNode> {
    @Override
    public Outline infer(FunctionTypeNode node, Inferences inferences) {
        Outline inferred = node.returns().infer(inferences);
        Returnable ret;
        if(inferred instanceof Reference){
            ret = cast(inferred);
        }else {
            ret = Return.from(node.returns().infer(inferences));
            ret.addReturn(ret.declaredToBe());
        }
        return HigherOrderFunction.from(node, ret, node.arguments().stream().map(a -> a.infer(inferences)).toArray(Outline[]::new));
    }
}
