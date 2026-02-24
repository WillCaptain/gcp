package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.builtin.Bool_;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

/**
 * Boolean type in the GCP type system.
 * <p>
 * Provides built-in logical operations.
 *
 * @author huizi 2025
 */
public class BOOL extends Primitive {
    private final static Bool_ bool_ = new Bool_();

    public BOOL(AbstractNode node) {
        super(bool_, node, node.ast());
        this.loadBuiltInMethods();
    }

    public BOOL(AST ast) {
        super(bool_, null, ast);
    }

    /**
     * Loads built-in boolean methods.
     * <ul>
     *   <li>{@code not()}            : Unit → Bool — logical negation</li>
     *   <li>{@code and_also(Bool)}   : Bool → Bool — short-circuit AND</li>
     *   <li>{@code or_else(Bool)}    : Bool → Bool — short-circuit OR</li>
     * </ul>
     */
    @Override
    public boolean loadBuiltInMethods() {
        if (!super.loadBuiltInMethods()) return false;
        AST ast = this.ast();
        members.put("not",      EntityMember.from("not",      FirstOrderFunction.from(ast, ast.Boolean, ast.Unit),    Modifier.PUBLIC, false, null, true));
        members.put("and_also", EntityMember.from("and_also", FirstOrderFunction.from(ast, ast.Boolean, ast.Boolean), Modifier.PUBLIC, false, null, true));
        members.put("or_else",  EntityMember.from("or_else",  FirstOrderFunction.from(ast, ast.Boolean, ast.Boolean), Modifier.PUBLIC, false, null, true));
        return true;
    }
}
