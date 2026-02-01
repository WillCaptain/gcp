package org.twelve.gcp.inference;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.node.unpack.EntityUnpackNode;
import org.twelve.gcp.node.unpack.Field;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;

public class EntityUnpackNodeInference implements Inference<EntityUnpackNode>{
    @Override
    public Outline infer(EntityUnpackNode node, Inferences inferences) {
        Entity entity = Entity.from(node);
        for (Field field : node.fields()) {
            entity.addMember(field.field().name(),field.infer(inferences), Modifier.PUBLIC, false, field.field());
        }
        return entity;
    }
}
