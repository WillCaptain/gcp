package org.twelve.gcp.inference;

import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.node.expression.typeable.ExtendTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.decorators.This;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.ArrayList;
import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class ExtendTypeNodeInference implements Inference<ExtendTypeNode>{
    @Override
    public Outline infer(ExtendTypeNode node, Inferences inferences) {
        Outline base = node.base().infer(inferences);
        Entity entity = cast(node.extension().infer(inferences));
//        Entity entity = Entity.from(node, extend.members());
        if (base instanceof ProductADT) {
            cloneMembers(entity, ((ProductADT) base).members());
        }
        return entity;
    }

    private void cloneMembers(Entity entity, List<EntityMember> members) {
        List<EntityMember> ms = new ArrayList<>();
        members.forEach(m -> {
            Outline o = m.outline().copy();
//            if (m.outline() instanceof FirstOrderFunction && ((FirstOrderFunction) m.outline()).getThis()!=null) {
//
//                FirstOrderFunction func = cast(m.outline().copy());
//                o = func;
//                This t = func.getThis();
//                if(t!=null) {
//                    t.setOrigin(entity);
//                }
//            }
            ms.add(EntityMember.from(m.name(), o, m.modifier(), m.mutable() == Mutable.True, m.node(), m.isDefault()));
        });
        entity.addMembers(ms);
    }
}
