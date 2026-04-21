package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.SYMBOL;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

public class SymbolIdentifierInference implements Inference<SymbolIdentifier> {
    @Override
    public Outline infer(SymbolIdentifier node, Inferencer inferencer) {
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        EnvSymbol supposed = oEnv.lookupSymbol(node.name());
        if (supposed == null) {
            // Built-in primitive name resolution: a bare identifier like
            // `Integer`, `String`, `Number`, `Boolean` … (common in
            // reference-projection positions such as `f<Integer>`) must
            // resolve to the canonical primitive Outline, not to a
            // name-matched SYMBOL placeholder. SYMBOL's is()-check is
            // literal-equality only — it cannot see that "Integer" is a
            // subtype of "Number" or of a `String|Number` union, which
            // causes reference-projection mismatches downstream.
            Outline builtin = resolveBuiltin(node.ast(), node.name());
            if (builtin != null) return builtin;
            Outline outline = new SYMBOL(node);
            oEnv.defineSymbol(node.name(), outline, false, node);
            return outline;
        } else {
            return supposed.outline();
        }
    }

    private static Outline resolveBuiltin(AST ast, String name) {
        return switch (name) {
            case "String"  -> ast.String;
            case "Integer" -> ast.Integer;
            case "Long"    -> ast.Long;
            case "Float"   -> ast.Float;
            case "Double"  -> ast.Double;
            case "Decimal" -> ast.Decimal;
            case "Number"  -> ast.Number;
            case "Boolean" -> ast.Boolean;
            default        -> null;
        };
    }
}
