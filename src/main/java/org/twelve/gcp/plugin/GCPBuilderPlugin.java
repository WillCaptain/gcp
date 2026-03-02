package org.twelve.gcp.plugin;

import org.twelve.gcp.interpreter.value.Value;

import java.util.List;

/**
 * SPI (Service Provider Interface) for GCP external-builder plugins.
 *
 * <p>A plugin JAR placed in the GCP plugins directory and whose filename starts with
 * {@code ext_builder_} will be scanned by {@link PluginLoader} at interpreter startup.
 * Every class listed in the JAR's {@code META-INF/services/org.twelve.gcp.plugin.GCPBuilderPlugin}
 * file is instantiated and registered with the interpreter automatically.
 *
 * <p>Usage in GCP source code:
 * <pre>
 *   let repo = __my_plugin__&lt;Employee&gt;;
 * </pre>
 * The double-underscore wrapper is stripped, and the interpreter looks up the registered
 * {@code GCPBuilderPlugin} whose {@link #id()} equals {@code "my_plugin"}.
 *
 * <p>Registering plugins programmatically (for testing or embedding):
 * <pre>
 *   interpreter.loadPlugins(Paths.get("/app/plugins"));
 * </pre>
 */
public interface GCPBuilderPlugin {

    /**
     * The constructor identifier (without surrounding {@code __}).
     * For example, a plugin for {@code __my_builder__} returns {@code "my_builder"}.
     */
    String id();

    /**
     * Constructs and returns a runtime {@link Value} for the given invocation.
     *
     * @param id        the constructor identifier (same as {@link #id()})
     * @param typeArgs  string representations of the type arguments, e.g. {@code ["Employee"]}
     * @param valueArgs runtime value arguments when the constructor is called as a function
     * @return the constructed runtime value
     */
    Value construct(String id, List<String> typeArgs, List<Value> valueArgs);
}
