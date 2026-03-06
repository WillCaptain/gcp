package org.twelve.gcp.meta;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.projectable.Function;
import org.twelve.gcp.outline.projectable.Genericable;
import org.twelve.gcp.outline.projectable.Returnable;
import org.twelve.gcp.outlineenv.AstScope;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;
import org.twelve.gcp.outlineenv.SYMBOL_CATEGORY;

import java.util.*;

/**
 * Extracts a navigable, typed metadata tree from a module's symbol environment.
 * <p>
 * The primary source of truth is {@link LocalSymbolEnvironment} which holds
 * all scopes and their resolved symbols after type inference. The AST is only
 * used to supplement with comments (via source code), positions (via node locations),
 * and structural info (imports/exports).
 * <p>
 * Produces {@link ModuleMeta} (from {@link AST}) and {@link ForestMeta}
 * (from {@link org.twelve.gcp.ast.ASF}), suitable for JSON export,
 * LLM indexing, and IDE autocomplete.
 */
public final class MetaExtractor {

    public static ModuleMeta extract(AST ast) {
        String source = ast.sourceCode();
        LocalSymbolEnvironment env = ast.symbolEnv();

        String name = ast.name();
        String namespace = ast.namespace().lexeme();
        String description = moduleDescription(ast, source);

        List<ImportMeta> imports = extractImports(ast, source);
        List<ExportMeta> exports = extractExports(ast, source);
        List<SchemaMeta> nodes = extractNodes(env, source);
        List<ScopeMeta> scopes = extractScopes(env, source);

        return new ModuleMeta(name, namespace, description, imports, exports, nodes, scopes);
    }

    // ── Imports (structural AST info, not in symbol env) ────────────────────

    private static List<ImportMeta> extractImports(AST ast, String source) {
        List<ImportMeta> result = new ArrayList<>();
        for (Import imp : ast.program().body().imports()) {
            for (ImportSpecifier spec : imp.specifiers()) {
                String symbol = spec.imported().name();
                String local = spec.local().name();
                String as = local.equals(symbol) ? null : local;
                String from = imp.source().name().name();
                String desc = descriptionFor(imp.loc(), source);
                result.add(new ImportMeta(symbol, as, from, desc));
            }
        }
        return result;
    }

    // ── Exports (structural AST info, not in symbol env) ────────────────────

    private static List<ExportMeta> extractExports(AST ast, String source) {
        List<ExportMeta> result = new ArrayList<>();
        for (Export exp : ast.program().body().exports()) {
            for (ExportSpecifier spec : exp.specifiers()) {
                String localName = spec.local().name();
                String exportedAs = (spec.exported() != spec.local()) ? spec.exported().name() : null;
                String desc = descriptionFor(exp.loc(), source);
                result.add(new ExportMeta(localName, exportedAs, desc));
            }
        }
        return result;
    }

    // ── Top-level declarations from root scope ──────────────────────────────

    private static List<SchemaMeta> extractNodes(LocalSymbolEnvironment env, String source) {
        List<SchemaMeta> nodes = new ArrayList<>();
        AstScope root = env.root();

        Set<String> outlineNames = new HashSet<>();

        for (EnvSymbol sym : root.outlineDefinitions().values()) {
            if (sym.node() == null) continue;
            nodes.add(buildOutlineMeta(sym, source));
            outlineNames.add(sym.name());
        }

        for (EnvSymbol sym : root.symbols().values()) {
            if (sym.node() == null) continue;
            if (!outlineNames.contains(sym.name()) && isOutlineDeclaration(sym)) {
                nodes.add(buildOutlineMeta(sym, source));
                continue;
            }
            Outline outline = sym.outline();
            if (outline != null && outline.eventual() instanceof Function<?, ?>) {
                nodes.add(buildFunctionMeta(sym, env, source));
            } else {
                nodes.add(buildVariableMeta(sym, env, source));
            }
        }

        return nodes;
    }

    // ── Outline meta ────────────────────────────────────────────────────────

    private static OutlineMeta buildOutlineMeta(EnvSymbol sym, String source) {
        String name = sym.name();
        Outline outline = sym.outline();
        String type = outlineTypeText(outline);
        String desc = descriptionForSymbol(sym, source);
        List<FieldMeta> allFields = extractEntityFields(outline, source);
        return new OutlineMeta(name, type, desc, allFields);
    }

