package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.SymbolConstructor;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.StringValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReferenceCallInterpretation implements Interpretation<ReferenceCallNode> {
    @Override
    public Value interpret(ReferenceCallNode node, Interpreter interp) {
        List<String> typeArgs = node.types().stream()
                .map(r -> r.lexeme().replaceAll("[<>]", "").trim())
                .collect(Collectors.toList());

        Value fn = interp.eval(node.host());

        if (fn instanceof StringValue sv) {
            String constructorName = sv.value();
            SymbolConstructor ctor = interp.constructors().get(constructorName);
            if (ctor == null) {
                return new EntityValue(constructorName,
                        Map.of("__typeArgs__", new StringValue(String.join(",", typeArgs))), null);
            }
            return ctor.construct(constructorName, typeArgs, Collections.emptyList());
        }

        return fn;
    }
}
