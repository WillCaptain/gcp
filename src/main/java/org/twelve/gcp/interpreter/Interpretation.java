package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.interpreter.value.Value;

/**
 * Stateless visitor logic for a specific AST node type.
 * Mirrors {@link org.twelve.gcp.inference.Inference} in the type-inference layer.
 *
 * @param <T> the concrete node type this interpretation handles
 */
public interface Interpretation<T extends AbstractNode> {
    /**
     * Interprets the given node, using {@code interpreters} to recurse into
     * sub-nodes and to access shared interpreter state (environment, etc.).
     */
    Value interpret(T node, Interpreter interpreter);
}
