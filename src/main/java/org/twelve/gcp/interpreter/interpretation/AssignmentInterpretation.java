package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.outline.primitive.Literal;

public class AssignmentInterpretation implements Interpretation<Assignment> {
    @Override
    public Value interpret(Assignment node, Interpreter interpreter) {
        Value rhs = interpreter.eval(node.rhs());
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
}
