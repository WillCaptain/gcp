package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.typeable.ReferenceAliasTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.SumADT;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.List;

public class ReferenceAliasTypeNodeInference implements Inference<ReferenceAliasTypeNode> {
    @Override
    public Outline infer(ReferenceAliasTypeNode node, Inferencer inferencer) {
        List<Reference> refs = node.refs().stream().map(r -> (Reference) r.infer(inferencer)).toList();
        Outline body = node.body().infer(inferencer);
        if (body instanceof SumADT sum) {
            sum.setReferences(refs);
        } else {
            GCPErrorReporter.report(node, GCPErrCode.NOT_REFER_ABLE,
                    "generic alias body must be referable");
        }
        return body;
    }
}
