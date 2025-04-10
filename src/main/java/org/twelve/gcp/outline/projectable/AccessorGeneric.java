package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.accessor.Accessor;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;

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
        return projected == null ? ((MemberAccessor) this.node()).entity().outline() : projected;
    }

    private String memberName() {
        return ((MemberAccessor) this.node()).member().token();
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if(session.getProjection(this)!=null){
            return session.getProjection(this);
        }
        if (this.entityOutline().id() == projected.id()) {//project belonged entity
            if (projection instanceof Genericable) {
                ((Projectable)this.entityOutline()).project(projected, projection, session);
                return new AccessorGeneric(this.node, projection);

            } else {
                Entity entity = cast(projection);
                Optional<EntityMember> member = entity.members().stream().filter(m -> m.name().equals(this.memberName())).findFirst();
                if (member.isPresent()) {
                    session.addProjection(this,member.get().outline());
                    return member.get().outline();
                } else {
                    ErrorReporter.report(this.node(), GCPErrCode.PROJECT_FAIL," member "+this.memberName()+" not found");
                    return Error;
                }

            }
        }

        return super.doProject(projected, projection, session);
    }

    @Override
    protected AccessorGeneric createNew() {
        return new AccessorGeneric(cast(this.node),this.projected);
    }


}
