package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

import java.util.HashMap;
import java.util.Map;

public class ProjectSession {
    private Map<Long,Outline> projections = new HashMap<>();
    public Outline getProjection(Projectable projected){
        return this.projections.get(projected.id());
    }
    public void addProjection(Projectable projected,Outline projection){
        this.projections.put(projected.id(),projection);
    }

}
