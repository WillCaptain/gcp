package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

/**
 * outline as declared outline constraint of generic
 * default value outline as extended outline constraint of generic
 */
public class ArgumentInference implements Inference<Argument> {
    @Override
    public Outline infer(Argument node, Inferences inferences) {
        if (node.token() == Token.unit()) return Generic.from(node, node.ast().Unit);
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol symbol = oEnv.lookupAll(node.name());
        if (symbol == null || !symbol.scope().equals(node.scope())) {
            Expression defaultValue = node.defaultValue();
            Outline outline = inferDeclared(node.declared(), inferences, node.ast());
            Genericable<?, ?> generic = Generic.from(node, outline);
            if (defaultValue != null) {
                generic.addExtendToBe(defaultValue.infer(inferences));
            }
            node.ast().symbolEnv().defineSymbol(node.name(), generic, true, node);
//            node.ast().cache().add(id.id());
            return generic;
        } else {
            if (symbol.node() != node) {
                GCPErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
            }
            return symbol.outline();
        }
    }

    private Outline inferDeclared(TypeNode declared, Inferences inferences, AST ast) {
        if (declared == null) return ast.Unknown;
        return declared.infer(inferences);
    }
}
