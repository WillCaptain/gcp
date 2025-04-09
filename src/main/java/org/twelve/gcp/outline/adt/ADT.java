package org.twelve.gcp.outline.adt;

import org.twelve.gcp.outline.Outline;

public abstract class ADT implements Outline {
    private long id;

    public ADT(){
        this.id = Counter.getAndIncrement();
    }

    @Override
    public long id() {
        return this.id;
    }
}
