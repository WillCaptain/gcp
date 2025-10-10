package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.unpack.TupleUnpackNode;
import org.twelve.gcp.node.unpack.UnpackNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.unpack.Unpack;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;

public class VariableInference implements Inference<Variable> {
    @Override
    public Outline infer(Variable node, Inferences inferences) {
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol symbol = oEnv.current().lookupSymbol(node.name());//only check my scope
        if (symbol == null) {
            if(node.identifier() instanceof UnpackNode){
                for (Identifier id : ((UnpackNode) node.identifier()).identifiers()) {
                    oEnv.defineSymbol(id.name(), node.ast().Unknown, false, id);
                }
                return new Unpack(cast(node.identifier()));
            }else {
                Outline outline = inferDeclared(node.declared(), inferences, node.ast());
                oEnv.defineSymbol(node.name(), outline, node.mutable(), node);
                return outline;
            }
        } else {
//            if (symbol.node() != node && !(node.parent().parent() instanceof EntityNode)) {
            if (symbol.node() != node) {
                GCPErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
                return node.ast().Error;
            }
            return symbol.outline();
        }
    }

    private Outline inferDeclared(TypeNode declared, Inferences inferences, AST ast) {
        if (declared == null) return ast.Unknown;
        return declared.infer(inferences);
    }
}
