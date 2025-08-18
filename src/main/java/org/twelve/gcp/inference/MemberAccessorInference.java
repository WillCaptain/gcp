package org.twelve.gcp.inference;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.primitive.ANY;
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
        Outline outline = node.host().infer(inferences);
        if (outline instanceof UNKNOWN) return outline;//可能需要下一次推导

        //泛化匹配
        if(outline instanceof Genericable){
            Genericable generic = cast(outline);
            Outline defined = generic.definedToBe();
            if(defined instanceof ANY){
                defined = Entity.from(node.host());
                generic.addDefinedToBe(defined);
            }
            if(defined instanceof Entity){
                return addMember(node, (Entity) defined, generic);
            }
            if(defined instanceof Poly){
                for (Outline option : ((Poly) defined).options()) {
                    if(option instanceof Entity){
                        return addMember(node, (Entity) option, generic);
                    }
                }
                Entity entity = Entity.from(node.host());
                Outline member = addMember(node, entity, generic);
                generic.addDefinedToBe(entity);
                return member;
            }
        }
        //实体匹配
        if (!(outline instanceof ProductADT)) {
            GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return node.ast().Error;
        }
        ProductADT host = cast(outline);
        host.loadMethods();//load methods when access member to avoid recursive call
        List<EntityMember> found = host.members().stream().filter(m -> m.name().equals(node.member().name())).collect(Collectors.toList());
        if (found.isEmpty()) {
            GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return node.ast().Error;
        } else {
            return found.getFirst().outline();
        }
    }

    private static Outline addMember(MemberAccessor node, Entity defined, Genericable<?,?> generic) {
        Optional<EntityMember> member = defined.members().stream().filter(m -> m.name().equals(node.member().name())).findFirst();
        if(member.isPresent()){
            return member.get().outline();
        }else {
            Entity entity = cast(generic.definedToBe());
            AccessorGeneric g = new AccessorGeneric(node);
            entity.addMember(node.member().name(), g, Modifier.PUBLIC, false, new Variable(node.member(),false,null));
            return g;
        }
    }
}