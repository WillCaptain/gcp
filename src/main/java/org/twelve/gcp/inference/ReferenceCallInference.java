package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;

import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class ReferenceCallInference implements Inference<ReferenceCallNode>{
    @Override
    public Outline infer(ReferenceCallNode node, Inferences inferences) {
        Outline hostOutline = node.host().infer(inferences);
        if(hostOutline instanceof ReferAble){
            ReferAble referAble = cast(hostOutline);
            return referAble.project(node.types().stream().map(t->(OutlineWrapper)t.infer(inferences)).toList());
        }else {
            ErrorReporter.report(node, GCPErrCode.NOT_REFER_ABLE);
            return hostOutline;
        }
    }
}
