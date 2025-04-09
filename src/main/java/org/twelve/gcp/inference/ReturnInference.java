package org.twelve.gcp.inference;

import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.outline.Outline;

public class ReturnInference implements Inference<ReturnStatement> {
    @Override
    public Outline infer(ReturnStatement node, Inferences inferences) {
        if (node.expression() == null) {
            return Outline.Unit;// return;
        } else {
            return node.expression().infer(inferences);// return ***
        }
    }
}
