package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;

import java.util.Collections;

public class SymbolIdentifierInterpretation implements Interpretation<SymbolIdentifier> {
    @Override
    public Value interpret(SymbolIdentifier node, Interpreter interpreter) {
        String name = node.name();
        Value v = interpreter.env().lookup(name);
        if (v != null) return v;
        return new EntityValue(name, Collections.emptyMap(), null);
    }
}
