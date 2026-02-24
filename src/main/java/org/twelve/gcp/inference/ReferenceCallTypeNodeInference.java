package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.typeable.ReferenceCallTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class ReferenceCallTypeNodeInference implements Inference<ReferenceCallTypeNode> {
    @Override
    public Outline infer(ReferenceCallTypeNode node, Inferences inferences) {
        Outline host = node.host().infer(inferences);
        if (!inferences.isLazy()) {
            host = host.eventual();
        }
        if (host instanceof ReferAble) {
            List<OutlineWrapper> list = node.typeNodes().stream().map(t -> new OutlineWrapper(t, t.infer(inferences))).toList();
            host = ((ReferAble) host).project(list);
        }else {
            GCPErrorReporter.report(node, GCPErrCode.NOT_REFER_ABLE);
        }
//        if(host instanceof ProductADT){
//            host.updateThis(cast(host));
//        }
        return host;
    }
}
