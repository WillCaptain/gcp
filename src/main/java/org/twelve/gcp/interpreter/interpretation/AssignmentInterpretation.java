package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.PolyValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.outline.primitive.Literal;

public class AssignmentInterpretation implements Interpretation<Assignment> {
    @Override
    public Value interpret(Assignment node, Interpreter interpreter) {
        Value rhs = interpreter.eval(node.rhs());
        // If the rhs is a PolyValue and the lhs declares a type annotation,
        // extract the matching variant — equivalent to "rhs as DeclaredType".
        // This handles: let ent:{name:String} = db  (same as  let ent = db as {name:String})
        if (rhs instanceof PolyValue poly && node.lhs() instanceof Variable var && var.declared() != null) {
            Class<? extends Value> target = AsInterpretation.targetValueClass(var.declared());
            if (target != null) rhs = poly.extract(target);
        }
        // When assigning to a variable that currently holds a PolyValue, replace only the
        // matching type variant(s) and preserve the rest.
        // Works for both single-value rhs (db = 100) and poly rhs (db = 200 & "Will1").
        String varName = assigneeName(node.lhs());
        if (varName != null) {
            Value existing = interpreter.env().lookup(varName);
            if (existing instanceof PolyValue pv) {
                if (rhs instanceof PolyValue rhsPoly) {
                    PolyValue merged = pv;
                    for (Value variant : rhsPoly.options()) {
                        merged = merged.withReplaced(variant);
                    }
                    rhs = merged;
                } else {
                    rhs = pv.withReplaced(rhs);
                }
            }
        }
        fillLiteralFields(node.lhs(), rhs, interpreter);
        UnpackBinder.bindAssignable(node.lhs(), rhs, interpreter);
        return rhs;
    }

    /**
     * When a variable has a declared outline type (e.g. {@code let x:Human = {...}}),
     * pre-populate any literal-type fields that were omitted from the entity literal.
     * Literal-type fields always carry a fixed constant value defined in the outline
     * declaration, so they do not need to be written explicitly by the programmer.
     * This resolution is performed purely from the AST (no inference required).
     */
    private static void fillLiteralFields(Assignable lhs, Value rhs, Interpreter interpreter) {
        if (!(rhs instanceof EntityValue ev)) return;

        Variable var = null;
        if (lhs instanceof Variable v) {
            var = v;
        } else {
            return;
        }

        TypeNode declared = var.declared();
        if (declared == null) return;

        EntityTypeNode etn = resolveEntityTypeNode(declared, interpreter);
        if (etn == null) return;

        for (Variable member : etn.members()) {
            String fieldName = member.name();
            if (ev.get(fieldName) != null) continue;

            TypeNode memberType = member.declared();
            if (!(memberType instanceof LiteralTypeNode ltn)) continue;

            var litOutline = ltn.outline();
            if (litOutline instanceof Literal lit && lit.node() instanceof LiteralNode<?> ln) {
                ev.setField(fieldName, interpreter.eval(ln));
            }
        }
    }

    private static EntityTypeNode resolveEntityTypeNode(TypeNode declared, Interpreter interpreter) {
        // Check for a symbol reference FIRST (e.g. :Human), because SymbolTupleTypeTypeNode extends
        // EntityTypeNode but is just a reference with no members. We resolve the actual definition
        // from the type registry.
        if (declared instanceof SymbolTypeNode<?> stn) {
            return lookupEntityType(stn.symbol().name(), interpreter);
        }
        if (declared instanceof EntityTypeNode etn) return etn;
        if (declared instanceof IdentifierTypeNode itn) {
            return lookupEntityType(itn.name(), interpreter);
        }
        return null;
    }

    private static EntityTypeNode lookupEntityType(String typeName, Interpreter interpreter) {
        OutlineDefinition def = interpreter.typeDefinitions().get(typeName);
        if (def != null && def.typeNode() instanceof EntityTypeNode etn) return etn;
        return null;
    }

    /** Returns the simple variable name from an assignable LHS, or null for complex targets. */
    private static String assigneeName(Assignable lhs) {
        if (lhs instanceof Variable v) return v.name();
        if (lhs instanceof Identifier id) return id.name();
        return null;
    }
}
