package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

import java.util.Arrays;

import static org.twelve.gcp.common.Tool.cast;

public class Tuple  extends Entity {


    public Tuple(Entity entity) {
       super(entity.node(),Outline.Any,entity.members());
    }

    @Override
    public Outline guess() {
        return new Tuple(cast(super.guess()));
    }
}