    /**
     * Extracts {@link FieldMeta} for all members of the given outline.
     * Handles:
     * <ul>
     *   <li>{@link ProductADT} (Entity, Array, Dict, …) — loads built-in methods,
     *       extracts own / base / builtin members.</li>
     *   <li>{@link Option} — merges members from all option arms (deduplicating by name).</li>
     * </ul>
     */
    private static List<FieldMeta> extractEntityFields(Outline outline, String source) {
        if (outline instanceof Option opt) {
            Map<String, FieldMeta> seen = new LinkedHashMap<>();
            for (Outline arm : opt.options()) {
                for (FieldMeta f : extractEntityFields(arm.eventual(), source)) {
                    seen.putIfAbsent(f.name(), f);
                }
            }
            return new ArrayList<>(seen.values());
        }
        List<FieldMeta> result = new ArrayList<>();
        if (outline instanceof ProductADT padt) {
            try { padt.loadBuiltInMethods(); } catch (Exception ignored) {}
            Set<String> baseMemberNames = (padt instanceof Entity entity)
                    ? baseMemberNames(entity)
                    : Set.of();
            for (EntityMember member : padt.members()) {
                try {
                    String mType = member.outline() != null ? member.outline().toString() : "?";
                    String desc = memberDescription(member, source);
                    String origin = memberOrigin(member, baseMemberNames);
                    result.add(new FieldMeta(member.name(), mType, desc, origin));
                } catch (Exception ignored) {}
            }
        }
        return result;
    }

    private static Set<String> baseMemberNames(Entity entity) {
        Set<String> names = new HashSet<>();
        Outline base = entity.base();
        if (base instanceof ProductADT basePadt && base != entity.ast().Any) {
            for (EntityMember m : basePadt.members()) {
                names.add(m.name());
            }
        }
        return names;
    }

    private static String memberOrigin(EntityMember member, Set<String> baseMemberNames) {
        if (member.isDefault() && !member.hasDefaultValue() && member.node() == null) {
            return "builtin";
        }
        if (baseMemberNames.contains(member.name())) {
            return "base";
        }
        return "own";
    }

    private static String memberDescription(EntityMember member, String source) {
        if (source == null || member.node() == null) return null;
        if (member.node().loc() == null) return null;
        long offset = member.node().loc().start();
        if (offset <= 0) return null;
        return CommentExtractor.precedingComment(source, offset);
    }

    // ── Variable meta ───────────────────────────────────────────────────────

    private static VariableMeta buildVariableMeta(EnvSymbol sym, LocalSymbolEnvironment env, String source) {
        String type = resolveTypeName(sym.outline(), env);
        String desc = descriptionForSymbol(sym, source);
        return new VariableMeta(sym.name(), sym.mutable() ? "var" : "let", type, sym.mutable(), desc);
    }

    // ── Function meta ───────────────────────────────────────────────────────

    private static FunctionMeta buildFunctionMeta(EnvSymbol sym, LocalSymbolEnvironment env, String source) {
        String type = outlineTypeText(sym.outline());
        String desc = descriptionForSymbol(sym, source);

        List<FieldMeta> params = new ArrayList<>();
        for (AstScope scope : env.allScopes()) {
            if (scope == env.root()) continue;
            if (scope.parent() != env.root()) continue;
            for (EnvSymbol s : scope.symbols().values()) {
                if (s.node() instanceof Argument) {
                    String argType = resolveTypeName(s.outline(), env);
                    params.add(new FieldMeta(s.name(), argType, null));
                }
            }
            if (!params.isEmpty()) break;
        }

        String returns = null;
        Outline outline = sym.outline();
        if (outline != null) {
            String s = outline.toString();
            if (s != null && !s.contains("Unknown")) returns = s;
        }

        return new FunctionMeta(sym.name(), type, desc, params, returns);
    }

    // ── Scopes from symbol environment ──────────────────────────────────────

