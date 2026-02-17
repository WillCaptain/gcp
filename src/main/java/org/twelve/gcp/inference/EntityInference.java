package org.twelve.gcp.inference;

import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.common.SCOPE_TYPE;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.decorators.This;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Generic;

import java.util.ArrayList;
import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class EntityInference implements Inference<EntityNode> {
    @Override
    public Outline infer(EntityNode node, Inferences inferences) {
        Entity entity;
        node.ast().symbolEnv().current().setScopeType(SCOPE_TYPE.IN_PRODUCT_ADT);
        if (node.outline() instanceof UNKNOWN) {//第一次infer
            //infer base
            Outline base = null;
            if (node.base() != null) {
                base = node.base().infer(inferences);
                if (!(base instanceof Generic)) {
                    if (!(base instanceof ADT)) {
                        GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
                        return node.ast().Error;
                    }
                }
                node.ast().symbolEnv().defineSymbol("base", base, false, null);
            }
            if (base instanceof SYMBOL) {
                entity = new SymbolEntity(cast(base), Entity.from(node, new ArrayList<>()));
            } else {
                entity = Entity.from(node, cast(base), new ArrayList<>());
                if (base instanceof ProductADT) {
                    cloneMembers(entity, ((ProductADT) base).members());
                }
            }
        } else {//第n次infer
            entity = cast(node.outline());
        }
        node.ast().symbolEnv().current().setOutline(entity);
        //infer my members
        node.members().forEach((k, v) -> {
            Outline outline = v.infer(inferences);
//            Outline outline = v.inferLazy(inferences);
            entity.addMember(k, outline, v.modifier(), v.mutable(), v.identifier());
        });
        return entity;
    }

    private void cloneMembers(Entity entity, List<EntityMember> members) {
        List<EntityMember> ms = new ArrayList<>();
        members.forEach(m -> {
            if (m.outline() instanceof FirstOrderFunction && ((FirstOrderFunction) m.outline()).getThis() != null) {

                FirstOrderFunction func = cast(m.outline().copy());
                This t = func.getThis();
//                if (t != null) {
//                    t.setOrigin(entity);
                    ms.add(EntityMember.from(m.name(), func, m.modifier(), m.mutable() == Mutable.True, m.node(), m.isDefault()));
//                }
            }
        });
        entity.addMembers(ms);
    }
}
