package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Reference;
import org.twelve.gcp.outline.projectable.Return;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionInference implements Inference<FunctionNode> {
    @Override
    public Outline infer(FunctionNode node, Inferences inferences) {
//        Map<String, Reference> references = new HashMap<>();
//        for (ReferenceNode ref : node.refs()) {
//            references.put(ref.name(),cast(ref.infer(inferences)));
//        }
        List<Reference> refs = node.refs().stream().map(r->(Reference)r.infer(inferences)).toList();
        Generic argument = cast(node.argument().infer(inferences));
        Return returns = cast(node.body().infer(inferences));
        FirstOrderFunction function = FirstOrderFunction.from(node,argument,returns,refs);
        return function;
    }
}
