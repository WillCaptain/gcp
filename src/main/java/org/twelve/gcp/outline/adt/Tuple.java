package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.ProjectSession;
import org.twelve.gcp.outline.projectable.Projectable;

import java.util.Arrays;
import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public class Tuple  extends Entity {


    public Tuple(Entity entity) {
       super(entity.node(),entity.ast(),entity.ast().Any,entity.members());
    }

    @Override
    public Outline guess() {
        return new Tuple(cast(super.guess()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        List<EntityMember> ms = this.members().stream().filter(m->!m.isDefault()).toList();
        for (int i = 0; i < ms.size(); i++) {
            sb.append(ms.get(i).toString().split(":")[1] + (i == ms.size() - 1 ? "" : ","));
        }
        sb.append(")");
        return sb.toString();
    }
}
