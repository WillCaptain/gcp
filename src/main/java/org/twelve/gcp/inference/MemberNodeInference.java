package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.Assignment;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.Outline;

public class MemberNodeInference implements Inference<MemberNode> {
    @Override
    public Outline infer(MemberNode node, Inferences inferences) {
        for (Assignment assignment : node.assignments()) {
            assignment.infer(new OutlineInferences(true));
        }
        return node.identifier().outline();
    }
}
