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

/**
 * Type inference for member-access expressions (e.g. {@code obj.field} or {@code agg.avg}).
 *
 * <h2>Two Inference Paths</h2>
 * <ul>
 *   <li><b>Generic path (Genericable host)</b>: the host type is not yet known (e.g. a lambda parameter
 *       such as {@code agg}). A new Entity member is added dynamically to {@code definedToBe},
 *       and an {@link AccessorGeneric} placeholder is returned for further constraint narrowing.</li>
 *   <li><b>Entity path (ProductADT host)</b>: the host type is fully known.
 *       The member is looked up directly in the member list and its Outline type is returned.</li>
 * </ul>
 *
 * <h2>Role of AccessorGeneric</h2>
 * When a member is accessed via the generic path and its type is not yet known, an
 * {@link AccessorGeneric} placeholder is created. Subsequent function-call projection in
 * {@link FunctionCallInference} will validate the member's actual type via the {@code hasToBe} constraint.
 *
 * @author huizi 2025
 */
public class MemberAccessorInference implements Inference<MemberAccessor> {
    @Override
    public Outline infer(MemberAccessor node, Inferencer inferencer) {
        Outline outline = node.host().infer(inferencer);
        if (outline instanceof UNKNOWN) return outline; // host not yet inferred; wait for the next pass

        // Generic path: host is a type variable (e.g. a lambda parameter); extend its Entity constraint dynamically
        if(outline instanceof Genericable){
            Genericable generic = cast(outline);
            Outline defined = generic.definedToBe();
            if(defined instanceof ANY){
                // First member access: establish an Entity structural constraint for the type variable
                defined = Entity.from(node.host());
                generic.addDefinedToBe(defined);
            }
            if(defined instanceof Entity){
                return addMember(node, (Entity) defined, generic);
            }
            if(defined instanceof Poly){
                // Poly case: find the Entity option within the union type
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
        // Entity path: host type is fully known; look up the member directly
        if (!(outline instanceof ProductADT)) {
            GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return node.ast().Error;
        }
        ProductADT host = cast(outline);
        if(host instanceof Entity){
            host.updateThis(host);
        }
        // Lazy-load built-in methods here to avoid recursive triggering before member access
        host.loadBuiltInMethods();
        host = cast(host.eventual());

        List<EntityMember> found = host.members().stream().filter(m -> m.name().equals(node.member().name())).collect(Collectors.toList());
        if (found.isEmpty()) {
            GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return node.ast().Error;
        } else {
            return found.getFirst().outline().eventual();
//            if(result instanceof Genericable<?,?>){
//                return ((Genericable<?, ?>) result).guess();//todo:i'm guessing...
//            }else{
//                return result;
//            }
        }
    }

    /**
     * Looks up or creates a member on the generic path.
     * <p>
     * If the member already exists (inferred in a previous pass), its type is returned directly.
     * Otherwise, an {@link AccessorGeneric} placeholder is created and registered in the Entity,
     * waiting for {@link FunctionCallInference} to fill in the concrete constraint via {@code hasToBe}.
     *
     * @param node    the member-access node
     * @param defined the Entity constraint of the host
     * @param generic the Genericable type of the host
     * @return the Outline type of the member
     */
    private static Outline addMember(MemberAccessor node, Entity defined, Genericable<?,?> generic) {
        Optional<EntityMember> member = defined.members().stream().filter(m -> m.name().equals(node.member().name())).findFirst();
        if(member.isPresent()){
            return member.get().outline();
        }else {
            Entity entity = cast(generic.min());
            AccessorGeneric g = new AccessorGeneric(node);
            entity.addMember(node.member().name(), g, Modifier.PUBLIC, false, new Variable(node.member(),false,null));
            return g;
        }
    }
}