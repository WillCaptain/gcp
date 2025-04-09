package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import static org.twelve.gcp.common.Tool.cast;


public interface Projectable extends Outline {

    Outline doProject(Projectable projected, Outline projection, ProjectSession session);

    default Outline project(Projectable projected, Outline projection, ProjectSession session) {
        Outline cachedProjection = session.getProjection(projected) == null ? projection : session.getProjection(projected);
        if (cachedProjection instanceof Projectable && session.getProjection(cast(cachedProjection)) != null) {
            cachedProjection = session.getProjection(cast(projection));
        }
        Outline cachedProjected = session.getProjection(this);
        if (cachedProjected == null) {
            Outline result = this.doProject(projected, cachedProjection, session);
            if (this.id() == projected.id()) {
                session.addProjection(this, result);
            }
            return result;
        } else {
            if (cachedProjected instanceof Projectable && cachedProjected.id() != cachedProjection.id()) {
                return ((Projectable) cachedProjected).project(projected, cachedProjection, session);
            } else {
                return cachedProjected;
            }
        }
    }

//    default boolean checkIfProjectMyself(Outline projected){
//        return this.id()==projected.id();
//    }

    default boolean is(Outline another) {
        return this.tryIamYou(another) || another.tryYouAreMe(this);
    }

    Outline guess();

}
