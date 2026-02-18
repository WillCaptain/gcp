package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.typeable.ExtendTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.ReferAble;
import org.twelve.gcp.outline.projectable.Reference;

import java.util.List;

public class ExtendTypeNodeInference implements Inference<ExtendTypeNode> {
    @Override
    public Outline infer(ExtendTypeNode node, Inferences inferences) {
        Outline base = node.base().infer(inferences);
        List<Reference> refs = node.references().stream().map(r -> (Reference) r.infer(inferences)).toList();
        if (node.refCall()!=null) {
            base = node.refCall().infer(inferences);
        }
        return Entity.from(node, base, ((Entity) node.extension().infer(inferences)).members(), refs);
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
