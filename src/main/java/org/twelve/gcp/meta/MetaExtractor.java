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
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.projectable.Function;
import org.twelve.gcp.outlineenv.AstScope;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;
import org.twelve.gcp.outlineenv.SYMBOL_CATEGORY;
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
                nodes.add(buildVariableMeta(sym, source));
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

    private static List<FieldMeta> extractEntityFields(Outline outline, String source) {
        List<FieldMeta> result = new ArrayList<>();
        if (outline instanceof Entity entity) {
            Set<String> baseMemberNames = baseMemberNames(entity);
            for (EntityMember member : entity.members()) {
                String mType = member.outline() != null ? member.outline().toString() : "?";
                String desc = memberDescription(member, source);
                String origin = memberOrigin(member, baseMemberNames);
                result.add(new FieldMeta(member.name(), mType, desc, origin));
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

    private static VariableMeta buildVariableMeta(EnvSymbol sym, String source) {
        String type = outlineTypeText(sym.outline());
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
                    String argType = outlineTypeText(s.outline());
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
                symbols.add(new SymbolMeta(sym.name(), outlineTypeText(sym.outline()), "outline", false));
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
                symbols.add(new SymbolMeta(sym.name(), outlineTypeText(sym.outline()), kind, sym.mutable()));
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

    // ── Helpers ─────────────────────────────────────────────────────────────

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
