package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class OutlineDefinitionInference implements Inference<OutlineDefinition> {
    @Override
    public Outline infer(OutlineDefinition node, Inferencer inferencer) {
        SymbolIdentifier symbolNode = node.symbolNode();
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        Outline rhs = node.typeNode().infer(inferencer);

        EnvSymbol symbol = oEnv.current().lookupSymbol(symbolNode.name());//only check my scope
        if (symbol != null && symbol.node() != node.symbolNode()) {
            GCPErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
            return node.ast().Ignore;
        }
        oEnv.defineSymbol(node.symbolNode().name(), symbolNode.merge(rhs, inferencer), false, node.symbolNode());
        node.symbolNode().infer(inferencer);
        return node.ast().Ignore;
    }
}