package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class VariableInference implements Inference<Variable> {
    @Override
    public Outline infer(Variable node, Inferences inferences) {
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol symbol = oEnv.current().lookupSymbol(node.name());//only check my scope
        if(symbol==null ) {
            Outline outline = inferDeclared(node.declared(),inferences);
            oEnv.defineSymbol(node.name(), outline, node.mutable(), node);
            return outline;
        }else{
//            if (symbol.node() != node && !(node.parent().parent() instanceof EntityNode)) {
            if (symbol.node() != node) {
                ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
                return Outline.Error;
            }
            return symbol.outline();
        }
    }

    private Outline inferDeclared(TypeNode declared, Inferences inferences) {
        if(declared==null) return Outline.Unknown;
        return declared.infer(inferences);
    }
}
