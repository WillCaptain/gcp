package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionInference implements Inference<FunctionNode> {
    @Override
    public Outline infer(FunctionNode node, Inferences inferences) {
        List<Reference> refs = node.refs().stream().map(r->(Reference)r.infer(inferences)).toList();
        Genericable<?,?> argument = cast(node.argument().infer(inferences));
        Returnable returns = cast(node.body().infer(inferences));
        return FirstOrderFunction.from(node,argument,returns,refs);
    }
}
