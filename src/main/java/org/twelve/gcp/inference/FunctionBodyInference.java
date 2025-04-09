package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Return;

public class FunctionBodyInference extends BodyInference<FunctionBody> {
    @Override
    public Outline infer(FunctionBody node, Inferences inferences) {
        Outline inferred = super.infer(node, inferences);
        Return returns = Return.from(node.parent());
        returns.addReturn(inferred);
        returns.replaceIgnores();
        return returns;
    }
}
