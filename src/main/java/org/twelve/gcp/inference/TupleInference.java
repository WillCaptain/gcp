package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.SymbolEntity;
import org.twelve.gcp.outline.adt.SymbolTuple;
import org.twelve.gcp.outline.adt.Tuple;

import static org.twelve.gcp.common.Tool.cast;

public class TupleInference extends EntityInference{
    public Outline infer(EntityNode node, Inferencer inferencer) {
        Entity entity = cast(super.infer(node, inferencer));
        if(entity instanceof SymbolEntity){
            return new SymbolTuple(((SymbolEntity) entity).base(),new Tuple(entity));
        }else {
            return new Tuple(entity);
        }
    }
}
