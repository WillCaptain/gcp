package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.SumADT;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.projectable.Return;
import org.twelve.gcp.outline.projectable.Returnable;

public class FunctionBodyInference extends BodyInference<FunctionBody> {
    @Override
    public Outline infer(FunctionBody node, Inferencer inferencer) {
        Outline inferred = super.infer(node, inferencer);
        if (inferred instanceof IGNORE) inferred = node.ast().Unit;
        if (inferred.containsIgnore()) {
            inferred = replaceIgnoreWithNothing(inferred, node.ast());
        }
        Returnable returns = Return.from(node.parent());
        returns.addReturn(inferred);
//        returns.replaceIgnores();
        return returns;
    }

    private Outline replaceIgnoreWithNothing(Outline outline, org.twelve.gcp.ast.AST ast) {
        if (!(outline instanceof SumADT sum) || !outline.containsIgnore()) {
            return outline;
        }
        Outline[] normalized = sum.options().stream()
                .map(option -> option instanceof IGNORE ? ast.Nothing : option)
                .toArray(Outline[]::new);
        return Option.from(sum.node(), ast, normalized);
    }
}
