package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.ExtendTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtendTypeNodeInference implements Inference<ExtendTypeNode> {
    @Override
    public Outline infer(ExtendTypeNode node, Inferencer inferencer) {
        Outline base = node.base().infer(inferencer);
        List<Reference> refs = node.references().stream().map(r -> (Reference) r.infer(inferencer)).toList();
        if (node.refCall()!=null) {
            base = node.refCall().infer(inferencer);
        }
        Entity extension = (Entity) node.extension().infer(inferencer);
        List<EntityMember> members = new ArrayList<>();
        for (EntityMember member : extension.members()) {
            if (base instanceof ProductADT product) {
                Optional<EntityMember> old = product.getMember(member.name());
                if (old.isPresent()) {
                    EntityMember merged = ProductADT.mergeMember(old.get(), member, node);
                    if (merged != null && merged != old.get()) {
                        members.add(merged);
                    }
                    continue;
                }
            }
            members.add(member);
        }
        return Entity.from(node, base, members, refs);
    }

//    private void cloneMembers(Entity entity, List<EntityMember> members) {
//        List<EntityMember> ms = new ArrayList<>();
//        members.forEach(m -> {
//            Outline o = m.outline().copy();
//            ms.add(EntityMember.from(m.name(), o, m.modifier(), m.mutable() == Mutable.True, m.node(), m.isDefault()));
//        });
//        entity.addMembers(ms);
//    }
}
