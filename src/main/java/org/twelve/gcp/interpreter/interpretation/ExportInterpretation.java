package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.imexport.Export;

/**
 * Export declarations are collected at the end of module execution via
 * {@code moduleExports()}, not during body traversal.  At runtime this node
 * produces no value.
 */
public class ExportInterpretation implements Interpretation<Export> {
    @Override
    public Value interpret(Export node, Interpreter interpreter) {
        return UnitValue.INSTANCE;
    }
}
