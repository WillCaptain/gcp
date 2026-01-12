package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.Assignment;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;

/**
 * add identifier into symbol environment as unknown outline before the inference
 */
public class VariableDeclaratorInference implements Inference<VariableDeclarator> {
    @Override
    public Outline infer(VariableDeclarator node, Inferences inferences) {
        for (Assignment assignment : node.assignments()) {
            assignment.infer(inferences);
        }
        return node.ast().Ignore;
    }
}
