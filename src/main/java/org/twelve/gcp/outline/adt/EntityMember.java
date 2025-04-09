package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.outline.Outline;

public class EntityMember {

    private final Node node;

    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable, Node node) {
        return new EntityMember(name, outline, modifier, mutable ? Mutable.True : Mutable.False, node);
    }

    /**
     * no node bind  is for outline declare
     */
    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable) {
        return from(name, outline, modifier, mutable, null);
    }

    public static EntityMember from(String name, Poly outline, Modifier modifier) {
        return new EntityMember(name, outline, modifier, Mutable.Unknown, null);
    }

    private final String name;
    protected Outline outline;
    private final Modifier modifier;
    private final Mutable mutable;

    private EntityMember(String name, Outline outline, Modifier modifier, Mutable mutable, Node node) {
        this.name = name;
        this.outline = outline;
        this.modifier = modifier;
        this.mutable = mutable;
        this.node = node;
    }

    public String name() {
        return this.name;
    }

    public Outline outline() {
        return this.outline;
    }

    public Node node() {
        return this.node;
    }

    public Modifier modifier() {
        return this.modifier;
    }

    public Mutable mutable() {
        return this.mutable;
    }

    @Override
    public String toString() {
        return this.name+": "+outline.toString();
    }
}
