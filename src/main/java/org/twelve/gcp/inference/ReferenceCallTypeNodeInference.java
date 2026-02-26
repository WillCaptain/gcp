package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.typeable.ReferenceCallTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;

import java.util.List;

public class ReferenceCallTypeNodeInference implements Inference<ReferenceCallTypeNode> {
    @Override
    public Outline infer(ReferenceCallTypeNode node, Inferencer inferencer) {
        Outline host = node.host().infer(inferencer);
        if (!inferencer.isLazy()) {
            host = host.eventual();
        }
        if (host instanceof ReferAble) {
            List<OutlineWrapper> list = node.typeNodes().stream().map(t -> new OutlineWrapper(t, t.infer(inferencer))).toList();
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
