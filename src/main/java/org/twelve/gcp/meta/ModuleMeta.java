package org.twelve.gcp.meta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Root metadata for a single AST module. Returned by {@code ast.meta()}.
 * <p>
 * Navigable structure for LLM indexing and IDE autocomplete:
 * <pre>
 * ast.meta()                     → ModuleMeta
 * ast.meta().nodes()             → all top-level declarations (outlines, variables, functions)
 * ast.meta().outlines()          → outline declarations only
 * ast.meta().variables()         → let/var declarations only
 * ast.meta().functions()         → function declarations only
 * ast.meta().imports()           → import specifiers
 * ast.meta().exports()           → export specifiers
 *
 * For an outline:
 * ast.meta().outlines().get(0).fields()   → data fields
 * ast.meta().outlines().get(0).methods()  → method-shaped fields (→)
 * ast.meta().outlines().get(0).members()  → all fields in order
 *
 * Position-aware symbol resolution (IDE / LLM dot-completion):
 * ast.meta().scopeAt(offset)               → ScopeMeta at cursor position
 * ast.meta().resolve("name", offset)       → SymbolMeta visible at position
 * ast.meta().visibleSymbols(offset)        → all symbols visible at position
 * ast.meta().membersOf("name", offset)     → FieldMeta list for dot-completion
 * </pre>
 */
public final class ModuleMeta {

    private final String name;
    private final String namespace;
    private final String description;
    private final List<ImportMeta> imports;
    private final List<ExportMeta> exports;
    private final List<SchemaMeta> nodes;
    private final List<ScopeMeta> scopes;
    private final Map<Long, ScopeMeta> scopeById;

    public ModuleMeta(String name, String namespace, String description,
                      List<ImportMeta> imports, List<ExportMeta> exports,
                      List<SchemaMeta> nodes, List<ScopeMeta> scopes) {
        this.name = name;
        this.namespace = namespace;
        this.description = description;
        this.imports = imports != null ? imports : List.of();
        this.exports = exports != null ? exports : List.of();
        this.nodes = nodes != null ? nodes : List.of();
        this.scopes = scopes != null ? scopes : List.of();
        this.scopeById = this.scopes.stream()
                .collect(Collectors.toMap(ScopeMeta::scopeId, s -> s, (a, b) -> a, LinkedHashMap::new));
    }

    public String name() { return name; }
    public String namespace() { return namespace; }
    public String description() { return description; }
    public List<ImportMeta> imports() { return imports; }
    public List<ExportMeta> exports() { return exports; }

    /** All top-level declarations: outlines, variables, functions in source order. */
    public List<SchemaMeta> nodes() { return nodes; }

    /** All scopes in this module. */
    public List<ScopeMeta> scopes() { return scopes; }

    /** Only outline declarations. */
    public List<OutlineMeta> outlines() {
        return nodes.stream()
                .filter(n -> n instanceof OutlineMeta)
                .map(n -> (OutlineMeta) n)
                .toList();
    }

    /** Only variable (let/var) declarations. */
    public List<VariableMeta> variables() {
        return nodes.stream()
                .filter(n -> n instanceof VariableMeta)
                .map(n -> (VariableMeta) n)
                .toList();
    }

    /** Only function declarations. */
    public List<FunctionMeta> functions() {
        return nodes.stream()
                .filter(n -> n instanceof FunctionMeta)
                .map(n -> (FunctionMeta) n)
                .toList();
    }

    /** Find a declaration by name across all kinds. */
    public SchemaMeta find(String name) {
        return nodes.stream().filter(n -> n.name().equals(name)).findFirst().orElse(null);
    }

    // ── Position-aware scope & symbol resolution ────────────────────────────

    /**
     * Find the innermost scope that contains the given source offset.
     * Picks the scope with the smallest range when multiple scopes overlap.
     */
    public ScopeMeta scopeAt(long offset) {
        ScopeMeta best = null;
        for (ScopeMeta s : scopes) {
            if (s.contains(offset)) {
                if (best == null || s.length() < best.length()) {
                    best = s;
                }
            }
        }
        return best;
    }

    /**
     * Resolve a symbol by name at the given cursor offset.
     * Walks the scope chain from innermost to root, returning the first match.
     */
    public SymbolMeta resolve(String name, long offset) {
        ScopeMeta scope = scopeAt(offset);
        while (scope != null) {
            for (SymbolMeta sym : scope.symbols()) {
                if (sym.name().equals(name)) return sym;
            }
            scope = scope.parentScopeId() != null ? scopeById.get(scope.parentScopeId()) : null;
        }
        return null;
    }

    /**
     * All symbols visible at the given cursor offset.
     * Collects symbols from the innermost scope outward; inner bindings shadow outer ones.
     */
    public List<SymbolMeta> visibleSymbols(long offset) {
        Map<String, SymbolMeta> visible = new LinkedHashMap<>();
        ScopeMeta scope = scopeAt(offset);
        while (scope != null) {
            for (SymbolMeta sym : scope.symbols()) {
                visible.putIfAbsent(sym.name(), sym);
            }
            scope = scope.parentScopeId() != null ? scopeById.get(scope.parentScopeId()) : null;
        }
        return new ArrayList<>(visible.values());
    }

    /**
     * Get members (fields + methods) for dot-completion on a symbol at a given position.
     * Resolves the symbol, finds a matching {@link OutlineMeta} by type name,
     * and returns its members.
     *
     * @return field list for dot-completion, or empty list if not an entity type
     */
    public List<FieldMeta> membersOf(String symbolName, long offset) {
        SymbolMeta sym = resolve(symbolName, offset);
        if (sym == null || sym.type() == null) return List.of();
        return membersOfType(sym.type());
    }

    /**
     * Get members for a type name by looking up a matching OutlineMeta in this module.
     * Matches by outline name (substring check) or by exact type text equality.
     */
    List<FieldMeta> membersOfType(String typeName) {
        if (typeName == null) return List.of();
        for (SchemaMeta n : nodes) {
            if (n instanceof OutlineMeta om) {
                if (typeName.contains(om.name())
                        || (om.type() != null && typeName.equals(om.type()))) {
                    return om.members();
                }
            }
        }
        return List.of();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("namespace", namespace);
        if (description != null) m.put("description", description);
        m.put("imports", imports.stream().map(ImportMeta::toMap).toList());
        m.put("exports", exports.stream().map(ExportMeta::toMap).toList());
        m.put("nodes", nodes.stream().map(SchemaMeta::toMap).toList());
        if (!scopes.isEmpty()) {
            m.put("scopes", scopes.stream().map(ScopeMeta::toMap).toList());
        }
        return m;
    }
}
