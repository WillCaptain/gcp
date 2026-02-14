package org.twelve.gcp.inference;

import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.projectable.Reference;

public class EntityTypeNodeInference implements Inference<EntityTypeNode> {
    @Override
    public Outline infer(EntityTypeNode node, Inferences inferences) {

        return this.infer(node, inferences, node.ast().Any);
    }

    public Outline infer(EntityTypeNode node, Inferences inferences, Outline base) {
        node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
        Entity entity = Entity.fromRefs(node, node.refs().stream().map(r -> (Reference) r.infer(inferences)).toList());
        node.ast().symbolEnv().current().setOutline(entity);

        Inferences sessionInferences = new OutlineInferences(true);
        for (Variable m : node.members()) {
            Outline declared = m.declared() == null ? node.ast().Any : m.declared().infer(sessionInferences);
            entity.addMember(m.name(), declared, m.modifier(), m.mutable(), m);
        }

        return entity;
    }
}
