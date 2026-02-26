package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.statement.OutlineDeclarator;

/**
 * Registers each named type definition in the interpreter's type-definition registry
 * so that declared-type annotations (e.g. {@code let x:T = ...}) can be resolved at
 * runtime without depending on the inference pass.
 */
public class OutlineDeclaratorInterpretation implements Interpretation<OutlineDeclarator> {
    @Override
    public Value interpret(OutlineDeclarator node, Interpreter interpreter) {
        for (OutlineDefinition def : node.definitions()) {
            interpreter.typeDefinitions().put(def.symbolNode().name(), def);
        }
        return UnitValue.INSTANCE;
    }
}
