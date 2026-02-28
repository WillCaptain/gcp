package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.primitive.Literal;
import org.twelve.gcp.outline.projectable.Reference;

public class EntityTypeNodeInference implements Inference<EntityTypeNode> {
    @Override
    public Outline infer(EntityTypeNode node, Inferencer inferencer) {

        return this.infer(node, inferencer, node.ast().Any);
    }

    public Outline infer(EntityTypeNode node, Inferencer inferencer, Outline base) {
        node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
        Entity entity = Entity.fromRefs(node, node.refs().stream().map(r -> (Reference) r.infer(inferencer)).toList());
        node.ast().symbolEnv().current().setOutline(entity);

        Inferencer sessionInferencer = new OutlineInferencer(true);
        for (Variable m : node.members()) {
            Node defaultValueNode = node.getDefault(m.name());
            Outline declared;
            if (defaultValueNode != null) {
                // `alias: "alice"` — field has String type and a default string value
                declared = node.ast().String;
                entity.addMemberWithDefault(m.name(), declared, m.modifier(), m.mutable(), m, defaultValueNode);
            } else {
                declared = m.declared() == null ? node.ast().Any : m.declared().infer(sessionInferencer);
                // literal-type fields (e.g. issuer: #"GCP-System") are auto-filled — mark isDefault=true
                boolean isLiteralField = declared instanceof Literal;
                entity.addMember(m.name(), declared, m.modifier(), m.mutable(), m, isLiteralField);
            }
        }

        return entity;
    }
}
