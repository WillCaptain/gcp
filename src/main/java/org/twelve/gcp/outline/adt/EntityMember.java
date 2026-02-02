package org.twelve.gcp.outline.adt;

import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Function;

import static org.twelve.gcp.common.Tool.cast;

public class EntityMember {

    private final Identifier node;

    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable, Identifier node, Boolean isDefault) {
        return new EntityMember(name, outline, modifier, Mutable.from(mutable), node, isDefault);
    }

    /**
     * no node bind  is for outline declare
     */
    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable) {
        return from(name, outline, modifier, mutable, null, false);
    }

    public static EntityMember from(String name, Poly outline, Modifier modifier) {
        return new EntityMember(name, outline, modifier, Mutable.Unknown, null, false);
    }


    private final String name;
    protected Outline outline;
    private final Modifier modifier;
    private final Mutable mutable;
    private final boolean isDefault;

    private EntityMember(String name, Outline outline, Modifier modifier, Mutable mutable, Identifier node, Boolean isDefault) {
        this.name = name;
        this.outline = outline;
        this.modifier = modifier;
        this.mutable = mutable;
        this.node = node;
        this.isDefault = isDefault;
    }

    public String name() {
        return this.name;
    }

    public Outline outline() {
        return this.outline;
    }

    public Identifier node() {
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
        return this.name + ": " + outline.toString();
    }

    public boolean isDefault() {
        return this.isDefault;
    }
}
