package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.FunctionTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

public class FunctionTypeNodeInference implements Inference<FunctionTypeNode> {
    @Override
    public Outline infer(FunctionTypeNode node, Inferences inferences) {
        Outline returns = node.returns().infer(inferences);
        FirstOrderFunction func = FirstOrderFunction.from(returns, node.arguments().stream().map(a -> a.infer(inferences)).toArray(Outline[]::new));
        return func;
    }
}
