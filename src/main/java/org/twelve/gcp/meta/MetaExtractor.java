package org.twelve.gcp.meta;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.typeable.TypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;

import java.util.*;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

/**
 * Extracts JavaDoc-like metadata from an AST module.
 * <p>
 * Produces a schema structure suitable for JSON export, including module info,
 * imports, exports, variables, and functions, each with optional descriptions
 * derived from preceding comments in the source.
 */
public final class MetaExtractor {

    /**
     * Builds metadata for a single AST. Returns a Map suitable for JSON serialization.
     */
    public static Map<String, Object> extract(AST ast) {
        Map<String, Object> meta = new LinkedHashMap<>();
        String source = ast.sourceCode();

        // Module
        meta.put("name", ast.name());
        meta.put("namespace", ast.namespace().lexeme());
        meta.put("description", moduleDescription(ast, source));

        // Imports
        List<Map<String, Object>> imports = new ArrayList<>();
        for (Import imp : ast.program().body().imports()) {
            for (ImportSpecifier spec : imp.specifiers()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("symbol", spec.imported().name());
                String local = spec.local().name();
                if (!local.equals(spec.imported().name())) {
                    m.put("as", local);
                }
                m.put("from", imp.source().name().name());
                putDescription(m, imp.loc(), source);
                imports.add(m);
            }
        }
        meta.put("imports", imports);

        // Exports
        List<Map<String, Object>> exports = new ArrayList<>();
        for (Export exp : ast.program().body().exports()) {
            for (ExportSpecifier spec : exp.specifiers()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", spec.local().name());
                if (spec.exported() != spec.local()) {
                    m.put("as", spec.exported().name());
                }
                putDescription(m, exp.loc(), source);
                exports.add(m);
            }
        }
        meta.put("exports", exports);

        // Variables and functions from statements
        List<Map<String, Object>> variables = new ArrayList<>();
        List<Map<String, Object>> functions = new ArrayList<>();

        for (Statement stmt : ast.program().body().statements()) {
            if (stmt instanceof VariableDeclarator vd) {
                for (org.twelve.gcp.node.expression.Assignment a : vd.assignments()) {
                    Map<String, Object> m = varMeta(a, vd.kind().name(), source, vd.loc());
                    if (m != null) variables.add(m);
                }
            }
        }

        // Function nodes may be inside VariableDeclarator (let f = x -> ...)
        for (Statement stmt : ast.program().body().statements()) {
            if (stmt instanceof VariableDeclarator vd) {
                for (org.twelve.gcp.node.expression.Assignment a : vd.assignments()) {
                    Expression rhs = a.rhs();
                    if (rhs instanceof FunctionNode fn) {
                        String funcName = (a.lhs() instanceof Identifier i) ? i.name() : null;
                        if (funcName == null) continue;
                        Map<String, Object> m = funcMeta(funcName, fn, source, vd.loc());
                        if (m != null) functions.add(m);
                    }
                }
            }
        }

        meta.put("variables", variables);
        meta.put("functions", functions);

        return meta;
    }

    private static String moduleDescription(AST ast, String source) {
        if (source == null) return null;
        // Use first content offset so block comment preceding "module" is found
        // (namespace.loc() points at "org.example" etc., after "module ", missing the comment)
        long offset = CommentExtractor.startOfFirstContent(source);
        return CommentExtractor.precedingComment(source, offset);
    }

    private static void putDescription(Map<String, Object> m, Location loc, String source) {
        if (source == null) return;
        long offset = loc != null ? loc.start() : 0;
        if (offset <= 0) return;
        String desc = CommentExtractor.precedingComment(source, offset);
        if (desc != null && !desc.isEmpty()) {
            m.put("description", desc);
        }
    }

    /** Fallback when loc is (0,0): search for "let name" or "var name" in source. */
    private static void putDescriptionFromSearch(Map<String, Object> m, String name, String kind, String source) {
        if (source == null || name == null || kind == null) return;
        String needle = kind.toLowerCase().equals("var") ? "var " + name : "let " + name;
        int idx = source.indexOf(needle);
        if (idx >= 0) {
            String desc = CommentExtractor.precedingComment(source, idx);
            if (desc != null && !desc.isEmpty()) m.put("description", desc);
        }
    }

    private static Map<String, Object> varMeta(org.twelve.gcp.node.expression.Assignment a, String kind, String source, Location stmtLoc) {
        Assignable lhs = a.lhs();
        if (!(lhs instanceof org.twelve.gcp.node.expression.Variable)) return null;
        org.twelve.gcp.node.expression.Variable var = cast(lhs);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", var.name());
        m.put("kind", kind.toLowerCase());
        m.put("mutable", var.mutable());

        TypeNode typeNode = var.declared();
        if (typeNode != null && typeNode.outline() != null) {
            m.put("type", typeNode.outline().toString());
        } else {
            Outline out = lhs.outline();
            if (out != null && !out.toString().contains("Unknown")) {
                m.put("type", out.toString());
            }
        }

        putDescription(m, stmtLoc, source);
        if (!m.containsKey("description")) {
            putDescriptionFromSearch(m, var.name(), kind, source);
        }
        return m;
    }

    private static Map<String, Object> funcMeta(String name, FunctionNode fn, String source, Location stmtLoc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);

        List<Map<String, Object>> params = new ArrayList<>();
        Argument arg = fn.argument();
        if (arg != null && arg.token() != Token.unit()) {
            String argName = arg.name();
            if (argName != null && !argName.isEmpty()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("name", argName);
                if (arg.declared() != null && arg.declared().outline() != null) {
                    p.put("type", arg.declared().outline().toString());
                }
                params.add(p);
            }
        }
        m.put("parameters", params);

        Outline ret = fn.outline();
        if (ret != null && !ret.toString().contains("Unknown")) {
            m.put("returns", ret.toString());
        }

        putDescription(m, stmtLoc, source);
        if (!m.containsKey("description")) {
            putDescriptionFromSearch(m, name, "let", source);
        }
        return m;
    }
}
