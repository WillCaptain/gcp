package org.twelve.gcp.ast;

import java.util.Objects;

public record SimpleLocation(long start, long end) implements Location {
    public SimpleLocation {
//        if (end < start) {
//            throw new IllegalArgumentException("End cannot precede start");
//        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location that)) return false;
        return start() == that.start() && end() == that.end();
    }

    @Override
    public int hashCode() {
        return Objects.hash(start(), end());
    }
}