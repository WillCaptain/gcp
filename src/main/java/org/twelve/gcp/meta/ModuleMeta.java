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
        List<FieldMeta> all = membersOfType(sym.type());
        // Private members (_-prefixed) are only accessible via `this`
        if ("this".equals(symbolName)) return all;
        return all.stream().filter(f -> !f.name().startsWith("_")).toList();
    }

    /**
     * Get members for a type name by looking up a matching OutlineMeta in this module.
     *
     * <h3>Two-pass strategy</h3>
     * <ol>
     *   <li><b>Structural inline types</b> — if the (normalised) type text looks like
     *       {@code {field1: Type1, field2: Type2}} (e.g. a lambda parameter inferred via
     *       structural typing), it is parsed directly using a nesting-aware parser to
     *       correctly handle nested {@code {}}, {@code ()}, {@code []} brackets inside
     *       field type expressions.</li>
     *   <li><b>Named types</b> — otherwise we search {@code nodes} for an {@link OutlineMeta}
     *       whose name exactly matches {@code typeName}, or whose name is the outer type of a
     *       generic instantiation (e.g. {@code "Aggregator"} matches {@code "Aggregator<School>"}),
     *       or whose declared type text matches exactly.</li>
     * </ol>
     */
    public List<FieldMeta> membersOfType(String typeName) {
        if (typeName == null) return List.of();
        // Normalise: strip backtick wrapping produced by Genericable.toString() ("`{code: String}`")
        String t = typeName.startsWith("`") && typeName.endsWith("`")
                ? typeName.substring(1, typeName.length() - 1) : typeName;
        // Fast path: structural type — parse fields directly, no OutlineMeta lookup.
        // Uses nesting-aware splitting to correctly handle nested entities inside function types,
        // e.g. {sum: ({name:String,city:City}->Number)->{...}} → only "sum" is a top-level field.
        if (t.startsWith("{") && t.endsWith("}")) {
            return parseStructuralFields(t);
        }
        for (SchemaMeta n : nodes) {
            if (n instanceof OutlineMeta om) {
                // Use exact name match or generic outer-type match (Aggregator matches Aggregator<School>)
                // to prevent false substring matches like "School" ⊆ "Aggregator<School>".
                if (t.equals(om.name())
                        || t.startsWith(om.name() + "<")
                        || (om.type() != null && (t.equals(om.type()) || typeName.equals(om.type())))) {
                    return om.members();
                }
            }
        }
        return List.of();
    }

    /**
     * Parses inline structural type notation {@code {field1: Type1, field2: Type2}} into
     * {@link FieldMeta} entries.
     *
     * <p>Uses a nesting-aware comma splitter that tracks {@code {}}, {@code ()}, and {@code []}
     * depth, so commas inside nested type expressions (e.g. function parameter types that expand
     * to {@code {name:String,city:City}}) are not treated as top-level field separators.
     */
    private static List<FieldMeta> parseStructuralFields(String structural) {
        String inner = structural.substring(1, structural.length() - 1).trim();
        if (inner.isEmpty()) return List.of();
        List<FieldMeta> fields = new ArrayList<>();
        for (String part : splitTopLevel(inner)) {
            String trimmed = part.trim();
            int colonIdx = firstTopLevelColon(trimmed);
            if (colonIdx > 0) {
                String fieldName = trimmed.substring(0, colonIdx).trim();
                String fieldType = trimmed.substring(colonIdx + 1).trim();
                if (!fieldName.isEmpty()) {
                    fields.add(new FieldMeta(fieldName, fieldType, null, "inferred"));
                }
            }
        }
        return fields;
    }

    /**
     * Splits {@code s} by commas at nesting depth 0, respecting {@code {}}, {@code ()},
     * and {@code []} brackets.  Does not track {@code <>} to avoid false positives with
     * the {@code ->} arrow operator.
     */
    private static List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '(' || c == '[') depth++;
            else if (c == '}' || c == ')' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (start < s.length()) parts.add(s.substring(start));
        return parts;
    }

    /**
     * Returns the index of the first {@code :} at nesting depth 0 in {@code s},
     * or {@code -1} if none found.
     */
    private static int firstTopLevelColon(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '(' || c == '[') depth++;
            else if (c == '}' || c == ')' || c == ']') depth--;
            else if (c == ':' && depth == 0) return i;
        }
        return -1;
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
