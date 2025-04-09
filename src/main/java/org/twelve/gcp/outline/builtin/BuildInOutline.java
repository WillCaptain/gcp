package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;

public interface BuildInOutline extends Outline {
    default boolean tryIamYou(Outline another) {
        if (another instanceof UNKNOWN) return true;
        Class<?> yourClass = another.getClass();
        return yourClass.isInstance(this);
    }

    @Override
    default Node node(){
        return null;
    }
}
