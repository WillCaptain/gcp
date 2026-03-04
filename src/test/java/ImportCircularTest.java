import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outline.decorators.LazyModuleSymbol;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.STRING;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.twelve.gcp.common.Tool.cast;

/**
 * Tests for mutual (circular) module imports.
 * <p>
 * All scenarios verify that ASF.infer() correctly resolves symbol types even when
 * two or more modules import from each other, thanks to the LazyModuleSymbol
 * mechanism and the two-phase pre-registration in ASF.infer().
 */
public class ImportCircularTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Sets namespace + module name on an AST.
     * Program.setNamespace pops the last element as the module name;
     * the rest become the namespace.  E.g. setNamespace(ast,"org","alpha")
     * yields namespace="org", module name="alpha".
     */
    private static void setNamespace(AST ast, String ns, String moduleName) {
        List<Identifier> parts = new ArrayList<>();
        parts.add(new Identifier(ast, new Token<>(ns)));
        parts.add(new Identifier(ast, new Token<>(moduleName)));
        ast.setNamespace(parts);
    }

    /**
     * Declares   {@code let <name> = <literal>}
     * Returns the VariableDeclarator so callers can inspect its inferred outline.
     */
    private static VariableDeclarator declareWithValue(AST ast, String name, Object literal) {
        VariableDeclarator decl = new VariableDeclarator(ast, VariableKind.LET);
        decl.declare(new Identifier(ast, new Token<>(name)),
                LiteralNode.parse(ast, new Token<>(literal)));
        ast.addStatement(decl);
        return decl;
    }

    /**
     * Declares   {@code let <name>: <type> = <literal>}
     */
    private static VariableDeclarator declareWithTypeAndValue(AST ast, String name,
                                                               String typeName, Object literal) {
        VariableDeclarator decl = new VariableDeclarator(ast, VariableKind.LET);
        decl.declare(new Identifier(ast, new Token<>(name)),
                new IdentifierTypeNode(new Identifier(ast, new Token<>(typeName))),
                LiteralNode.parse(ast, new Token<>(literal)));
        ast.addStatement(decl);
        return decl;
    }

    /**
     * Exports {@code export <name>} (exported name == local name).
     */
    private static void exportSymbol(AST ast, String name) {
        List<Pair<Identifier, Identifier>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Identifier(ast, new Token<>(name)), null));
        ast.addExport(new Export(vars));
    }

    /**
     * Exports {@code export <local> as <exported>}.
     */
    private static void exportSymbolAs(AST ast, String local, String exported) {
        List<Pair<Identifier, Identifier>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Identifier(ast, new Token<>(local)),
                new Identifier(ast, new Token<>(exported))));
        ast.addExport(new Export(vars));
    }

    /**
     * Imports {@code import <importedName> as <localName> from <sourceModule>}.
     * The source module is in the same namespace as the importing AST.
     */
    private static Import importSymbol(AST ast, String importedName, String localName,
                                        String sourceModule) {
        List<Pair<Identifier, Identifier>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Identifier(ast, new Token<>(importedName)),
                new Identifier(ast, new Token<>(localName))));
        List<Identifier> source = new ArrayList<>();
        source.add(new Identifier(ast, new Token<>(sourceModule)));
        Import imp = new Import(vars, source);
        ast.addImport(imp);
        return imp;
    }

    /**
     * Imports {@code import <name> from <sourceModule>} (local name == imported name).
     */
    private static Import importSymbol(AST ast, String name, String sourceModule) {
        return importSymbol(ast, name, name, sourceModule);
    }

    // ── tests ────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * module org.alpha
     *   import val_b from beta
     *   let val_a: Integer = 10
     *   export val_a
     *
     * module org.beta
     *   import val_a from alpha
     *   let val_b: String = "hello"
     *   export val_b
     * </pre>
     * Types are independent (no cross-type dependency), only values cross the boundary.
     * Both imports should resolve after two rounds.
     */
    @Test
    void test_simple_mutual_import() {
        ASF asf = new ASF();

        AST alpha = asf.newAST();
        setNamespace(alpha, "org", "alpha");
        Import alphaImport = importSymbol(alpha, "val_b", "beta");
        declareWithTypeAndValue(alpha, "val_a", "Integer", 10);
        exportSymbol(alpha, "val_a");

        AST beta = asf.newAST();
        setNamespace(beta, "org", "beta");
        Import betaImport = importSymbol(beta, "val_a", "alpha");
        declareWithTypeAndValue(beta, "val_b", "String", "hello");
        exportSymbol(beta, "val_b");

        assertTrue(asf.infer(), "ASF should fully infer");
        assertTrue(asf.allErrors().isEmpty(), "No type errors expected");

        // alpha imports val_b → String
        assertInstanceOf(STRING.class, alphaImport.specifiers().getFirst().outline(),
                "alpha.val_b should be String");

        // beta imports val_a → Integer
        assertInstanceOf(INTEGER.class, betaImport.specifiers().getFirst().outline(),
                "beta.val_a should be Integer");

        // check global module symbols
        Module alphaModule = asf.get("alpha").symbolEnv().module();
        assertInstanceOf(INTEGER.class, alphaModule.getSymbol("val_a"));

        Module betaModule = asf.get("beta").symbolEnv().module();
        assertInstanceOf(STRING.class, betaModule.getSymbol("val_b"));
    }

    /**
     * Same as above but alpha is processed <em>second</em> (beta first in the ASF list).
     * Verifies order-independence: whichever module comes first, the result is the same.
     */
    @Test
    void test_simple_mutual_import_reversed_order() {
        ASF asf = new ASF();

        // beta is registered first this time
        AST beta = asf.newAST();
        setNamespace(beta, "org", "beta");
        Import betaImport = importSymbol(beta, "val_a", "alpha");
        declareWithTypeAndValue(beta, "val_b", "String", "world");
        exportSymbol(beta, "val_b");

        AST alpha = asf.newAST();
        setNamespace(alpha, "org", "alpha");
        Import alphaImport = importSymbol(alpha, "val_b", "beta");
        declareWithTypeAndValue(alpha, "val_a", "Integer", 99);
        exportSymbol(alpha, "val_a");

        assertTrue(asf.infer(), "ASF should fully infer regardless of AST order");
        assertTrue(asf.allErrors().isEmpty());

        assertInstanceOf(STRING.class, alphaImport.specifiers().getFirst().outline());
        assertInstanceOf(INTEGER.class, betaImport.specifiers().getFirst().outline());
    }

    /**
     * <pre>
     * module org.alpha
     *   import score from beta
     *   let rating: Integer = 5
     *   export rating
     *
     * module org.beta
     *   import rating from alpha
     *   let score: Integer = 100
     *   export score
     * </pre>
     * Both imported symbols have the same primitive type (Integer).
     */
    @Test
    void test_mutual_import_same_type() {
        ASF asf = new ASF();

        AST alpha = asf.newAST();
        setNamespace(alpha, "ns", "alpha");
        Import alphaImport = importSymbol(alpha, "score", "beta");
        declareWithTypeAndValue(alpha, "rating", "Integer", 5);
        exportSymbol(alpha, "rating");

        AST beta = asf.newAST();
        setNamespace(beta, "ns", "beta");
        Import betaImport = importSymbol(beta, "rating", "alpha");
        declareWithTypeAndValue(beta, "score", "Integer", 100);
        exportSymbol(beta, "score");

        assertTrue(asf.infer());
        assertTrue(asf.allErrors().isEmpty());

        assertInstanceOf(INTEGER.class, alphaImport.specifiers().getFirst().outline());
        assertInstanceOf(INTEGER.class, betaImport.specifiers().getFirst().outline());
    }

    /**
     * Three-way circular import: A → B → C → A.
     * <pre>
     * module m.a  imports c_val from c,  exports a_val: Integer
     * module m.b  imports a_val from a,  exports b_val: String
     * module m.c  imports b_val from b,  exports c_val: Integer
     * </pre>
     * All three should resolve to their declared types after fixed-point iteration.
     */
    @Test
    void test_three_way_circular_import() {
        ASF asf = new ASF();

        AST a = asf.newAST();
        setNamespace(a, "m", "a");
        Import aImport = importSymbol(a, "c_val", "c");
        declareWithTypeAndValue(a, "a_val", "Integer", 1);
        exportSymbol(a, "a_val");

        AST b = asf.newAST();
        setNamespace(b, "m", "b");
        Import bImport = importSymbol(b, "a_val", "a");
        declareWithTypeAndValue(b, "b_val", "String", "from_b");
        exportSymbol(b, "b_val");

        AST c = asf.newAST();
        setNamespace(c, "m", "c");
        Import cImport = importSymbol(c, "b_val", "b");
        declareWithTypeAndValue(c, "c_val", "Integer", 3);
        exportSymbol(c, "c_val");

        assertTrue(asf.infer(), "Three-way circular ASF should fully infer");
        assertTrue(asf.allErrors().isEmpty(), "No type errors in three-way circular import");

        // a imports c_val → Integer
        assertInstanceOf(INTEGER.class, aImport.specifiers().getFirst().outline());
        // b imports a_val → Integer
        assertInstanceOf(INTEGER.class, bImport.specifiers().getFirst().outline());
        // c imports b_val → String
        assertInstanceOf(STRING.class, cImport.specifiers().getFirst().outline());
    }

    /**
     * Module B imports multiple symbols from A, and A imports multiple symbols from B.
     * <pre>
     * module cross.alpha
     *   import { x, y } from beta     (x: Integer, y: String)
     *   let p: Integer = 10
     *   let q: String  = "Q"
     *   export p, q
     *
     * module cross.beta
     *   import { p, q } from alpha    (p: Integer, q: String)
     *   let x: Integer = 20
     *   let y: String  = "Y"
     *   export x, y
     * </pre>
     */
    @Test
    void test_multi_symbol_mutual_import() {
        ASF asf = new ASF();

        AST alpha = asf.newAST();
        setNamespace(alpha, "cross", "alpha");
        // import { x, y } from beta
        List<Pair<Identifier, Identifier>> alphaImportVars = new ArrayList<>();
        alphaImportVars.add(new Pair<>(new Identifier(alpha, new Token<>("x")),
                new Identifier(alpha, new Token<>("x"))));
        alphaImportVars.add(new Pair<>(new Identifier(alpha, new Token<>("y")),
                new Identifier(alpha, new Token<>("y"))));
        List<Identifier> betaSrc = new ArrayList<>();
        betaSrc.add(new Identifier(alpha, new Token<>("beta")));
        Import alphaImport = new Import(alphaImportVars, betaSrc);
        alpha.addImport(alphaImport);
        declareWithTypeAndValue(alpha, "p", "Integer", 10);
        declareWithTypeAndValue(alpha, "q", "String", "Q");
        exportSymbol(alpha, "p");
        exportSymbol(alpha, "q");

        AST beta = asf.newAST();
        setNamespace(beta, "cross", "beta");
        // import { p, q } from alpha
        List<Pair<Identifier, Identifier>> betaImportVars = new ArrayList<>();
        betaImportVars.add(new Pair<>(new Identifier(beta, new Token<>("p")),
                new Identifier(beta, new Token<>("p"))));
        betaImportVars.add(new Pair<>(new Identifier(beta, new Token<>("q")),
                new Identifier(beta, new Token<>("q"))));
        List<Identifier> alphaSrc = new ArrayList<>();
        alphaSrc.add(new Identifier(beta, new Token<>("alpha")));
        Import betaImport = new Import(betaImportVars, alphaSrc);
        beta.addImport(betaImport);
        declareWithTypeAndValue(beta, "x", "Integer", 20);
        declareWithTypeAndValue(beta, "y", "String", "Y");
        exportSymbol(beta, "x");
        exportSymbol(beta, "y");

        assertTrue(asf.infer());
        assertTrue(asf.allErrors().isEmpty());

        // alpha imports x: Integer, y: String
        assertInstanceOf(INTEGER.class, alphaImport.specifiers().getFirst().outline(),
                "alpha.x should be Integer");
        assertInstanceOf(STRING.class, alphaImport.specifiers().getLast().outline(),
                "alpha.y should be String");

        // beta imports p: Integer, q: String
        assertInstanceOf(INTEGER.class, betaImport.specifiers().getFirst().outline(),
                "beta.p should be Integer");
        assertInstanceOf(STRING.class, betaImport.specifiers().getLast().outline(),
                "beta.q should be String");
    }

    /**
     * One module imports an alias, the other uses a different local name.
     * <pre>
     * module alias.src
     *   import grade as level from alias.dest
     *   let myVal: Integer = 42
     *   export myVal
     *
     * module alias.dest
     *   import myVal as imported from alias.src
     *   let grade: String = "A+"
     *   export grade
     * </pre>
     */
    @Test
    void test_mutual_import_with_alias() {
        ASF asf = new ASF();

        AST src = asf.newAST();
        setNamespace(src, "alias", "src");
        Import srcImport = importSymbol(src, "grade", "level", "dest");
        declareWithTypeAndValue(src, "myVal", "Integer", 42);
        exportSymbol(src, "myVal");

        AST dest = asf.newAST();
        setNamespace(dest, "alias", "dest");
        Import destImport = importSymbol(dest, "myVal", "imported", "src");
        declareWithTypeAndValue(dest, "grade", "String", "A+");
        exportSymbol(dest, "grade");

        assertTrue(asf.infer());
        assertTrue(asf.allErrors().isEmpty());

        // src imports grade as level → String
        assertInstanceOf(STRING.class, srcImport.specifiers().getFirst().outline(),
                "'level' (alias for grade) should be String");
        // dest imports myVal as imported → Integer
        assertInstanceOf(INTEGER.class, destImport.specifiers().getFirst().outline(),
                "'imported' (alias for myVal) should be Integer");
    }

    /**
     * LazyModuleSymbol should NOT remain in the ImportSpecifier after the fixed-point
     * converges.  Verifies that the lazy placeholder is fully replaced by the concrete type.
     */
    @Test
    void test_lazy_placeholder_resolves_completely() {
        ASF asf = new ASF();

        AST a = asf.newAST();
        setNamespace(a, "lazy", "a");
        Import aImport = importSymbol(a, "bStr", "b");
        declareWithTypeAndValue(a, "aInt", "Integer", 7);
        exportSymbol(a, "aInt");

        AST b = asf.newAST();
        setNamespace(b, "lazy", "b");
        Import bImport = importSymbol(b, "aInt", "a");
        declareWithTypeAndValue(b, "bStr", "String", "lazy_resolved");
        exportSymbol(b, "bStr");

        assertTrue(asf.infer());

        Outline aSpecifierOutline = aImport.specifiers().getFirst().outline();
        Outline bSpecifierOutline = bImport.specifiers().getFirst().outline();

        // Neither specifier should still hold a LazyModuleSymbol
        assertFalse(aSpecifierOutline instanceof LazyModuleSymbol,
                "alpha specifier outline must not be a lazy placeholder after inference");
        assertFalse(bSpecifierOutline instanceof LazyModuleSymbol,
                "beta specifier outline must not be a lazy placeholder after inference");

        assertInstanceOf(STRING.class, aSpecifierOutline);
        assertInstanceOf(INTEGER.class, bSpecifierOutline);
    }

    /**
     * Importing a symbol that does not exist in the source module must produce a
     * VARIABLE_NOT_DEFINED error, not silently succeed or leave a lazy placeholder.
     * <pre>
     * module err.a  imports nonexistent from err.b
     * module err.b  exports real_val: Integer
     * </pre>
     */
    @Test
    void test_import_nonexistent_symbol_reports_error() {
        ASF asf = new ASF();

        AST a = asf.newAST();
        setNamespace(a, "err", "a");
        importSymbol(a, "nonexistent", "b");

        AST b = asf.newAST();
        setNamespace(b, "err", "b");
        declareWithTypeAndValue(b, "real_val", "Integer", 1);
        exportSymbol(b, "real_val");

        asf.infer();

        assertTrue(asf.hasErrors(), "An error should be reported for missing symbol");
        assertTrue(asf.allErrors().stream()
                        .anyMatch(e -> e.errorCode() == GCPErrCode.VARIABLE_NOT_DEFINED),
                "Error code should be VARIABLE_NOT_DEFINED");
    }

    /**
     * Importing from a module that does not exist in the ASF at all must produce a
     * MODULE_NOT_DEFINED error without throwing an exception.
     */
    @Test
    void test_import_nonexistent_module_reports_error() {
        ASF asf = new ASF();

        AST a = asf.newAST();
        setNamespace(a, "missing", "a");
        importSymbol(a, "something", "ghost_module");

        asf.infer();

        assertTrue(asf.hasErrors(), "An error should be reported for missing module");
        assertTrue(asf.allErrors().stream()
                        .anyMatch(e -> e.errorCode() == GCPErrCode.MODULE_NOT_DEFINED),
                "Error code should be MODULE_NOT_DEFINED");
    }

    /**
     * Regression: the existing one-way import (education → human direction) must
     * continue to work exactly as before after the mutual-import refactoring.
     * This mirrors the scenario in InferenceTest.test_initialization_inference().
     */
    @Test
    void test_regression_one_way_import_still_works() {
        ASF asf = ASTHelper.educationAndHuman();
        assertTrue(asf.infer(), "One-way import must still fully infer");

        AST education = cast(asf.get("education"));
        AST human = cast(asf.get("human"));

        Export educationExports = education.program().body().exports().getFirst();
        assertInstanceOf(INTEGER.class, educationExports.specifiers().getFirst().outline(),
                "education.grade should be Integer");
        assertInstanceOf(STRING.class, educationExports.specifiers().getLast().outline(),
                "education.college should be String");

        Import humanImports = human.program().body().imports().getFirst();
        assertInstanceOf(INTEGER.class, humanImports.specifiers().getFirst().outline(),
                "human imported level (grade) should be Integer");
        assertInstanceOf(STRING.class, humanImports.specifiers().get(1).outline(),
                "human imported school (college) should be String");

        assertTrue(education.errors().isEmpty());
    }

    /**
     * A module may import from itself (self-import) — unlikely in practice but must
     * not hang or throw.  The symbols simply reference the module's own exports.
     * <pre>
     * module self.loop
     *   let val: Integer = 1
     *   export val
     *   import val as selfVal from self.loop   (same module, same namespace)
     * </pre>
     * Self-import is treated as a same-module forward reference; the symbol must resolve.
     */
    @Test
    void test_self_import_does_not_deadlock() {
        ASF asf = new ASF();

        AST loop = asf.newAST();
        setNamespace(loop, "self", "loop");
        declareWithTypeAndValue(loop, "val", "Integer", 1);
        exportSymbol(loop, "val");
        importSymbol(loop, "val", "selfVal", "loop");

        // Must not hang or throw
        asf.infer();

        // selfVal should resolve to Integer (same as val)
        Import selfImport = loop.program().body().imports().getFirst();
        assertInstanceOf(INTEGER.class, selfImport.specifiers().getFirst().outline(),
                "Self-imported val should be Integer");
    }
}