    static List<ScopeMeta> extractScopes(LocalSymbolEnvironment env, String source) {
        List<ScopeMeta> result = new ArrayList<>();

        for (AstScope scope : env.allScopes()) {
            long scopeId = scope.id();
            Long parentScopeId = (scope.parent() != null) ? scope.parent().id() : null;
            if (Objects.equals(scopeId, parentScopeId)) parentScopeId = null;

            long minStart = Long.MAX_VALUE;
            long maxEnd = Long.MIN_VALUE;

            Node scopeNode = scope.node();
            if (scopeNode != null && scopeNode.loc() != null) {
                Location loc = scopeNode.loc();
                if (!(loc.start() == 0 && loc.end() == 0)) {
                    minStart = loc.start();
                    maxEnd = loc.end();
                }
            }

            List<SymbolMeta> symbols = new ArrayList<>();

            for (EnvSymbol sym : scope.outlineDefinitions().values()) {
                if (sym.node() == null) continue;
                expandRange(sym.node(), minStart, maxEnd);
                long[] range = expandRange(sym.node(), minStart, maxEnd);
                minStart = range[0]; maxEnd = range[1];
                symbols.add(new SymbolMeta(sym.name(), resolveTypeName(sym.outline(), env), "outline", false));
            }

            for (EnvSymbol sym : scope.symbols().values()) {
                if (sym.node() == null) continue;
                long[] range = expandRange(sym.node(), minStart, maxEnd);
                minStart = range[0]; maxEnd = range[1];
                String kind;
                if (sym.node() instanceof Argument) {
                    kind = "parameter";
                } else if (sym.outline() != null && sym.outline().eventual() instanceof Function<?, ?>) {
                    kind = "function";
                } else {
                    kind = "variable";
                }
                symbols.add(new SymbolMeta(sym.name(), resolveTypeName(sym.outline(), env), kind, sym.mutable()));
            }

            long s = (minStart == Long.MAX_VALUE) ? 0 : minStart;
            long e = (maxEnd == Long.MIN_VALUE) ? 0 : maxEnd;

            if (parentScopeId != null && source != null) {
                e = extendToStatementEnd(source, e);
            }

            result.add(new ScopeMeta(scopeId, s, e, parentScopeId, List.copyOf(symbols)));
        }

        return result;
    }

    private static long[] expandRange(Identifier node, long minStart, long maxEnd) {
        if (node != null && node.loc() != null) {
            Location loc = node.loc();
            if (!(loc.start() == 0 && loc.end() == 0)) {
                if (loc.start() < minStart) minStart = loc.start();
                if (loc.end() > maxEnd) maxEnd = loc.end();
            }
        }
        return new long[]{minStart, maxEnd};
    }

