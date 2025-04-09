package org.twelve.gcp.common;

public enum Modifier {
    PUBLIC, PRIVATE;

    public Modifier max(Modifier another) {
        if (this == PUBLIC) return this;
        else return another;
    }

    public Modifier min(Modifier another) {
        if (this == PRIVATE) return this;
        else return another;
    }
}
