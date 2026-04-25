package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.outline.Outline;

/**
 * Sentinel for an absent declared type.
 *
 * <p>Epsilon is intentionally transparent in subtype checks:
 * {@code A <: Epsilon <: B} is treated as {@code A <: B}. It is not meant to
 * appear as a user-facing type; it only distinguishes "undeclared" from a real
 * declared {@code any} in the declaredToBe dimension.</p>
 */
public class Epsilon extends ANY {
    public Epsilon(AST ast) {
        super(ast);
    }

    @Override
    public long id() {
        return CONSTANTS.EPSILON_INDEX;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        return true;
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        return true;
    }

    @Override
    public String toString() {
        return "epsilon";
    }
}
