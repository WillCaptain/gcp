package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ProjectSession {
    private Map<Long,Outline> projections = new HashMap<>();
    private boolean disabled = false;

    public Outline getProjection(Projectable projected){
        return this.projections.get(projected.id());
    }
    public void addProjection(Projectable projected,Outline projection){
        if(projected.id()==projection.id() || this.disabled) return;
        this.projections.put(projected.id(),projection);
    }

    public void disable(Consumer<ProjectSession> project) {
        this.disabled = true;
        try {
            project.accept(this);
        }finally {
            this.disabled = false;
        }
    }
}
