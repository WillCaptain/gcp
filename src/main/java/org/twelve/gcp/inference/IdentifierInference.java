package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

/**
 * look up symbol environment to find the symbol outline
 */
public class IdentifierInference implements Inference<Identifier> {
    @Override
    public Outline infer(Identifier node, Inferences inferences) {
//        if (node.isDeclared()) return node.outline();
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol supposed = oEnv.lookup(node.token());
        if (supposed == null) {
            ErrorReporter.report(node, GCPErrCode.VARIABLE_NOT_DEFINED);
            return Outline.Nothing;
        }
        return supposed.outline();
    }


}
