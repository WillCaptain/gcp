package org.twelve.gcp.inference;

import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.*;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionInference implements Inference<FunctionNode> {
    @Override
    public Outline infer(FunctionNode node, Inferencer inferencer) {
        List<Reference> refs = node.refs().stream().map(r->(Reference)r.infer(inferencer)).toList();
        Genericable<?,?> argument = cast(node.argument().infer(inferencer));
        Returnable returns = cast(node.body().infer(inferencer));
        return FirstOrderFunction.from(node,argument,returns,refs);
    }
}
