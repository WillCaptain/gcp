package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;

import java.util.ArrayList;
import java.util.List;

public class EntityTypeNodeInference implements Inference<EntityTypeNode>{
    @Override
    public Outline infer(EntityTypeNode node, Inferences inferences) {
        List<EntityMember> members = new ArrayList<>();
        for (Variable m : node.members()) {
            EntityMember member = EntityMember.from(m.name(),m.declared().infer(inferences),m.modifier(),m.mutable());
            members.add(member);
        }
        return Entity.from(members);
    }
}