    private static long extendToStatementEnd(String source, long maxEnd) {
        if (maxEnd >= source.length()) return maxEnd;
        int pos = (int) maxEnd;
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == ';' || c == '}') return pos;
            pos++;
        }
        return maxEnd;
    }

    private static boolean isOutlineDeclaration(EnvSymbol sym) {
        if (sym.category() == SYMBOL_CATEGORY.OUTLINE) return true;
        if (sym.node() == null) return false;
        Node node = sym.node();
        while (node != null) {
            if (node instanceof org.twelve.gcp.node.statement.OutlineDeclarator) return true;
            node = node.parent();
        }
        return false;
    }

    // ── Public outline resolution API ────────────────────────────────────────

    /**
     * Resolves any {@link Returnable} or {@link Genericable} wrapper to its concrete
     * underlying type, recursively.
     *
     * <ul>
     *   <li>{@link Returnable} (e.g. result of {@code countries.first()}) →
     *       {@code ret.supposedToBe()} which holds the actual return type ({@code Country}).</li>
     *   <li>{@link Genericable} (e.g. lambda parameter {@code c} in
     *       {@code filter(c->c.)}) → {@code g.guess()} which extracts the best
     *       concrete type from the four constraint dimensions.</li>
     * </ul>
     *
     * Callers (playground completion services, LLM tools) should use this method
     * instead of duplicating the resolution logic.
     */
    public static Outline resolveOutline(Outline outline) {
        if (outline == null) return null;
        if (outline instanceof Returnable ret) {
            Outline supposed = ret.supposedToBe();
            if (supposed != null && !(supposed instanceof UNKNOWN) && !(supposed instanceof ANY)) {
                return resolveOutline(supposed.eventual());
            }
        }
        if (outline instanceof Genericable<?, ?> g) {
            // For meta/completions purposes we prefer the upper bound (max() = extendToBe,
            // i.e. the actual concrete type that was passed/inferred at the call site) over
            // the lower bound (min() = definedToBe, the minimal structural usage).
            // Example: lambda param `c` in `filter(c -> c.code == "CN")` has
            //   extendToBe = Country  (from filter's (a->Bool) instantiated with a=Country)
            //   definedToBe = {code:String}  (structural access pattern)
            // guess() would return the lower bound {code:String}, losing the full Country type.
            // Using max() restores the complete Country type for LLM and IDE use.
            Outline upper = g.max();
            if (upper != null && !(upper instanceof Genericable)
                    && !(upper instanceof UNKNOWN) && !(upper instanceof ANY)
                    && !(upper instanceof org.twelve.gcp.outline.primitive.NOTHING)) {
                return resolveOutline(upper);
            }
            Outline guessed = g.guess();
            if (guessed != null && !(guessed instanceof Genericable) && !(guessed instanceof UNKNOWN)) {
                return resolveOutline(guessed);
            }
        }
        return outline.eventual();
    }

    /**
     * Returns the {@link FieldMeta} list for any {@link Outline} — resolving
     * {@link Genericable} and {@link Returnable} wrappers first.
     *
     * <p>This is the canonical entry point for dot-completion:
     * instead of each service reimplementing type unwrapping, callers do:
     * <pre>
     *   List&lt;FieldMeta&gt; members = MetaExtractor.fieldsOf(expr.outline());
     * </pre>
     *
     * @param source the module source code used to extract field documentation
     *               (may be {@code null} — documentation is omitted when absent)
     */
    public static List<FieldMeta> fieldsOf(Outline outline, String source) {
        Outline resolved = resolveOutline(outline);
        if (resolved == null) return List.of();
        return extractEntityFields(resolved, source);
    }

    /** Convenience overload when source code is not available. */
    public static List<FieldMeta> fieldsOf(Outline outline) {
        return fieldsOf(outline, null);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolves an outline to a human-readable type name, aware of the current
     * module's symbol environment so that named outlines ({@code Country},
     * {@code Countries}, …) are returned by name rather than by structural
     * {@code toString()} ({@code {code:String,name:String}}).
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Unwrap {@link Returnable}/{@link Genericable} via
     *       {@link #resolveOutline(Outline)}.</li>
     *   <li>Look up the resolved type's id in the environment's outline
     *       definitions to get the declared name.</li>
     *   <li>Fall back to {@link #outlineTypeText(Outline)} (structural
     *       toString).</li>
     * </ol>
     */
    private static String resolveTypeName(Outline outline, LocalSymbolEnvironment env) {
        if (outline == null) return "?";
        Outline resolved = resolveOutline(outline);
        if (resolved != outline) {
            String name = lookupOutlineName(resolved, env);
            if (name != null) return name;
            return outlineTypeText(resolved);
        }
        String name = lookupOutlineName(outline, env);
        if (name != null) return name;
        return outlineTypeText(outline);
    }

    /**
     * Searches all scopes in {@code env} for an outline definition whose
     * resolved {@code id()} matches the given outline.  Returns the declared
     * name (e.g. {@code "Country"}) or {@code null} if not found.
     */
    /**
     * Searches all scopes in {@code env} for a named outline whose type matches the given outline.
     *
     * <h3>Storage layout</h3>
     * <ul>
     *   <li>{@link AstScope#outlineDefinitions()} — built-in primitive types registered via
     *       {@code defineOutline()} (Integer, String, …).</li>
     *   <li>{@link AstScope#symbols()} — BOTH user-declared outline types
     *       ({@code outline Country = {…}}) AND regular variables ({@code let countries = …}).
     *       The Outline parser uses {@code defineSymbol()} for outline declarations, so they
     *       share the same map as variables.  Both a type like {@code Countries} and its
     *       variable {@code countries} will have the same entity id after inference.</li>
     * </ul>
     *
     * <h3>Match + selection strategy</h3>
     * <ol>
     *   <li>Collect all candidates whose entity id matches (or whose structural toString matches).
     *   <li>Prefer a candidate whose name starts with an <em>uppercase</em> letter — this is the
     *       Outline language convention for type declarations (PascalCase), distinguishing
     *       {@code "Countries"} (type) from {@code "countries"} (variable).</li>
     * </ol>
     */
    private static String lookupOutlineName(Outline outline, LocalSymbolEnvironment env) {
        if (outline == null || env == null) return null;
        long id = outline.id();
        String structural = (outline instanceof ProductADT) ? outline.toString() : null;
        String best = null;
        for (AstScope scope : env.allScopes()) {
            // ① built-in type definitions (defineOutline map)
            for (Map.Entry<String, EnvSymbol> entry : scope.outlineDefinitions().entrySet()) {
                best = pickBetter(best, entry.getKey(), entry.getValue().outline(), id, structural);
            }
            // ② all symbols: contains both outline declarations AND variables
            for (Map.Entry<String, EnvSymbol> entry : scope.symbols().entrySet()) {
                best = pickBetter(best, entry.getKey(), entry.getValue().outline(), id, structural);
            }
        }
        return best;
    }

    /**
     * Returns the "better" name between {@code current} and {@code candidate}, given that
     * {@code candidateOutline} matches the target id / structural toString.
     * A name starting with an uppercase letter (type declaration convention) wins over a
     * lowercase name (variable convention) to correctly choose "Countries" over "countries".
     */
    private static String pickBetter(String current, String candidate, Outline candidateOutline,
                                     long targetId, String structural) {
        if (candidateOutline == null) return current;
        boolean matches = candidateOutline.id() == targetId
                || (structural != null && structural.equals(candidateOutline.toString()));
        if (!matches) return current;
        if (current == null) return candidate;
        // Prefer uppercase-starting name (type declaration convention)
        boolean candidateIsType = !candidate.isEmpty() && Character.isUpperCase(candidate.charAt(0));
        boolean currentIsType   = !current.isEmpty()   && Character.isUpperCase(current.charAt(0));
        return (candidateIsType && !currentIsType) ? candidate : current;
    }

    private static String outlineTypeText(Outline outline) {
        if (outline == null) return "?";
        String s = outline.toString();
        return (s == null || s.contains("Unknown")) ? "?" : s;
    }

    private static String moduleDescription(AST ast, String source) {
        if (source == null) return null;
        long offset = CommentExtractor.startOfFirstContent(source);
        return CommentExtractor.precedingComment(source, offset);
    }

    private static String descriptionForSymbol(EnvSymbol sym, String source) {
        if (source == null || sym.node() == null) return null;
        Location loc = sym.node().loc();
        if (loc == null) return null;

        // Walk up to the statement node to find the comment preceding the full statement
        Node stmtNode = sym.node();
        while (stmtNode.parent() != null && !(stmtNode.parent() instanceof org.twelve.gcp.node.expression.body.ProgramBody)) {
            stmtNode = stmtNode.parent();
        }
        long offset = stmtNode.loc() != null ? stmtNode.loc().start() : loc.start();
        if (offset <= 0) {
            offset = loc.start();
        }

        String desc = CommentExtractor.precedingComment(source, offset);
        if (desc != null && !desc.isEmpty()) return desc;

        return descriptionBySearch(sym.name(), source);
    }

    private static String descriptionBySearch(String name, String source) {
        if (source == null || name == null) return null;
        for (String prefix : new String[]{"let " + name, "var " + name, "outline " + name}) {
            int idx = source.indexOf(prefix);
            if (idx >= 0) {
                String desc = CommentExtractor.precedingComment(source, idx);
                if (desc != null && !desc.isEmpty()) return desc;
            }
        }
        return null;
    }

    private static String descriptionFor(Location loc, String source) {
        if (source == null) return null;
        long offset = loc != null ? loc.start() : 0;
        if (offset <= 0) return null;
        String desc = CommentExtractor.precedingComment(source, offset);
        return (desc != null && !desc.isEmpty()) ? desc : null;
    }
}
