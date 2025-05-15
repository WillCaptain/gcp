package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.accessor.Accessor;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

/**
 * look up symbol environment to find the symbol outline
 */
public class IdentifierInference implements Inference<Identifier> {
    @Override
    public Outline infer(Identifier node, Inferences inferences) {
//        if (!(node.outline() instanceof UNKNOWN)) return node.outline();
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol supposed;
        if(node.parent() instanceof FunctionCallNode || node.parent() instanceof Accessor) {
            supposed = oEnv.lookupAll(node.name());
        }else{
            supposed = oEnv.lookupSymbol(node.name());
        }
        if (supposed == null) {
            ErrorReporter.report(node, GCPErrCode.VARIABLE_NOT_DEFINED);
            return Outline.Unknown;
        }
        if(supposed.outline() instanceof UNKNOWN){
            if(this.confirmRecursive(node,supposed.node())){
                return Outline.Pending;
            }
        }
        return supposed.outline();
    }

    private boolean confirmRecursive(Identifier me, Identifier source) {
        if(!(source.parent() instanceof Assignment)) return false;
        Expression rhs = ((Assignment) source.parent()).rhs();
        Node parent = me.parent();
        while(parent!=me.ast().program()){
            if(parent==rhs) return true;
            parent = parent.parent();
        }
        return false;
    }
}
