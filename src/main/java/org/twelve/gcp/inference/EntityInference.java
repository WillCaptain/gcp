package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;

import java.util.ArrayList;

import static org.twelve.gcp.common.Tool.cast;

public class EntityInference implements Inference<EntityNode> {
    @Override
    public Outline infer(EntityNode node, Inferences inferences) {
        Entity entity;
        if (node.outline() == Outline.Unknown) {//第一次infer
            //infer base
            Outline base = null;
            if (node.base() != null) {
                base = node.base().infer(inferences);
                if (!(base instanceof ProductADT)) {
                    ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
                    return Outline.Error;
                }
            }
            entity = Entity.from(node, cast(base), new ArrayList<>());
        } else {//第n次infer
            entity = cast(node.outline());
        }
        //infer my members
        node.members().forEach((k, vs) -> {
            for (MemberNode v : vs) {
                Outline outline = v.infer(inferences);
//                if(v.inferred()) {
                entity.addMember(k, outline, v.modifier(), v.mutable(), v);
//                }
            }
        });
        return entity;
    }
}
