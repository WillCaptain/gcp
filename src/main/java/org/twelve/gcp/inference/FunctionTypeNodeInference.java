package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.FunctionTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.*;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionTypeNodeInference implements Inference<FunctionTypeNode> {
    @Override
    public Outline infer(FunctionTypeNode node, Inferencer inferencer) {
        List<Reference> refs = node.refs().stream().map(r->(Reference)r.infer(inferencer)).toList();
        Outline inferred = node.returns().infer(inferencer);
        Returnable ret;
        if(inferred instanceof Reference){
            ret = cast(inferred);
        }else {
            ret = Return.from(node.ast(),node.returns().infer(inferencer));
            ret.addReturn(ret.declaredToBe());
        }
//        return HigherOrderFunction.from(node, ret, node.arguments().stream().map(a -> a.infer(inferences)).toArray(Outline[]::new));
        return FirstOrderFunction.from(node.ast(), refs,ret, node.arguments().stream().map(a -> a.infer(inferencer)).toArray(Outline[]::new));
    }
}
