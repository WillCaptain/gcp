package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.FunctionTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.HigherOrderFunction;
import org.twelve.gcp.outline.projectable.Return;

public class FunctionTypeNodeInference implements Inference<FunctionTypeNode> {
    @Override
    public Outline infer(FunctionTypeNode node, Inferences inferences) {
        Return returns = Return.from(node.returns().infer(inferences));
        returns.addReturn(returns.declaredToBe());
        return HigherOrderFunction.from(node, returns, node.arguments().stream().map(a -> a.infer(inferences)).toArray(Outline[]::new));
    }
}
