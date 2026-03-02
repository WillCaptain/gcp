package org.twelve.gcp.config;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Module-level configuration for the GCP runtime.
 *
 * <p>Backed by <a href="https://commons.apache.org/proper/commons-configuration/">
 * Apache Commons Configuration 2</a>, with a four-layer lookup order:
 * <ol>
 *   <li><b>Programmatic overrides</b> – supplied via {@link #with(String, String)}</li>
 *   <li><b>Java system properties</b> – {@code -Dplugin_dir=/path}</li>
 *   <li><b>Environment variables</b> – {@code PLUGIN_DIR=/path}</li>
 *   <li><b>Classpath defaults</b> – {@code gcp.properties} bundled in the JAR</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>
 *   // standard load — reads gcp.properties + system/env overrides
 *   GCPConfig cfg = GCPConfig.load();
 *   String dir = cfg.getString("plugin_dir");    // → "ext_builders"
 *   Path   p   = cfg.getPath("plugin_dir");      // → Path("ext_builders")
 *
 *   // override a single key (e.g. in tests)
 *   GCPConfig cfg = GCPConfig.load().with("plugin_dir", "target/test-plugins");
 *   new OutlineInterpreter(cfg);
 * </pre>
 *
 * <h2>Adding new configuration keys</h2>
 * <ol>
 *   <li>Add the key + default value to {@code src/main/resources/gcp.properties}.</li>
 *   <li>Add a typed getter here (optional, for convenience).</li>
 *   <li>Access everywhere via {@code config.getString("the_key")}.</li>
 * </ol>
 */
public final class GCPConfig {

    private static final String DEFAULTS_RESOURCE = "gcp.properties";

    private final CompositeConfiguration composite;

    // ── constructor ───────────────────────────────────────────────────────────

    private GCPConfig(CompositeConfiguration composite) {
        this.composite = composite;
    }

    // ── factory ───────────────────────────────────────────────────────────────

    /**
     * Loads the configuration in standard priority order:
     * programmatic → system props → env vars → {@code gcp.properties} classpath defaults.
     */
    public static GCPConfig load() {
        CompositeConfiguration cc = new CompositeConfiguration();
        cc.setThrowExceptionOnMissing(false);

        // Layer 1: empty map reserved for per-instance programmatic overrides
        cc.addConfiguration(new MapConfiguration(new HashMap<>()));

        // Layer 2: JVM system properties (-Dplugin_dir=…)
        cc.addConfiguration(new SystemConfiguration());

        // Layer 3: OS environment variables (PLUGIN_DIR=…)
        cc.addConfiguration(new EnvironmentConfiguration());

        // Layer 4: bundled defaults (gcp.properties on classpath)
        URL url = GCPConfig.class.getClassLoader().getResource(DEFAULTS_RESOURCE);
        if (url != null) {
            PropertiesConfiguration props = new PropertiesConfiguration();
            FileHandler fh = new FileHandler(props);
            fh.setURL(url);
            try {
                fh.load();
                cc.addConfiguration(props);
            } catch (ConfigurationException ignored) {
                // fall through to hardcoded safety net
            }
        }

        // Safety net: hardcoded defaults in case gcp.properties is missing
        cc.addConfiguration(new MapConfiguration(Map.of("plugin_dir", "ext_builders")));

        return new GCPConfig(cc);
    }

    // ── override API ──────────────────────────────────────────────────────────

    /**
     * Returns a new {@link GCPConfig} that looks up {@code key} from {@code value}
     * before consulting any other source.  All other keys are inherited unchanged.
     *
     * <p>Typical use:
     * <pre>
     *   GCPConfig cfg = GCPConfig.load().with("plugin_dir", "target/test-plugins");
     * </pre>
     */
    public GCPConfig with(String key, String value) {
        CompositeConfiguration cc = new CompositeConfiguration();
        cc.setThrowExceptionOnMissing(false);
        cc.addConfiguration(new MapConfiguration(Map.of(key, value)));
        cc.addConfiguration(this.composite);
        return new GCPConfig(cc);
    }

    // ── typed getters ─────────────────────────────────────────────────────────

    /**
     * Returns the string value for {@code key}, or {@code null} if not found.
     */
    public String getString(String key) {
        return composite.getString(key);
    }

    /**
     * Returns the string value for {@code key}, or {@code defaultValue} if not found.
     */
    public String getString(String key, String defaultValue) {
        return composite.getString(key, defaultValue);
    }

    /**
     * Returns the value for {@code key} as a {@link Path}.
     * Equivalent to {@code Paths.get(getString(key))}.
     */
    public Path getPath(String key) {
        return Paths.get(getString(key));
    }

    /**
     * Returns the value for {@code key} as a {@link Path},
     * or {@code defaultPath} if the key is absent.
     */
    public Path getPath(String key, Path defaultPath) {
        String v = getString(key);
        return v != null ? Paths.get(v) : defaultPath;
    }
}
