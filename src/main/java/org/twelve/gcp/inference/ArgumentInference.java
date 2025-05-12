package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outlineenv.EnvSymbol;

/**
 * outline as declared outline constraint of generic
 * default value outline as extended outline constraint of generic
 */
public class ArgumentInference implements Inference<Argument> {
    @Override
    public Outline infer(Argument node, Inferences inferences) {
        Identifier id = node.identifier();
        EnvSymbol symbol = node.ast().symbolEnv().lookupAll(id.token());
        if (symbol == null) {
            Expression defaultValue = node.defaultValue();
            Generic generic = Generic.from(node, id.outline());
            if (defaultValue != null) {
                generic.addExtendToBe(defaultValue.infer(inferences));
            }
            node.ast().symbolEnv().defineSymbol(id.token(), generic, true, id);
            node.ast().cache().add(id.id());
            return generic;
        } else {
            if(!node.ast().cache().contains(id.id())){
                ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
            }
            return symbol.outline();
        }
    }
}
