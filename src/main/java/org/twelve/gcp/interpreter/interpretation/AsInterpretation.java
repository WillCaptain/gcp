package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.As;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.*;

public class AsInterpretation implements Interpretation<As> {
    @Override
    public Value interpret(As node, Interpreter interpreter) {
        Value val = interpreter.eval(node.expression());
        // If the value is a PolyValue, extract the variant whose type matches
        // the target type annotation.
        if (val instanceof PolyValue poly) {
            Class<? extends Value> target = targetValueClass(node.as());
            if (target != null) return poly.extract(target);
        }
        return val;
    }

    /**
     * Determine the runtime {@link Value} class for a {@link TypeNode}.
     * <p>
     * Checks the TypeNode's Java class first (works without inference), then
     * falls back to the inferred outline when available.  Returns {@code null}
     * when the mapping is unknown (caller returns the value as-is).
     * <p>
     * Package-visible so that {@link AssignmentInterpretation} can reuse the same
     * mapping when a declared type annotation is used instead of an explicit {@code as}.
     */
    static Class<? extends Value> targetValueClass(TypeNode typeNode) {
        if (typeNode == null) return null;

        // Fast structural check – works without prior inference
        if (typeNode instanceof EntityTypeNode || typeNode instanceof ExtendTypeNode) return EntityValue.class;
        if (typeNode instanceof TupleTypeNode)   return TupleValue.class;
        if (typeNode instanceof FunctionTypeNode) return FunctionValue.class;

        // Named / identifier-based type (e.g. "Int" → IdentifierTypeNode("Integer"), "String", user-defined)
        // Note: IntTypeConverter creates IdentifierTypeNode("Integer"), not ("Int")
        if (typeNode instanceof IdentifierTypeNode itn) {
            return switch (itn.name()) {
                case "Integer", "Long"           -> IntValue.class;
                case "Float", "Double", "Number",
                     "Decimal", "FLOAT", "LONG"  -> FloatValue.class;
                case "String"                    -> StringValue.class;
                case "Bool", "Boolean"           -> BoolValue.class;
                default                          -> EntityValue.class; // user-defined outline or ADT symbol
            };
        }

        // Fallback: use the inferred outline if it has been resolved
        Outline outline = typeNode.outline();
        if (outline instanceof UNKNOWN) return null;
        if (outline instanceof INTEGER || outline instanceof LONG)   return IntValue.class;
        if (outline instanceof DECIMAL || outline instanceof FLOAT
                || outline instanceof DOUBLE || outline instanceof NUMBER) return FloatValue.class;
        if (outline instanceof STRING)      return StringValue.class;
        if (outline instanceof BOOL)        return BoolValue.class;
        if (outline instanceof SYMBOL)      return EntityValue.class;
        if (outline instanceof Tuple)       return TupleValue.class;
        if (outline instanceof ProductADT)  return EntityValue.class;
        return null;
    }
}
