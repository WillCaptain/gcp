package org.twelve.gcp.outline.adt;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.HashMap;
import java.util.Map;

public abstract class ADT implements Outline {
    protected long id;

    protected final Map<String, EntityMember> members = new HashMap<>();

    public ADT() {
        this.id = Counter.getAndIncrement();
        this.init();
    }

    protected void init(){
        EntityMember toString = EntityMember.from(CONSTANTS.TO_STR, FirstOrderFunction.from(Outline.String,Outline.Unit),
                Modifier.PUBLIC, false,null,true);
        this.members.put(CONSTANTS.TO_STR, toString);
    }
    @Override
    public long id() {
        return this.id;
    }
}
