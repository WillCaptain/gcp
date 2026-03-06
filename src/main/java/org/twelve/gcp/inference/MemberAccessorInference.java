package org.twelve.gcp.inference;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.ThisNode;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.projectable.AccessorGeneric;
import org.twelve.gcp.outline.projectable.Genericable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // Option path: if every variant in the union has the requested member, return the common
        // member type (or a union of them when they differ). This enables structural member access
        // on sum types — e.g. (EntityA|EntityB).field when both variants declare the same field.
        if (outline instanceof Option optType) {
            return inferMemberFromOption(node, optType);
        }

        // A bare type name (SymbolIdentifier, e.g. Human, Gender) is an outline type definition,
        // not a value — accessing its members is forbidden.
        // EXCEPTION: stdlib singleton modules (Math, Date, Console) are also SymbolIdentifiers
        // but are registered as entity VALUES (their symbol's origin node is null, meaning they
        // were pre-defined by the runtime rather than declared in user source code).
        if (node.host() instanceof SymbolIdentifier si) {
            var sym = node.ast().symbolEnv().lookupAll(si.name());
            boolean isStdlibValue = sym != null && sym.node() == null && outline instanceof ProductADT;
            if (!isStdlibValue) {
                GCPErrorReporter.report(node.host(), GCPErrCode.OUTLINE_USED_AS_VALUE);
                return node.ast().Error;
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

        Optional<EntityMember> found = host.getMember(node.member().name());
        if (found.isEmpty()) {
            GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
            return node.ast().Error;
        } else {
            // Protected-member check: _-prefixed members (PRIVATE modifier) are only accessible
            // via 'this' (within the entity itself or its derived types).
            // Accessing them through an external reference (e.g. a._age) is forbidden.
            if (found.get().modifier() == Modifier.PRIVATE
                    && !(node.host() instanceof ThisNode)) {
                GCPErrorReporter.report(node.member(), GCPErrCode.NOT_ACCESSIBLE);
                return node.ast().Error;
            }
            return found.get().outline().eventual();
//            if(result instanceof Genericable<?,?>){
//                return ((Genericable<?, ?>) result).guess();//todo:i'm guessing...
//            }else{
//                return result;
//            }
        }
    }

    /**
     * Handles member access on an {@link Option} (union) type.
     * <p>
     * A field access {@code expr.field} is valid on a union type when <em>every</em> variant
     * in the union carries the field. The result type is the union of all per-variant field types,
     * collapsed to a single type when they are all identical.
     * <p>
     * If any variant is not a {@link ProductADT}, or does not carry the field, the access is
     * reported as {@code FIELD_NOT_FOUND}.
     */
    private static Outline inferMemberFromOption(MemberAccessor node, Option optType) {
        List<Outline> memberTypes = new ArrayList<>();
        for (Outline option : optType.options()) {
            Outline opt = option.eventual();
            if (!(opt instanceof ProductADT)) {
                GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
                return node.ast().Error;
            }
            ProductADT adt = cast(opt);
            adt.loadBuiltInMethods();
            Optional<EntityMember> found = adt.getMember(node.member().name());
            if (found.isEmpty()) {
                GCPErrorReporter.report(node.member(), GCPErrCode.FIELD_NOT_FOUND);
                return node.ast().Error;
            }
            memberTypes.add(found.get().outline().eventual());
        }
        // Collapse identical types; otherwise build a union of all field types.
        boolean allSame = memberTypes.stream().allMatch(t -> t.is(memberTypes.getFirst()) && memberTypes.getFirst().is(t));
        if (allSame) return memberTypes.getFirst();
        return Option.from(node.member(), node.ast(), memberTypes.toArray(new Outline[0]));
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
        Optional<EntityMember> member = defined.getMember(node.member().name());
        if(member.isPresent()){
            return member.get().outline();
        }else {
            // Use the declaredToBe Entity if it is one (e.g. value:{name:String}).
            // Fall back to the definedToBe Entity when the declared type is a primitive
            // (e.g. value:String) — in that case generic.min() returns the primitive, not an Entity.
            Outline minOutline = generic.min();
            Entity entity = (minOutline instanceof Entity) ? cast(minOutline) : defined;
            AccessorGeneric g = new AccessorGeneric(node);
            entity.addMember(node.member().name(), g, Modifier.PUBLIC, false, new Variable(node.member(),false,null));
            return g;
        }
    }
}