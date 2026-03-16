package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.Assignment;
import org.twelve.gcp.node.expression.TupleNode;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.Outline;

public class MemberNodeInference implements Inference<MemberNode> {
    // Shared lazy-mode inferencer for all entity member nodes — stateless, safe to reuse.
    private static final Inferencer LAZY_INFERENCER = new OutlineInferencer(true);

    @Override
    public Outline infer(MemberNode node, Inferencer inferencer) {
        // Tuple-element MemberNodes are structural scaffolding wrapping plain value expressions.
        // They must be inferred with the caller's inferencer (never forced into a lazy context)
        // so that multi-pass type propagation works correctly for nested HOF chains like
        // schools.filter(s->s.students().sum(t->t.age)>80).count().
        boolean isTupleElement = node.parent() instanceof TupleNode;
        Inferencer effectiveInferencer = isTupleElement ? inferencer : LAZY_INFERENCER;
        for (Assignment assignment : node.assignments()) {
            assignment.infer(effectiveInferencer);
        }
        return node.identifier().outline();
    }
}
