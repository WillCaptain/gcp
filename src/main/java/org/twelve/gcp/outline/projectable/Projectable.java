package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import static org.twelve.gcp.common.Tool.cast;


public interface Projectable extends Outline {

    Outline doProject(Projectable projected, Outline projection, ProjectSession session);

    default Outline project(Projectable projected, Outline projection, ProjectSession session) {
        //check if want to be projected has done the projection before
        Outline cachedProjection = session.getProjection(projected) == null ? projection : session.getProjection(projected);
        while (cachedProjection instanceof Projectable && session.getProjection(cast(cachedProjection)) != null) {
//            cachedProjection = session.getProjection(cast(projection));
            Outline previous = cachedProjection;
            cachedProjection = session.getProjection(cast(cachedProjection));
            if(previous==cachedProjection) break;
        }
        //check if i have done the projection
        Outline cachedProjected = session.getProjection(this);
        if (cachedProjected == null) {//project if i'm not done
            Outline result = this.doProject(projected, cachedProjection, session);
            if (this.id() == projected.id()) {
                session.addProjection(this, result);
            }
            return result;
        } else {//fetch the cached projection and project deeper if the result still need to be projected
            if (cachedProjected instanceof Projectable && cachedProjected.id() != cachedProjection.id()) {
                return ((Projectable) cachedProjected).project(projected, cachedProjection, session);
            } else {
                return cachedProjected;
            }
        }
    }
    default boolean is(Outline another) {
        return this.tryIamYou(another) || another.tryYouAreMe(this);
    }

    Outline guess();

}
