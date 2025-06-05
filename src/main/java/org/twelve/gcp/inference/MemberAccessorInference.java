package org.twelve.gcp.inference;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.projectable.AccessorGeneric;
import org.twelve.gcp.outline.projectable.Genericable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class MemberAccessorInference implements Inference<MemberAccessor> {
    @Override
    public Outline infer(MemberAccessor node, Inferences inferences) {
        Outline outline = node.entity().infer(inferences);
        if (outline instanceof UNKNOWN) return outline;//可能需要下一次推导

        //泛化匹配
        if(outline instanceof Genericable){
            Genericable generic = cast(outline);
            if(generic.definedToBe() instanceof ANY){
                generic.addDefinedToBe(Entity.from(node.entity()));
            }
            Optional<EntityMember> member = ((Entity) generic.definedToBe()).members().stream().filter(m -> m.name().equals(node.member().name())).findFirst();
           if(member.isPresent()){
               return member.get().outline();
           }else {
               Entity entity = cast(generic.definedToBe());
               AccessorGeneric g = new AccessorGeneric(node);
//               Generic g = new Generic(node);
               entity.addMember(node.member().name(), g, Modifier.PUBLIC, false, new Variable(node.member(),false,null));
               return g;
           }
        }
        //实体匹配
        if (!(outline instanceof ProductADT)) {
            ErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return Outline.Error;
        }
        ProductADT entity = cast(outline);
        List<EntityMember> found = entity.members().stream().filter(m -> m.name().equals(node.member().name())).collect(Collectors.toList());
        if (found.isEmpty()) {
            ErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return Outline.Error;
        } else {
            return found.getFirst().outline();
        }
//        if(found.size()==1){
//            return found.get(0).outline();
//        }else {
//            return new Overwrite(found.stream().map(m -> (Function)m.outline()).collect(Collectors.toList()));
//        }
    }
}