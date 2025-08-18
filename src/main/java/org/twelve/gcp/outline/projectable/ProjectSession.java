package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProjectSession {
    private Map<Long,Outline> projections = new HashMap<>();
    private List<String> traces = new ArrayList<>();
    private boolean disabled = false;
    private Map<Outline, Outline> copiedCache = new HashMap<>();

    public Outline getProjection(Projectable target){
        return this.projections.get(target.id());
    }
    public void addProjection(Projectable target,Outline projection){
        if(target.id()==projection.id() || this.disabled) return;
        this.projections.put(target.id(),projection);
    }

    public void disable(Consumer<ProjectSession> project) {
        this.disabled = true;
        try {
            project.accept(this);
        }finally {
            this.disabled = false;
        }
    }

    public void addTrace(Projectable target, Projectable projected) {
           traces.add(target.id()+"|"+projected.id());
    }

    public boolean contains(Projectable target, Projectable projected) {
        return traces.contains(target.id()+"|"+projected.id());
    }

    public Map<Outline, Outline> copiedCache() {
        return this.copiedCache;
    }

    public void removeProjection(Outline key) {
        this.projections.remove(key.id());
    }
}
