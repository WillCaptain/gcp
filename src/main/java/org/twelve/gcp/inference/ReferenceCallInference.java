package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;

import static org.twelve.gcp.common.Tool.*;

public class ReferenceCallInference implements Inference<ReferenceCallNode>{
    @Override
    public Outline infer(ReferenceCallNode node, Inferences inferences) {
        if(inferences.isLazy() && isInFunction(node) && isInMember(node)) {
            return new Lazy(node,node.ast().inferences());
        }

        Outline hostOutline = cast(node.host().infer(inferences));
        if(hostOutline instanceof ReferAble){
            ReferAble referAble = cast(((ReferAble)hostOutline).copy());
            return referAble.project(node.types().stream().map(t->new OutlineWrapper(node,t.infer(inferences))).toList());
        }else {
            GCPErrorReporter.report(node, GCPErrCode.NOT_REFER_ABLE);
            return hostOutline;
        }
    }

    private Outline handleExternal(ReferenceCallNode node, Inferences inferences) {
        return node.types().getFirst().infer(inferences);
    }
}
