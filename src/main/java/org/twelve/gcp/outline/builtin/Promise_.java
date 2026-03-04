package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.common.CONSTANTS;

/**
 * Singleton builtin tag for the {@code Promise} container type.
 * Mirrors {@link Array_} / {@link Dict_} in role: its only purpose is to
 * provide a stable, unique {@link #id()} so that two {@code Promise} instances
 * share the same "kind" in {@link org.twelve.gcp.outline.adt.ProductADT#maybe}.
 */
public class Promise_ extends BuildInOutline {

    private static final Promise_ _instance = new Promise_();

    public static Promise_ instance() {
        return _instance;
    }

    private Promise_() {
        super(null);
    }

    @Override
    public long id() {
        return CONSTANTS.PROMISE_INDEX;
    }
}
