package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root metadata for an ASF (Abstract Syntax Forest). Returned by {@code asf.meta()}.
 * <p>
 * Contains metadata for all modules in the forest.
 * <pre>
 * asf.meta()                       → ForestMeta
 * asf.meta().modules()             → List&lt;ModuleMeta&gt;
 * asf.meta().modules().get(0)      → ModuleMeta for first module
 * </pre>
 */
public final class ForestMeta {

    private final List<ModuleMeta> modules;

    public ForestMeta(List<ModuleMeta> modules) {
        this.modules = modules != null ? modules : List.of();
    }

    public List<ModuleMeta> modules() { return modules; }

    /** Find a module by name. */
    public ModuleMeta find(String moduleName) {
        return modules.stream().filter(m -> m.name().equals(moduleName)).findFirst().orElse(null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("modules", modules.stream().map(ModuleMeta::toMap).toList());
        return m;
    }
}
