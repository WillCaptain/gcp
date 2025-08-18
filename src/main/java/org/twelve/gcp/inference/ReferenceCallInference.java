package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;

import static org.twelve.gcp.common.Tool.cast;

public class ReferenceCallInference implements Inference<ReferenceCallNode>{
    @Override
    public Outline infer(ReferenceCallNode node, Inferences inferences) {
        Outline func = cast(node.host().infer(inferences));
        if(func instanceof ReferAble){
            ReferAble referAble = cast(((ReferAble)func).copy());
//            ReferAble referAble = cast(func);
            return referAble.project(node.types().stream().map(t->new OutlineWrapper(node,t.infer(inferences))).toList());
        }else {
            GCPErrorReporter.report(node, GCPErrCode.NOT_REFER_ABLE);
            return func;
        }
    }
}
