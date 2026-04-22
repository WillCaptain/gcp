package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.accessor.Accessor;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.NOTHING;

import java.util.Optional;

import static org.twelve.gcp.common.Tool.cast;

public class AccessorGeneric extends Genericable<AccessorGeneric, Accessor> {
    private final Outline projected;

    public AccessorGeneric(Accessor node) {
        this(node, null);
    }

    public AccessorGeneric(Accessor node, Outline projection) {
        super(node);
        this.projected = projection;
    }

    protected Outline entityOutline() {
        return projected == null ? ((MemberAccessor) this.node()).host().outline() : projected;
    }

    private String memberName() {
        return ((MemberAccessor) this.node()).member().name();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (session.getProjection(this) != null) {
            return session.getProjection(this);
        }
        if (this.entityOutline().id() == projected.id()) {//project belonged entity
            if (projection instanceof Genericable) {
                ((Projectable) this.entityOutline()).project(projected, projection, session);
                AccessorGeneric result = new AccessorGeneric(this.node, projection);
                result.id = this.id;
                return result;

            } else {
                Outline eventual = projection.eventual();
                Entity entity;
                if (eventual instanceof Poly poly) {
                    // Poly argument: find the entity component for member access
                    entity = poly.options().stream()
                            .filter(o -> o.eventual() instanceof Entity)
                            .map(o -> (Entity) o.eventual())
                            .findFirst()
                            .orElse(null);
                    if (entity == null) {
                        GCPErrorReporter.report(this.node(), GCPErrCode.PROJECT_FAIL,
                                " no entity member in Poly for member access ." + this.memberName());
                        return this.ast().Error;
                    }
                } else if (eventual instanceof Entity) {
                    entity = cast(eventual);
                } else {
                    // Projection resolved to a non-Entity type (e.g. a primitive like String).
                    // The declared parameter type may be a primitive with structural member constraints
                    // (e.g. (value:String)->value.age) — no member can be found on a primitive.
                    GCPErrorReporter.report(this.node(), GCPErrCode.FIELD_NOT_FOUND,
                            " member '" + this.memberName() + "' not found on type " + eventual);
                    return this.ast().Error;
                }
                Optional<EntityMember> member = entity.members().stream().filter(m -> m.name().equals(this.memberName())).findFirst();
                if (member.isPresent()) {
                    session.addProjection(this, member.get().outline());
                    return member.get().outline();
                } else {
                    GCPErrorReporter.report(this.node(), GCPErrCode.PROJECT_FAIL,
                            " member " + this.memberName() + " not found in " + entity.node());
                    return this.ast().Error;
                }

            }
        }

        return super.doProject(projected, projection, session);
    }

    /**
     * When a projected copy (this.projected != null, created by doProject) receives a
     * constraint from the surrounding expression context (e.g. "must be Number" from
     * arithmetic), back-propagate that constraint to the ORIGINAL AccessorGeneric in
     * the function definition so that later call-site projections can detect mismatches.
     *
     * <p>Example: {@code g = y -> y.age; g(x.son) - 1}
     * <ol>
     *   <li>{@code g(x.son)} projects {@code Return(g).supposed = AccessorGeneric(y.age)}
     *       and returns a copy {@code AccessorGeneric(y.age, projected=AccessorGeneric(son))}.</li>
     *   <li>{@code - 1} calls {@code copy.addDefinedToBe(Number)}.</li>
     *   <li>This method back-propagates: {@code AccessorGeneric(y.age).addHasToBe(Number)}.</li>
     *   <li>When {@code f(\{son=\{age="will"\}\})} is later projected, {@code String} is checked
     *       against {@code AccessorGeneric(y.age)\{hasToBe=Number\}} → PROJECT_FAIL.</li>
     * </ol>
     */
    private void backPropagateToOriginal(Outline outline) {
        if (this.projected == null) return; // this IS the original; nothing to propagate to
        if (outline == null || outline instanceof ANY || outline instanceof NOTHING) return;
        if (this.node == null) return;
        Outline original = this.node.outline();
        if (original instanceof Constrainable orig && original != this && original.id() == this.id) {
            orig.addHasToBe(outline);
        }
    }

    @Override
    public boolean addDefinedToBe(Outline outline) {
        boolean result = super.addDefinedToBe(outline);
        if (result) backPropagateToOriginal(outline);
        return result;
    }

    @Override
    public void addHasToBe(Outline outline) {
        super.addHasToBe(outline);
        backPropagateToOriginal(outline);
    }

    /**
     * Returns the minimum (lower-bound) constraint for this AccessorGeneric.
     * <p>
     * When this instance is a projected copy (created by {@link #doProject} with a non-null
     * {@code projected} field), its own constraints may be stale — they were captured at
     * copy time, before any back-propagation from downstream arithmetic (or other usage)
     * constraints reached the original.  In that case, fall back to the original's
     * {@code min()} so that type checks see the up-to-date constraint.
     * <p>
     * Example: {@code g = y -> y.age; f = x -> g(x.son) - 1}
     * <ul>
     *   <li>The {@code - 1} back-propagates {@code Number} to {@code AG_original(y.age)}.</li>
     *   <li>A copy {@code AG_copy2} stored inside {@code f}'s parameter entity was made
     *       before that propagation and has own {@code min() = ANY}.</li>
     *   <li>This override returns {@code AG_original.min() = Number} for the copy,
     *       so that {@code String.is(AG_copy2)} → false → PROJECT_FAIL.</li>
     * </ul>
     */
    @Override
    public Outline min() {
        Outline ownMin = super.min();
        if (!(ownMin instanceof ANY)) return ownMin;
        if (this.node != null) {
            Outline original = this.node.outline();
            if (original instanceof Generalizable orig && original != this && original.id() == this.id) {
                return orig.min();
            }
        }
        return ownMin;
    }

    @Override
    protected AccessorGeneric createNew() {
        return new AccessorGeneric(cast(this.node), this.projected);
    }


}
