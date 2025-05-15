package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
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
            Outline outline = inferDeclared(oEnv,node.getDeclared(),inferences);
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

    private Outline inferDeclared(LocalSymbolEnvironment oEnv, Expression declared, Inferences inferences) {
        if(declared instanceof Identifier){
            return oEnv.lookupOutline(((Identifier) declared).name()).outline();
        }else {
            return declared.infer(inferences);
        }
    }
}
