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
            Outline declared = m.declared()==null?node.ast().Any:m.declared().infer(inferences);
            EntityMember member = EntityMember.from(m.name(),declared,m.modifier(),m.mutable());
            members.add(member);
        }
        return Entity.from(node,members);
    }
}
