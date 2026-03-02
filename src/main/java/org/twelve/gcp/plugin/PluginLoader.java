package org.twelve.gcp.plugin;

import org.twelve.gcp.config.GCPConfig;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Scans a directory for GCP external-builder plugin JARs and loads them via
 * Java's {@link ServiceLoader} mechanism.
 *
 * <h2>Naming convention</h2>
 * Only JAR files whose name starts with {@value #PLUGIN_JAR_PREFIX} are considered.
 * This prevents accidental loading of unrelated JARs that happen to be in the same directory.
 *
 * <h2>ServiceLoader registration</h2>
 * Each plugin JAR must contain a standard service descriptor at:
 * <pre>META-INF/services/org.twelve.gcp.plugin.GCPBuilderPlugin</pre>
 * listing the fully-qualified class name(s) of the {@link GCPBuilderPlugin} implementations.
 *
 * <h2>Configuration key</h2>
 * <pre>plugin_dir</pre> – read from {@link GCPConfig} to locate the plugins directory.
 */
public final class PluginLoader {

    /** Configuration key used to look up the plugins directory. */
    public static final String CONFIG_KEY_PLUGIN_DIR = "plugin_dir";

    /** Filename prefix required for a JAR to be recognised as a GCP builder plugin. */
    public static final String PLUGIN_JAR_PREFIX = "ext_builder_";

    private PluginLoader() {}

    /**
     * Loads all {@link GCPBuilderPlugin} instances from the directory specified by
     * {@code config.getPath("plugin_dir")}.
     * Non-existent directories, unreadable JARs, or JARs without a service descriptor are
     * silently skipped.
     *
     * @param config the runtime configuration supplying {@code plugin_dir}
     * @return immutable list of loaded plugin instances, in discovery order
     */
    public static List<GCPBuilderPlugin> load(GCPConfig config) {
        return loadFromDirectory(config.getPath(CONFIG_KEY_PLUGIN_DIR));
    }

    /**
     * Loads all {@link GCPBuilderPlugin} instances found in JARs inside {@code pluginsDir}.
     * Non-existent directories, unreadable JARs, or JARs without a service descriptor are
     * silently skipped.
     *
     * @param pluginsDir the directory to scan (may be {@code null} or non-existent)
     * @return immutable list of loaded plugin instances, in discovery order
     */
    public static List<GCPBuilderPlugin> loadFromDirectory(Path pluginsDir) {
        List<GCPBuilderPlugin> plugins = new ArrayList<>();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) return plugins;

        try (var stream = Files.list(pluginsDir)) {
            stream
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.startsWith(PLUGIN_JAR_PREFIX) && name.endsWith(".jar");
                })
                .forEach(jarPath -> loadFromJar(jarPath, plugins));
        } catch (IOException e) {
            System.err.println("[PluginLoader] Failed to scan plugins directory: " + pluginsDir + " — " + e.getMessage());
        }
        return List.copyOf(plugins);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static void loadFromJar(Path jarPath, List<GCPBuilderPlugin> sink) {
        try {
            URL jarUrl = jarPath.toUri().toURL();
            // Parent = current class loader so plugin code can reference gcp Value types.
            URLClassLoader loader = new URLClassLoader(
                new URL[]{jarUrl},
                PluginLoader.class.getClassLoader()
            );
            ServiceLoader.load(GCPBuilderPlugin.class, loader).forEach(plugin -> {
                sink.add(plugin);
                System.out.println("[PluginLoader] Registered plugin: __" + plugin.id() + "__ from " + jarPath.getFileName());
            });
        } catch (Exception e) {
            System.err.println("[PluginLoader] Failed to load plugin from " + jarPath + " — " + e.getMessage());
        }
    }
}
