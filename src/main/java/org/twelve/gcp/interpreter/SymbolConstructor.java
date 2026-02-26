package org.twelve.gcp.interpreter;

import org.twelve.gcp.interpreter.value.Value;

import java.util.List;

/**
 * External plugin interface for {@code __xxx__} built-in constructors.
 * <p>
 * When the interpreter encounters a {@code ReferenceCallNode} whose callee identifier
 * is wrapped in double underscores (e.g. {@code __ontology_repo__<Employee>}), it strips
 * the underscores and delegates construction to the registered {@code SymbolConstructor}.
 * </p>
 * <pre>
 *   // Register:
 *   interpreter.registerConstructor("ontology_repo", (name, typeArgs) -> ...);
 *   // Usage in GCP code:
 *   let repo = __ontology_repo__&lt;Employee&gt;;
 * </pre>
 */
@FunctionalInterface
public interface SymbolConstructor {
    /**
     * Constructs a runtime value for the given constructor invocation.
     *
     * @param constructorName the stripped constructor name (e.g. {@code "ontology_repo"})
     * @param typeArgs        string representations of type arguments (for metadata/dispatch)
     * @param valueArgs       actual value arguments when the constructor is called as a function
     * @return the constructed runtime value
     */
    Value construct(String constructorName, List<String> typeArgs, List<Value> valueArgs);
}
