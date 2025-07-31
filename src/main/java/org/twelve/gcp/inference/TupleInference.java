package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.Tuple;

import static org.twelve.gcp.common.Tool.cast;

public class TupleInference extends EntityInference{
    public Outline infer(EntityNode node, Inferences inferences) {
        Entity entity = cast(super.infer(node,inferences));
        return new Tuple(entity);
    }
}
