package org.twelve.gcp.interpreter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Immutable-style configuration for {@link OutlineInterpreter}.
 *
 * <p>Use the fluent factory to build a config:
 * <pre>
 *   // use all defaults
 *   InterpreterConfig cfg = InterpreterConfig.defaults();
 *
 *   // override individual items
 *   InterpreterConfig cfg = InterpreterConfig.defaults()
 *       .pluginsDir(Paths.get("/app/ext_builders"));
 * </pre>
 *
 * <h2>Adding new config items</h2>
 * <ol>
 *   <li>Declare a {@code private} field with a sensible default constant.</li>
 *   <li>Add a public constant {@code DEFAULT_*} for documentation / testing.</li>
 *   <li>Add a public fluent setter that validates and returns {@code this}.</li>
 *   <li>Add a public getter.</li>
 * </ol>
 */
public final class InterpreterConfig {

    // ── defaults ──────────────────────────────────────────────────────────────

    /**
     * Default directory scanned for {@code ext_builder_*.jar} plugin files.
     * Relative to the JVM working directory, so it resolves to {@code ./ext_builders/}
     * in production and to {@code <module>/ext_builders/} in unit-test runs.
     * If the directory is absent the scan is silently skipped.
     */
    public static final Path DEFAULT_PLUGINS_DIR = Paths.get("ext_builders");

    // ── config fields (each has its own default) ──────────────────────────────

    private Path pluginsDir = DEFAULT_PLUGINS_DIR;

    // ── constructor ───────────────────────────────────────────────────────────

    private InterpreterConfig() {}

    // ── factory ───────────────────────────────────────────────────────────────

    /**
     * Returns a new {@link InterpreterConfig} pre-filled with all default values.
     * Use the fluent setters to override specific items.
     */
    public static InterpreterConfig defaults() {
        return new InterpreterConfig();
    }

    // ── fluent setters ────────────────────────────────────────────────────────

    /**
     * Overrides the directory scanned for {@code ext_builder_*.jar} plugins.
     *
     * @param dir path to the plugins directory; must not be {@code null}
     * @return {@code this}
     */
    public InterpreterConfig pluginsDir(Path dir) {
        this.pluginsDir = Objects.requireNonNull(dir, "pluginsDir must not be null");
        return this;
    }

    // ── getters ───────────────────────────────────────────────────────────────

    public Path pluginsDir() {
        return pluginsDir;
    }
}
