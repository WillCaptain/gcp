package org.twelve.gcp.inference;

import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.outline.Outline;

public class ReturnInference implements Inference<ReturnStatement> {
    @Override
    public Outline infer(ReturnStatement node, Inferencer inferencer) {
        if (node.expression() == null) {
            return node.ast().Unit;// return;
        } else {
            return node.expression().infer(inferencer);// return ***
        }
    }
}
