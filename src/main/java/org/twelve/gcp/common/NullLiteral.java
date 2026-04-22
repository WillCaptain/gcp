package org.twelve.gcp.common;

/**
 * Marker payload for the `null` literal token.
 *
 * <p>Token payload cannot be Java null, so we use this sentinel object and map it
 * to Outline `Nothing` in inference and UnitValue at runtime interpretation.</p>
 */
public final class NullLiteral {
    public static final NullLiteral INSTANCE = new NullLiteral();

    private NullLiteral() {}

    @Override
    public String toString() {
        return "null";
    }
}
