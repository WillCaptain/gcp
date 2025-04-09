package org.twelve.gcp.ast;

public class SimpleLocation implements Location{
    private final long start;
    private final long end;

    public SimpleLocation(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public long start() {
        return this.start;
    }

    @Override
    public long end() {
        return this.end;
    }
}