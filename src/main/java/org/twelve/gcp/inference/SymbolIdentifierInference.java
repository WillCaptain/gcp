package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class SymbolIdentifierInference implements Inference<SymbolIdentifier> {
    @Override
    public Outline infer(SymbolIdentifier node, Inferences inferences) {
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol supposed = oEnv.lookupSymbol(node.name());
        if (supposed == null) {
            Outline outline = new SYMBOL(node.name(), node.ast());
            oEnv.defineSymbol(node.name(), outline, false, node);
            return outline;
        } else {
            return supposed.outline();
        }
    }
}
