package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.node.unpack.TupleUnpackNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Tuple;

import java.util.ArrayList;
import java.util.List;

public class TupleUnpackNodeInference implements Inference<TupleUnpackNode>{
    @Override
    public Outline infer(TupleUnpackNode node, Inferences inferences) {
        return new Tuple(convertEntity(node.begins(),node.ends(),inferences));
    }
    private static Entity convertEntity(List<Node> begins, List<Node> ends, Inferences inferences) {
        AST ast = begins.isEmpty()?ends.getFirst().ast() : begins.getFirst().ast();;
        List<EntityMember> members = new ArrayList<>();
//        if(!begins.isEmpty()){
            for (int i = 0; i < begins.size(); i++) {
                members.add(EntityMember.from(String.valueOf(i),begins.get(i).infer(inferences), Modifier.PUBLIC,false));
            }
//        }
//        if(!ends.isEmpty()){
            for (int i = 0; i < ends.size(); i++) {
                members.add(EntityMember.from(String.valueOf(i-ends.size()),ends.get(i).infer(inferences), Modifier.PUBLIC,false));
            }
//        }
        return Entity.from(ast,members);
    }
}
