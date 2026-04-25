package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.FieldMergeMode;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.common.Mutable;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

public class EntityMember {

    private final Identifier node;

    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable, Identifier node, Boolean isDefault) {
        return from(name, outline, modifier, mutable, node, isDefault, FieldMergeMode.DEFAULT);
    }

    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable, Identifier node,
                                    Boolean isDefault, FieldMergeMode mergeMode) {
        return new EntityMember(name, outline, modifier, Mutable.from(mutable), node, isDefault, null, mergeMode);
    }

    /** Creates an EntityMember with a default-value node (for outline fields like {@code alias: "alice"}). */
    public static EntityMember fromWithDefault(String name, Outline outline, Modifier modifier, boolean mutable, Identifier node, Node defaultValueNode) {
        return fromWithDefault(name, outline, modifier, mutable, node, defaultValueNode, FieldMergeMode.DEFAULT);
    }

    public static EntityMember fromWithDefault(String name, Outline outline, Modifier modifier, boolean mutable,
                                               Identifier node, Node defaultValueNode, FieldMergeMode mergeMode) {
        return new EntityMember(name, outline, modifier, Mutable.from(mutable), node, true, defaultValueNode, mergeMode);
    }

    /**
     * no node bind  is for outline declare
     */
    public static EntityMember from(String name, Outline outline, Modifier modifier, boolean mutable) {
        return from(name, outline, modifier, mutable, null, false);
    }

    public static EntityMember from(String name, Poly outline, Modifier modifier) {
        return new EntityMember(name, outline, modifier, Mutable.Unknown, null, false, null, FieldMergeMode.OVERLOAD);
    }


    private final String name;
    protected Outline outline;
    private final Modifier modifier;
    private final Mutable mutable;
    private final boolean isDefault;
    private final FieldMergeMode mergeMode;
    /** AST node for the default value expression (e.g. the {@code "alice"} literal), or {@code null}. */
    private final Node defaultValueNode;

    private EntityMember(String name, Outline outline, Modifier modifier, Mutable mutable, Identifier node, Boolean isDefault, Node defaultValueNode) {
        this(name, outline, modifier, mutable, node, isDefault, defaultValueNode, FieldMergeMode.DEFAULT);
    }

    private EntityMember(String name, Outline outline, Modifier modifier, Mutable mutable, Identifier node,
                         Boolean isDefault, Node defaultValueNode, FieldMergeMode mergeMode) {
        this.name = name;
        this.outline = outline;
        this.modifier = modifier;
        this.mutable = mutable;
        this.node = node;
        this.isDefault = isDefault;
        this.defaultValueNode = defaultValueNode;
        this.mergeMode = mergeMode == null ? FieldMergeMode.DEFAULT : mergeMode;
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

    /** Returns the AST node for the default value, or {@code null} if the field has no default. */
    public Node defaultValueNode() {
        return this.defaultValueNode;
    }

    /** True if this field has an explicit default value (not a literal-type constant). */
    public boolean hasDefaultValue() {
        return this.defaultValueNode != null;
    }

    public FieldMergeMode mergeMode() {
        return this.mergeMode;
    }
}
