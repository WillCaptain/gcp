import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.SELECTION_TYPE;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.node.expression.Identifier;
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
        assertEquals(19, this.ast.program().namespace().loc().end());

        assertEquals("org", ast.program().namespace().nodes().get(0).lexeme());
        assertEquals("twelve", ast.program().namespace().nodes().get(1).lexeme());
        assertEquals("human", ast.program().moduleName());
        assertEquals("org.twelve", ast.program().namespace().lexeme());

        //like outline of "package org.twelve.test" is unknown before inference
        assertInstanceOf(UNKNOWN.class, ast.program().namespace().outline());
        Namespace org = cast(ast.program().namespace().nodes().getFirst().outline());
        // but outline of "org" is namespace
        assertInstanceOf(Namespace.class, org);
        assertTrue(org.isTop());
        assertNull(org.parentNamespace());
        assertEquals("org", org.namespace());
        //namespace is not assignable, means or can not express alone without tail
        assertFalse(org.beAssignedAble());

        Namespace twelve = cast(ast.program().namespace().nodes().get(1).outline());
        assertEquals(twelve, org.subNamespaces().getFirst());
        assertEquals(org, twelve.parentNamespace());
        assertEquals("twelve", twelve.namespace());

        assertEquals("org.twelve", ast.namespace().lexeme());

        assertEquals(0, ast.program().id());
        assertEquals(2, ast.program().body().id());
        assertEquals(6, ast.program().namespace().id());
        assertEquals(8, ast.program().namespace().nodes().get(0).id());
        assertEquals(10, ast.program().namespace().nodes().get(1).id());
    }


    @Test
    void test_import() {
        Import imported = this.ast.program().body().imports().getFirst();
        //check to string
        assertEquals("import grade as level, college as school from education;", ast.program().body().imports().getFirst().lexeme());
        //check location
        assertEquals(29, imported.loc().start());
        assertEquals(52, imported.loc().end());
        //check source
        assertEquals("education", imported.source().lexeme());
        assertEquals(44, imported.source().loc().start());
        assertEquals(52, imported.source().loc().end());
        assertInstanceOf(Module.class, imported.source().outline());
        //check a
        ImportSpecifier a = imported.specifiers().getFirst();
        assertEquals("grade as level", a.lexeme());
        assertEquals("grade", a.imported().lexeme());
        assertEquals(29, a.imported().loc().start());
        assertEquals(33, a.imported().loc().end());
        assertEquals("level", a.local().lexeme());
        assertEquals(34, a.local().loc().start());
        assertEquals(38, a.local().loc().end());

        assertInstanceOf(UNKNOWN.class, a.get(0).outline());//outline is not confirmed yet
        assertSame(a.get(0).outline(), a.get(1).outline());//outline of b is a reference of a outline
        //check c
        ImportSpecifier c = imported.specifiers().get(1);
        assertEquals("school", c.get(1).toString());
        assertEquals("college as school", c.lexeme());
        assertEquals("college", c.imported().lexeme());
        assertEquals(37, c.imported().loc().start());
        assertEquals(43, c.imported().loc().end());

        //import * from e
        List<Identifier> source = new ArrayList<>();
        source.add(new Identifier(ast,new Token<>("e", 22)));
        source.add(new Identifier(ast,new Token<>("f", 22)));
        source.add(new Identifier(ast,new Token<>("g", 22)));
        imported = ast.program().body().addImport(new Import(source));
        assertEquals(0, imported.specifiers().size());
        assertEquals("e.f.g", imported.source().lexeme());

    }

    @Test
    void test_export() {
        Export exported = ast.program().body().exports().getFirst();
        //check to string
        assertEquals("export height as stature, name;", ast.program().body().exports().getFirst().toString());
        //check location
        assertEquals(100, exported.loc().start());
        assertEquals(119, exported.loc().end());
        //check a
        ExportSpecifier a = exported.specifiers().getFirst();
        assertEquals("height as stature", a.lexeme());
        assertEquals("height", a.local().lexeme());
        assertEquals(100, a.local().loc().start());
        assertEquals(105, a.local().loc().end());
        assertEquals("stature", a.exported().lexeme());
        assertEquals(111, a.exported().loc().start());
        assertEquals(117, a.exported().loc().end());

        assertInstanceOf(UNKNOWN.class, a.get(0).outline());//outline is not confirmed yet
        assertSame(a.get(0).outline(), a.get(1).outline());//outline of b is a reference of a outline
        //check c
        ExportSpecifier c = exported.specifiers().get(1);
        assertSame(c.get(0), c.get(1));
        assertEquals("name", c.lexeme());
        assertEquals("name", c.local().lexeme());
        assertEquals(116, c.local().loc().start());
        assertEquals(119, c.local().loc().end());
    }

    @Test
    void test_variable_declare() {
        List<Statement> stmts = this.ast.program().body().statements();
        VariableDeclarator var = cast(stmts.getFirst());
        assertEquals(50, var.loc().start());
        assertEquals(114, var.loc().end());
        assertEquals("let age: Integer, name = \"Will\", height: Double = 1.68, grade = level;",
                var.toString());
    }

    @Test
    void test_function_definition(){
        AST ast = ASTHelper.mockAddFunc();
        String expected = """
                module default
                
                let add = x->{
                  y->{
                    x+y
                  }
                };""";
        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_non_argument_function(){
        AST ast = new ASF().newAST();
        FunctionBody body = new FunctionBody(ast);
        FunctionNode function = FunctionNode.from(body);
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast,new Token<>("get", 0)),function);
        ast.addStatement(declare);
        assertEquals(Token.unit().lexeme(),function.argument().name());
    }

    @Test
    void test_entity(){
        AST ast = ASTHelper.mockSimplePersonEntity();
        String expected = """
                module test
                
                let person = {
                  get_name = ()->{
                    this.name
                  },
                  get_my_name = ()->{
                    name
                  },
                  name = "Will",
                };
                let name_1 = person.name;
                let name_2 = person.get_name();""";
        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_poly(){
        AST ast = ASTHelper.mockDefinedPoly();
        String expected = "module default\n\nvar poly = 100&\"some\";";
        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_union(){
        AST ast = ASTHelper.mockDefinedLiteralUnion();
        String expected = "module default\n\nvar union = 100|\"some\";";
        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_if(){
        AST ast = ASTHelper.mockIf(SELECTION_TYPE.IF);
        String expected = """
                module default
                
                if(name=="Will"){
                  name
                } else {
                  "Someone"
                };""";
        assertEquals(expected,ast.lexeme());
        ast = ASTHelper.mockIf(SELECTION_TYPE.TERNARY);
        assertEquals("module default\n\nname==\"Will\"? name: \"Someone\";",ast.lexeme());
    }
    @Test
    void test_declare(){
        /*
         let f = (x:String->Integer->{name:String,age:Integer},y:String,z:Integer)->x(y,z);
         */
        AST ast = ASTHelper.mockDeclare();
        String expected = """
                module default
                
                let f = x: String->Integer->{name: String, var age: Integer}->{
                  y: String->{
                    z: Integer->{
                      x(y,z)
                    }
                  }
                };""";

        assertEquals(expected,ast.lexeme());
    }

    @Test
    void test_reference_in_function(){
        /*
        let f = func<a,b>(x:a)->{
           let y:b = 100;
           y
        }*/
        AST ast = ASTHelper.mockReferenceInFunction();
        String expected = """
                module default
                
                let f = func<a,b>(x: a)->{
                  let y: b = 100;
                  y
                };""";
        assertEquals(expected,ast.lexeme());
    }
}
