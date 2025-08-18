package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Reference;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class ReferenceNodeInference implements Inference<ReferenceNode> {
    @Override
    public Outline infer(ReferenceNode node, Inferences inferences) {
        Outline outline = Reference.from(node, node.declared() == null ? null : node.declared().infer(inferences));
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol symbol = oEnv.current().lookupOutline(node.name());//only check my scope
        if (symbol == null) {
            oEnv.defineOutline(node.name(), outline, node);
//            return outline;
        } else {
            if (symbol.node() != node) {
                GCPErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
                return node.ast().Error;
            }
            outline = symbol.outline();
        }
        return outline;
    }
}
