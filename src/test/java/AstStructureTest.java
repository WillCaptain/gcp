import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.ExportSpecifier;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.imexport.ImportSpecifier;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.builtin.Module;
import org.twelve.gcp.outline.builtin.Namespace;

import java.util.ArrayList;
import java.util.List;

import static org.twelve.gcp.common.Tool.cast;
import static org.junit.jupiter.api.Assertions.*;

public class AstStructureTest {
    private ASF asf;
    private AST ast;


    @BeforeEach
    void before() {
        this.asf = new ASF();
        this.ast = this.asf.newAST();
        ASTHelper.fillHumanAst(ast);
    }

    @Test
    void test_namespace() {
        assertEquals(10, this.ast.program().namespace().loc().start());
        assertEquals(20, this.ast.program().namespace().loc().end());

        assertEquals("org", ast.program().namespace().nodes().get(0).lexeme());
        assertEquals("twelve", ast.program().namespace().nodes().get(1).lexeme());
        assertEquals("human", ast.program().moduleName());
        assertEquals("org.twelve", ast.program().namespace().lexeme());

        //like outline of "package org.twelve.test" is unknown before inference
        assertTrue(ast.program().namespace().outline() instanceof UNKNOWN);
        Namespace org = cast(ast.program().namespace().nodes().get(0).outline());
        // but outline of "org" is namespace
        assertTrue(org instanceof Namespace);
        assertTrue(org.isTop());
        assertEquals(null, org.parentNamespace());
        assertEquals("org", org.namespace());
        //namespace is not assignable, means or can not express alone without tail
        assertFalse(org.reAssignable());

        Namespace twelve = cast(ast.program().namespace().nodes().get(1).outline());
        assertEquals(twelve, org.subNamespaces().get(0));
        assertEquals(org, twelve.parentNamespace());
        assertEquals("twelve", twelve.namespace());

        assertEquals("org.twelve", ast.namespace().lexeme());

        assertEquals(0, ast.program().id());
        assertEquals(2, ast.program().body().id());
        assertEquals(3, ast.program().namespace().id());
        assertEquals(5, ast.program().namespace().nodes().get(0).id());
        assertEquals(7, ast.program().namespace().nodes().get(1).id());
    }


    @Test
    void test_import() {
        Import imported = this.ast.program().body().imports().get(0);
        //check to string
        assertEquals("import grade as level, school from education;", ast.program().body().imports().get(0).lexeme());
        //check location
        assertEquals(29, imported.loc().start());
        assertEquals(53, imported.loc().end());
        //check source
        assertEquals("education", imported.source().lexeme());
        assertEquals(44, imported.source().loc().start());
        assertEquals(53, imported.source().loc().end());
        assertTrue(imported.source().outline() instanceof Module);
        //check a
        ImportSpecifier a = imported.specifiers().get(0);
        assertEquals("grade as level", a.lexeme());
        assertEquals("grade", a.imported().lexeme());
        assertEquals(29, a.imported().loc().start());
        assertEquals(34, a.imported().loc().end());
        assertEquals("level", a.local().lexeme());
        assertEquals(34, a.local().loc().start());
        assertEquals(39, a.local().loc().end());

        assertTrue(a.get(0).outline() instanceof UNKNOWN);//outline is not confirmed yet
        assertTrue(a.get(0).outline() == a.get(1).outline());//outline of b is a reference of a outline
        //check c
        ImportSpecifier c = imported.specifiers().get(1);
        assertTrue(c.get(0) == c.get(1));
        assertEquals("school", c.lexeme());
        assertEquals("school", c.imported().lexeme());
        assertEquals(37, c.imported().loc().start());
        assertEquals(43, c.imported().loc().end());

        //import * from e
        List<Token> source = new ArrayList<>();
        source.add(new Token("e", 22));
        source.add(new Token("f", 22));
        source.add(new Token("g", 22));
        imported = ast.program().body().addImport(new Import(ast, source));
        assertEquals(0, imported.specifiers().size());
        assertEquals("e.f.g", imported.source().lexeme());

    }

    @Test
    void test_export() {
        Export exported = ast.program().body().exports().get(0);
        //check to string
        assertEquals("export height as stature, name;", ast.program().body().exports().get(0).toString());
        //check location
        assertEquals(100, exported.loc().start());
        assertEquals(120, exported.loc().end());
        //check a
        ExportSpecifier a = exported.specifiers().get(0);
        assertEquals("height as stature", a.lexeme());
        assertEquals("height", a.local().lexeme());
        assertEquals(100, a.local().loc().start());
        assertEquals(106, a.local().loc().end());
        assertEquals("stature", a.exported().lexeme());
        assertEquals(111, a.exported().loc().start());
        assertEquals(118, a.exported().loc().end());

        assertTrue(a.get(0).outline() instanceof UNKNOWN);//outline is not confirmed yet
        assertTrue(a.get(0).outline() == a.get(1).outline());//outline of b is a reference of a outline
        //check c
        ExportSpecifier c = exported.specifiers().get(1);
        assertTrue(c.get(0) == c.get(1));
        assertEquals("name", c.lexeme());
        assertEquals("name", c.local().lexeme());
        assertEquals(116, c.local().loc().start());
        assertEquals(120, c.local().loc().end());
    }

    @Test
    void test_variable_declare() {
        List<Statement> stmts = this.ast.program().body().statements();
        VariableDeclarator var = cast(stmts.get(0));
        assertEquals(50, var.loc().start());
        assertEquals(115, var.loc().end());
        assertEquals("let age: Integer, name = \"Will\", height: Decimal = 1.68, grade = level;",
                var.toString());
    }

    @Test
    void test_function_definition(){
        AST ast = ASTHelper.mockAddFunc();
        String expected = "let add = x->{" +
                "  y->{" +
                "    x+y" +
                "  }" +
                "};";
        assertEquals(expected,ast.lexeme().replace("\n",""));
    }

    @Test
    void test_non_argument_function(){
        AST ast = new ASF().newAST();
        FunctionBody body = new FunctionBody(ast);
        FunctionNode function = FunctionNode.from(body);
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token("get",0),function);
        ast.addStatement(declare);
        assertEquals(Token.unit().lexeme(),function.argument().identifier().token());
    }

    @Test
    void test_entity(){
        AST ast = ASTHelper.mockSimplePersonEntity();
        String expected = "let person = {\n" +
                "  name = \"Will\",\n" +
                "  get_name = ()->{\n" +
                "    this.name\n" +
                "  },\n" +
                "  get_my_name = ()->{\n" +
                "    name\n" +
                "  },\n" +
                "};\n" +
                "let name_1 = person.name;\n" +
                "let name_2 = person.get_name();";
        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_poly(){
        AST ast = ASTHelper.mockDefinedPoly();
        String expected = "var poly = 100&\"some\";";
        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_union(){
        AST ast = ASTHelper.mockDefinedLiteralUnion();
        String expected = "var union = 100|\"some\";";
        assertEquals(expected,ast.lexeme());
    }
}
