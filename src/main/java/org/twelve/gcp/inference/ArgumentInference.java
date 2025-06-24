package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Token;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import static org.twelve.gcp.common.Tool.cast;

/**
 * outline as declared outline constraint of generic
 * default value outline as extended outline constraint of generic
 */
public class ArgumentInference implements Inference<Argument> {
    @Override
    public Outline infer(Argument node, Inferences inferences) {
        if (node.token() == Token.unit()) return Generic.from(node, Outline.Unit);
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol symbol = oEnv.lookupAll(node.name());
        if (symbol == null || !symbol.scope().equals(node.scope())) {
            Expression defaultValue = node.defaultValue();
            Outline outline = inferDeclared(node.declared(), inferences);
            Genericable<?, ?> generic = Generic.from(node, outline);
            if (defaultValue != null) {
                generic.addExtendToBe(defaultValue.infer(inferences));
            }
            node.ast().symbolEnv().defineSymbol(node.name(), generic, true, node);
//            node.ast().cache().add(id.id());
            return generic;
        } else {
            if (symbol.node() != node) {
                ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
            }
            return symbol.outline();
        }
    }

    private Outline inferDeclared(TypeNode declared, Inferences inferences) {
        if (declared == null) return Outline.Unknown;
        return declared.infer(inferences);
    }
}
