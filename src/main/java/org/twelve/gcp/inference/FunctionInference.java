package org.twelve.gcp.inference;

import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Return;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionInference implements Inference<FunctionNode> {
    @Override
    public Outline infer(FunctionNode node, Inferences inferences) {
        Generic argument = cast(node.argument().infer(inferences));
        Return returns = cast(node.body().infer(inferences));
        FirstOrderFunction function = FirstOrderFunction.from(node,argument,returns);
        return function;
    }
}
