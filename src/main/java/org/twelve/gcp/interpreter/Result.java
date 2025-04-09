package org.twelve.gcp.interpreter;

import org.twelve.gcp.outline.Outline;

public class Result<T> {
    private final T value;
    private final Outline outline;

    public Result(T value,Outline outline){
        this.value = value;
        this.outline = outline;
    }

    public T get(){
        return this.value;
    }

    public Outline outline() {
        return this.outline;
    }
}
