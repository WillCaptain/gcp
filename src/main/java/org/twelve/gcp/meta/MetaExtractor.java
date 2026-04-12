package org.twelve.gcp.meta;

import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.node.expression.Variable;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.node.expression.typeable.ExtendTypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.statement.OutlineDeclarator;
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

    // ── Outline name index ───────────────────────────────────────────────────

    /**
     * Inverse index built once per meta-extraction call.
     * Maps outline id → declared name (PascalCase preferred) and structural
     * toString → name for ProductADT types, so {@link #lookupOutlineName(Outline, OutlineNameIndex)}
     * runs in O(1) instead of the previous O(scopes × symbols) full scan.
     */
    private record OutlineNameIndex(Map<Long, String> byId, Map<String, String> byStructural) {

        static OutlineNameIndex build(LocalSymbolEnvironment env) {
            Map<Long, String> byId  = new HashMap<>();
            Map<String, String> bySt = new HashMap<>();
            for (AstScope scope : env.allScopes()) {
                addToIndex(byId, bySt, scope.outlineDefinitions());
                addToIndex(byId, bySt, scope.symbols());
            }
            return new OutlineNameIndex(byId, bySt);
        }

        private static void addToIndex(Map<Long, String> byId, Map<String, String> bySt,
                                       Map<String, EnvSymbol> symbols) {
            for (Map.Entry<String, EnvSymbol> entry : symbols.entrySet()) {
                Outline o = entry.getValue().outline();
                if (o == null) continue;
                String name = entry.getKey();
                boolean newIsType = !name.isEmpty() && Character.isUpperCase(name.charAt(0));
                // id index
                String existing = byId.get(o.id());
                boolean oldIsType = existing != null && !existing.isEmpty()
                        && Character.isUpperCase(existing.charAt(0));
                if (existing == null || (newIsType && !oldIsType)) {
                    byId.put(o.id(), name);
                }
                // structural index for ProductADT (handles generic instantiation id mismatch)
                if (o instanceof ProductADT) {
                    String structural = o.toString();
                    if (structural != null) {
                        String existingS = bySt.get(structural);
                        boolean oldIsTypeS = existingS != null && !existingS.isEmpty()
                                && Character.isUpperCase(existingS.charAt(0));
                        if (existingS == null || (newIsType && !oldIsTypeS)) {
                            bySt.put(structural, name);
                        }
                    }
                }
            }
        }

        String lookup(Outline outline) {
            if (outline == null) return null;
            String name = byId.get(outline.id());
            if (name != null) return name;
            if (outline instanceof ProductADT) {
                String structural = outline.toString();
                return structural != null ? byStructural.get(structural) : null;
            }
            return null;
        }
    }

    // ── Top-level declarations from root scope ──────────────────────────────

    private static List<SchemaMeta> extractNodes(LocalSymbolEnvironment env, String source) {
        List<SchemaMeta> nodes = new ArrayList<>();
        AstScope root = env.root();
        OutlineNameIndex nameIndex = OutlineNameIndex.build(env);

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
                nodes.add(buildFunctionMeta(sym, env, nameIndex, source));
            } else {
                nodes.add(buildVariableMeta(sym, nameIndex, source));
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
            try { padt.loadBuiltInMethods(); } catch (Throwable ignored) {}
            Set<String> baseMemberNames;
            try {
                baseMemberNames = (padt instanceof Entity entity) ? baseMemberNames(entity) : Set.of();
            } catch (Throwable t) {
                baseMemberNames = Set.of();
            }
            for (EntityMember member : padt.members()) {
                try {
                    String memberName = member.name();
                    // Skip private FK/internal fields (_fieldName) and the synthetic 'type' discriminant
                    if (memberName == null || memberName.startsWith("_") || "type".equals(memberName)) continue;
                    // member.outline().toString() can cause StackOverflowError for ~this
                    // self-referential types (e.g. Aggregator methods returning ~this).
                    // Catch Throwable so the member is still included with a safe type string.
                    String mType;
                    try {
                        mType = member.outline() != null ? member.outline().toString() : "?";
                    } catch (Throwable t) {
                        mType = "~this";
                    }
                    String desc = memberDescription(member, source);
                    String origin = memberOrigin(member, baseMemberNames);
                    result.add(new FieldMeta(memberName, mType, desc, origin));
                } catch (Throwable ignored) {}
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
        if (offset <= 0 || offset >= source.length()) return null;
        return CommentExtractor.precedingComment(source, offset);
    }

    // ── Variable meta ───────────────────────────────────────────────────────

    private static VariableMeta buildVariableMeta(EnvSymbol sym, OutlineNameIndex nameIndex, String source) {
        String type = resolveTypeName(sym.outline(), nameIndex);
        String desc = descriptionForSymbol(sym, source);
        return new VariableMeta(sym.name(), sym.mutable() ? "var" : "let", type, sym.mutable(), desc);
    }

    // ── Function meta ───────────────────────────────────────────────────────

    private static FunctionMeta buildFunctionMeta(EnvSymbol sym, LocalSymbolEnvironment env,
                                                   OutlineNameIndex nameIndex, String source) {
        String type = outlineTypeText(sym.outline());
        String desc = descriptionForSymbol(sym, source);

        List<FieldMeta> params = new ArrayList<>();
        for (AstScope scope : env.allScopes()) {
            if (scope == env.root()) continue;
            if (scope.parent() != env.root()) continue;
            for (EnvSymbol s : scope.symbols().values()) {
                if (s.node() instanceof Argument) {
                    String argType = resolveTypeName(s.outline(), nameIndex);
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
        OutlineNameIndex nameIndex = OutlineNameIndex.build(env);

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
                symbols.add(new SymbolMeta(sym.name(), resolveTypeName(sym.outline(), nameIndex), "outline", false));
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
                symbols.add(new SymbolMeta(sym.name(), resolveTypeName(sym.outline(), nameIndex), kind, sym.mutable()));
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
            // Priority order for member resolution (IDE dot-completion / LLM tooling):
            //   1. extendToBe     — upper bound from actual assigned/projected value
            //   2. projectedType  — concrete entity recorded by projectEntity (lambda params);
            //                       not part of the inference constraint chain, safe to read here
            //   3. declaredToBe   — explicit programmer annotation  (e.g. (c: Country) -> ...)
            //   4. min()          — merges hasToBe + definedToBe as parallel lower-bound constraints
            //                       (e.g. `y=x; x.age-1` → x: {name:String, age:Int}, not just {name:String})
            //   5. hasToBe        — usage constraint (fallback when min() is not concrete)
            //   6. definedToBe    — structural access pattern (fallback)
            Outline ext = g.extendToBe();
            if (isConcrete(ext)) return resolveOutline(ext);

            Outline proj = g.projectedType();
            if (isConcrete(proj)) return resolveOutline(proj);

            Outline decl = g.declaredToBe();
            if (isConcrete(decl)) return resolveOutline(decl);

            // hasToBe and definedToBe are parallel independent lower-bound constraints.
            // Use min() to merge them (same as Genericable.guess()) instead of
            // picking one arbitrarily — e.g. x in `y={name="will"}; y=x; x.age-1`
            // must resolve to {name:String, age:Int}, not just {name:String}.
            Outline min = g.min();
            if (isConcrete(min)) return resolveOutline(min);

            Outline has = g.hasToBe();
            if (isConcrete(has)) return resolveOutline(has);

            Outline def = g.definedToBe();
            if (isConcrete(def)) return resolveOutline(def);
        }
        return outline.eventual();
    }

    /**
     * Returns {@code true} when {@code o} represents a concrete, usable type —
     * i.e. not null, not an unresolved wrapper, not a trivial top/bottom type.
     * Used by {@link #resolveOutline} to decide whether a constraint dimension
     * carries enough information to drive dot-completion.
     */
    private static boolean isConcrete(Outline o) {
        return o != null
                && !(o instanceof Genericable)
                && !(o instanceof UNKNOWN)
                && !(o instanceof ANY)
                && !(o instanceof org.twelve.gcp.outline.primitive.NOTHING);
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

    /**
     * Returns completable members from a type text in the current module metadata.
     *
     * <p>This is the canonical fallback for structural inline types such as
     * {@code {age:Int,name:String,map:Poly(() → {...})}} which already contain
     * enough information for dot-completion without needing a named outline lookup.
     */
    public static List<FieldMeta> completionMembersOfType(String typeText, ModuleMeta moduleMeta) {
        if (typeText == null || typeText.isBlank() || moduleMeta == null) return List.of();
        return moduleMeta.membersOfType(typeText);
    }

    /**
     * Returns completable members of a method's return type using the receiver's type.
     *
     * <p>If the method returns a self-like placeholder such as {@code {...}}
     * (common for {@code () -> this} / projected self-returning methods), this
     * falls back to the receiver members instead of returning an empty result.
     */
    public static List<FieldMeta> completionMembersOfMethodReturn(String receiverTypeText,
                                                                  String methodName,
                                                                  ModuleMeta moduleMeta) {
        if (receiverTypeText == null || receiverTypeText.isBlank()
                || methodName == null || methodName.isBlank()
                || moduleMeta == null) {
            return List.of();
        }
        List<FieldMeta> receiverMembers = moduleMeta.membersOfType(receiverTypeText);
        for (FieldMeta field : receiverMembers) {
            if (!methodName.equals(field.name()) || !field.isMethod()) continue;
            String returnType = extractMethodReturnTypeText(field.type());
            if (looksLikeSelfReturn(returnType)) {
                return receiverMembers;
            }
            return completionMembersOfType(returnType, moduleMeta);
        }
        return List.of();
    }

    /**
     * Outline overload for {@link #completionMembersOfMethodReturn(String, String, ModuleMeta)}.
     */
    public static List<FieldMeta> completionMembersOfMethodReturn(Outline receiverOutline,
                                                                  String methodName,
                                                                  ModuleMeta moduleMeta) {
        if (receiverOutline == null) return List.of();
        Outline resolved = resolveOutline(receiverOutline);
        String typeText = resolved != null ? resolved.toString() : receiverOutline.toString();
        return completionMembersOfMethodReturn(typeText, methodName, moduleMeta);
    }

    /**
     * Canonical IDE dot-completion entry point.
     *
     * <p>Given the {@link Outline} of the expression before the dot, returns its completable members:
     * <ol>
     *   <li>Unwraps {@link Returnable}/{@link Genericable} wrappers via {@link #resolveOutline}.</li>
     *   <li>Extracts members via {@link #extractEntityFields} (own + base + builtin).</li>
     *   <li>If the result is trivial (empty or only {@code to_str}) AND a context {@link ASF} is
     *       provided, falls back to AST-declared-body lookup by the entity's declared name — this
     *       covers system generics ({@code Aggregator}, {@code GroupBy}, …) whose projected
     *       instances may not carry full member lists after inference.</li>
     * </ol>
     *
     * <p>All callers — playground service, LLM tools, IDE — should use this method instead of
     * calling {@link #fieldsOf(Outline)} directly, because this method additionally handles the
     * AST-body fallback transparently.
     *
     * @param outline    Outline of the expression before the dot (may be a wrapper type)
     * @param contextAsf the preamble ASF for fallback AST body lookup; may be {@code null}
     * @return list of completable members, never {@code null}
     */
    public static List<FieldMeta> completionMembersOf(Outline outline, ASF contextAsf) {
        Outline resolved = resolveOutline(outline);
        if (resolved == null) return List.of();

        String source = resolved.ast() != null ? resolved.ast().sourceCode() : null;
        List<FieldMeta> fields = extractEntityFields(resolved, source);

        if (!isTrivialResult(fields) || contextAsf == null) return fields;

        // AST fallback: look up the entity's declared body by its declared name in the preamble ASF.
        // This handles system-typed outlines (e.g. Aggregator<School>, GroupBy<...>) whose
        // instantiated projections may not carry full member lists after inference.
        if (resolved instanceof Entity ent && ent.node() != null) {
            String declaredName = ent.node().lexeme();
            if (declaredName != null && !declaredName.isEmpty()) {
                for (AST ast : contextAsf.asts()) {
                    List<FieldMeta> astFields = fieldsOf(declaredName, ast);
                    if (!astFields.isEmpty()) return astFields;
                }
            }
        }

        return fields;
    }

    /** Convenience overload when no context ASF is available. */
    public static List<FieldMeta> completionMembersOf(Outline outline) {
        return completionMembersOf(outline, null);
    }

    /**
     * Unified dot-completion entry point for IDE editors and LLM tools.
     *
     * <p>This is the <em>single authoritative method</em> that both the Outline playground editor
     * and the Entitir playground editor (and any LLM {@code getMembers} tool) must call when
     * resolving what appears after a {@code .} — regardless of whether the expression before the
     * dot is a plain entity, a VirtualSet collection, or a lazy/Genericable wrapper.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Unwraps all wrappers ({@link Returnable}, {@link Genericable}, lazy) via
     *       {@link #resolveOutline} + {@code .eventual()}.</li>
     *   <li><b>VirtualSet collection</b> (e.g. {@code Employees}, {@code Schools}): uses AST
     *       by-name lookup to reliably read own navigation methods from the declared outline body,
     *       then appends {@code VirtualSet} builtin operators ({@code filter}, {@code count}, …).
     *       Direct {@code outline.members()} is intentionally avoided here because GCP represents
     *       these as lazy parametric types whose projected instances may carry an incomplete member
     *       list after forked inference.</li>
     *   <li><b>Plain entity / ADT</b>: delegates to {@link #completionMembersOf} which uses
     *       {@code extractEntityFields} with an AST-body fallback for system generics.</li>
     * </ol>
     *
     * @param outline    the {@link Outline} of the expression before the dot
     * @param contextAsf the ASF that contains the type declarations (preamble or current module);
     *                   must contain both the specific collection outline and {@code VirtualSet}
     * @return completable members, never {@code null}
     */
    public static List<FieldMeta> dotCompletionOf(Outline outline, ASF contextAsf) {
        if (outline == null) return List.of();

        // Collection-returning member calls can carry two different "views" at once:
        //   1) outline.eventual()     -> the real projected collection type (e.g. Employees)
        //   2) resolveOutline(outline)-> a narrower supposed return type (e.g. Employee)
        //
        // For dot-completion we must prefer the eventual collection view first; otherwise
        // chains like badgeRequests.employee(). would be incorrectly collapsed to Employee
        // entity fields instead of Employees VirtualSet operators.
        Outline eventual = outline.eventual();
        if (isVirtualSetCollection(eventual)) {
            return virtualSetDotCompletion(eventual, contextAsf);
        }

        Outline resolved = resolveOutline(outline);
        if (resolved == null) return List.of();
        resolved = resolved.eventual();

        if (isVirtualSetCollection(resolved)) {
            return virtualSetDotCompletion(resolved, contextAsf);
        }
        return completionMembersOf(resolved, contextAsf);
    }

    /**
     * Returns own navigation methods declared on the VirtualSet collection outline
     * (looked up by declared name in the AST body) plus the universal {@code VirtualSet}
     * builtin operators ({@code filter}, {@code count}, {@code take}, …).
     *
     * <p>This method is the canonical way to resolve members of any
     * {@code outline Xs = VirtualSet<X>{...}} — it never relies on walking the projected
     * {@link org.twelve.gcp.outline.adt.Entity#members()} list, which is unreliable for
     * lazy parametric types produced by GCP's generic instantiation.
     */
    public static List<FieldMeta> virtualSetDotCompletion(Outline resolved, ASF contextAsf) {
        if (contextAsf == null) return List.of();
        String outlineName = null;
        if (resolved instanceof Entity ent && ent.node() != null) {
            outlineName = ent.node().lexeme();
        }
        List<FieldMeta> result = new ArrayList<>();
        Set<String> ownNames = new HashSet<>();
        if (outlineName != null && !outlineName.isEmpty()) {
            for (AST ast : contextAsf.asts()) {
                List<FieldMeta> own = fieldsOf(outlineName, ast);
                if (!own.isEmpty()) {
                    result.addAll(own);
                    own.forEach(f -> ownNames.add(f.name()));
                    break;
                }
            }
        }
        for (AST ast : contextAsf.asts()) {
            List<FieldMeta> builtin = fieldsOf("VirtualSet", ast);
            if (!builtin.isEmpty()) {
                for (FieldMeta fm : builtin) {
                    if (!ownNames.contains(fm.name())) {
                        result.add(fm);
                    }
                }
                break;
            }
        }
        return result;
    }

    /**
     * Returns {@code true} when {@code fields} carries no useful member information beyond the
     * universal {@code to_str} builtin — i.e., inference did not fully resolve the type.
     */
    private static boolean isTrivialResult(List<FieldMeta> fields) {
        if (fields.isEmpty()) return true;
        if (fields.size() == 1 && "to_str".equals(fields.get(0).name())) return true;
        return false;
    }

    private static String extractMethodReturnTypeText(String rawType) {
        if (rawType == null || rawType.isBlank()) return null;
        int asciiArrow = rawType.lastIndexOf("->");
        int unicodeArrow = rawType.lastIndexOf("→");
        String tail;
        if (asciiArrow >= 0) tail = rawType.substring(asciiArrow + 2);
        else if (unicodeArrow >= 0) tail = rawType.substring(unicodeArrow + 1);
        else tail = rawType;
        tail = tail.trim();
        while (tail.startsWith("Poly(") && tail.endsWith(")")) {
            tail = tail.substring("Poly(".length(), tail.length() - 1).trim();
            asciiArrow = tail.lastIndexOf("->");
            unicodeArrow = tail.lastIndexOf("→");
            if (asciiArrow >= 0) tail = tail.substring(asciiArrow + 2).trim();
            else if (unicodeArrow >= 0) tail = tail.substring(unicodeArrow + 1).trim();
        }
        while (tail.startsWith("(") && tail.endsWith(")")) {
            tail = tail.substring(1, tail.length() - 1).trim();
        }
        return tail;
    }

    private static boolean looksLikeSelfReturn(String typeText) {
        if (typeText == null || typeText.isBlank()) return false;
        String t = typeText.trim();
        return "{...}".equals(t) || "{…}".equals(t) || t.contains("{...}") || t.contains("{…}");
    }

    /**
     * Looks up an outline by name in the given AST and returns its declared body members.
     *
     * <p>Unlike {@link #fieldsOf(Outline, String)} which operates on an already-inferred
     * {@link Outline} type, this overload works directly from the AST declaration body.
     * It is the canonical fallback for system generics like {@code Aggregator}, {@code VirtualSet},
     * {@code GroupBy} whose inferred projections may not carry full member lists.
     *
     * <p>Both {@link org.twelve.entitir.ontology.world.OntologyWorld} and the playground service
     * delegate to this method — there is only one implementation of AST-body extraction.
     *
     * @param outlineName name of the outline to look up (e.g. "Aggregator", "VirtualSet", "Schools")
     * @param ast         the AST in which to search
     * @return declared members with {@code origin = "own"}, or empty list if not found
     */
    public static List<FieldMeta> fieldsOf(String outlineName, AST ast) {
        if (outlineName == null || ast == null) return List.of();
        List<FieldMeta> result = new ArrayList<>();
        collectFieldsFromNode(ast.program(), outlineName, ast.sourceCode(), result);
        return result;
    }

    /**
     * Returns {@code true} when {@code outline} represents a VirtualSet-based collection
     * (i.e., an {@link Entity} whose base is a concrete {@link ProductADT}).
     *
     * <p>Use this to distinguish collection symbols (Schools, Cities, …) from plain
     * entities (School, City, Aggregator&lt;T&gt;, …) in dot-completion logic.
     * This is the single authoritative implementation — do not reimplement this check
     * in dependent modules.
     */
    public static boolean isVirtualSetCollection(Outline outline) {
        if (!(outline instanceof Entity entity)) return false;
        try {
            Outline base = entity.base();
            if (base == null) return false;
            return base instanceof ProductADT && base != entity.ast().Any;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean collectFieldsFromNode(Node node, String targetName,
                                                 String src, List<FieldMeta> out) {
        if (node instanceof OutlineDeclarator od) {
            for (OutlineDefinition def : od.definitions()) {
                try {
                    if (!targetName.equals(def.symbolNode().lexeme())) continue;
                    EntityTypeNode body = null;
                    if (def.typeNode() instanceof EntityTypeNode etn) {
                        body = etn;
                    } else if (def.typeNode() instanceof ExtendTypeNode ext) {
                        body = ext.extension();
                    }
                    if (body == null) return true;
                    for (Variable v : body.members()) {
                        try {
                            String name = v.name();
                            if (name == null || name.startsWith("_") || name.equals("type")) continue;
                            String rawType = v.declared() != null ? v.declared().lexeme() : "?";
                            if (rawType != null && rawType.startsWith("#")) continue;
                            String doc = (src != null && v.loc() != null && v.loc().start() > 0)
                                    ? CommentExtractor.precedingComment(src, v.loc().start())
                                    : null;
                            out.add(new FieldMeta(name, rawType, doc, "own"));
                        } catch (Exception ignored) {}
                    }
                    return true;
                } catch (Exception ignored) {}
            }
        }
        for (Node child : node.nodes()) {
            if (collectFieldsFromNode(child, targetName, src, out)) return true;
        }
        return false;
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
    /** Fast O(1) path using a pre-built name index. */
    private static String resolveTypeName(Outline outline, OutlineNameIndex nameIndex) {
        if (outline == null) return "?";
        Outline resolved = resolveOutline(outline);
        if (resolved != outline) {
            String name = nameIndex.lookup(resolved);
            if (name != null) return name;
            return outlineTypeText(resolved);
        }
        String name = nameIndex.lookup(outline);
        if (name != null) return name;
        return outlineTypeText(outline);
    }

    /** Slow O(scopes×symbols) fallback — kept for callers that don't have a pre-built index. */
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
