import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.*;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.DOUBLE;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.STRING;
import org.twelve.gcp.outline.projectable.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.Ignore;

public class InferenceTest {


    @Test
    void test_initialization_inference() {
        /*
        module org.twelve.education
        let grade: Integer = 1, school: String = "NO.1";
        export grade, school as college;

        module org.twelve.human
        import grade as level, college as school from education;
        let age: Integer, name: String = "Will", height: Double = 1.68, grade: Integer = level;
        export height as stature, name;
         */
        ASF asf = ASTHelper.asf();
        asf.infer();
        assertTrue(asf.inferred());
        AST ast = cast(asf.get("education"));
        Export exports = ast.program().body().exports().getFirst();
        assertInstanceOf(INTEGER.class, exports.specifiers().getFirst().outline());
        assertInstanceOf(STRING.class, exports.specifiers().getLast().outline());
        assertTrue(ast.errors().isEmpty());
        ast = cast(asf.get("human"));
        Import imports = ast.program().body().imports().getFirst();
        assertInstanceOf(INTEGER.class, imports.specifiers().getFirst().outline());
        assertInstanceOf(STRING.class, imports.specifiers().get(1).outline());
        VariableDeclarator var = cast(ast.program().body().statements().get(0));
        assertEquals(var.assignments().getFirst().lhs(), ast.errors().getFirst().node());
        assertEquals(Ignore, var.outline());
        assertEquals(Ignore, var.assignments().getFirst().outline());
        //age:Integer
        Assignment age = var.assignments().getFirst();
        assertEquals(Outline.Integer, ((Identifier) age.lhs()).declared());
        assertEquals(Outline.Integer, age.lhs().outline());
        //name = "Will"
        Assignment name = var.assignments().get(1);
        assertInstanceOf(STRING.class, name.lhs().outline());
        //height:Decimal = 1.68
        Assignment height = var.assignments().get(2);
        assertInstanceOf(DOUBLE.class, height.lhs().outline());
        assertEquals(ast.errors().getFirst().node(),age.lhs());
    }

    @Test
    void test_declared_assignment_type_mismatch_inference() {
        /*
        var age: Integer = "some";
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("module", 10));
        ast.program().setNamespace(namespace);

        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.VAR);
        var.declare(new Token<>("age"), Outline.Integer, LiteralNode.parse(ast, new Token("some")));
        ast.program().body().addStatement(var);
        asf.infer();
        assertTrue(asf.inferred());
        assertInstanceOf(INTEGER.class, var.assignments().getFirst().lhs().outline());
        assertEquals(1, ast.errors().size());
        assertEquals(var.assignments().getFirst().rhs(), ast.errors().getFirst().node());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH,ast.errors().getFirst().errorCode());
    }

    @Test
    void test_assign_mismatch_inference() {
        /*
        var age: String = "some";
        age = 100;
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        LiteralNode<String> str = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<Integer> num = LiteralNode.parse(ast, new Token<>(100));

        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("module", 10));
        ast.program().setNamespace(namespace);

        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.VAR);
        var.declare(new Token<>("age"), str);
        ast.program().body().addStatement(var);
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("age")), num);
        ast.addStatement(assignment);
        asf.infer();
        assertTrue(asf.inferred());
        assertEquals(1, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().get(0).errorCode());
        assertEquals(assignment, ast.errors().get(0).node().parent());
    }

    @Test
    void test_declared_poly_mismatch_assignment_inference() {
        ASF asf = new ASF();
        AST ast = asf.newAST();

        LiteralNode str = LiteralNode.parse(ast, new Token("some"));
        LiteralNode num = LiteralNode.parse(ast, new Token(100));

        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("module", 10));
        ast.program().setNamespace(namespace);

        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.VAR);
        var.declare(new Token<>("age"), Poly.from(var, true, Outline.String), str);
        ast.program().body().addStatement(var);
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("age")), num);
        ast.addStatement(assignment);
        asf.infer();
        assertEquals(1, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().getFirst().errorCode());
    }


//    @Test
//    void test_import_inference() {
//        ASF asf = ASTHelper.asf();
//        asf.infer();
//        AST human = asf.get("human");
//        VariableDeclarator declare = cast(human.program().body().statements().get(0));
//        Assignment school = declare.assignments().get(3);
//        assertEquals("grade: Integer = level", school.toString());
//    }

    @Test
    void test_poly_cant_be_assigned_more_outline_inference() {
        AST ast = ASTHelper.mockErrorPoly();
        ast.asf().infer();
        ;
        Outline add = ((Assignment) ast.program().body().nodes().get(0).nodes().getFirst()).lhs().outline();
        assertTrue(add instanceof Poly);
        Poly poly = cast(add);
        assertEquals(2, poly.options().size());
        assertEquals(1, ast.errors().size());
//        assertEquals(GCPErrCode.NOT_ASSIGNABLE,ast.errors().get(0).errorCode());//let can't be assigned
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().getFirst().errorCode());//id doesn't math any poly options
    }

    @Test
    void test_explicit_poly_inference() {
        AST ast = ASTHelper.mockDefinedPoly();
        ast.asf().infer();
        Identifier lhs = cast(((VariableDeclarator) ast.program().body().nodes().get(0)).assignments().get(0).lhs());
        Poly poly = cast(lhs.outline());
        assertEquals(Outline.Integer.toString(), poly.options().get(0).toString());
        assertTrue(poly.options().get(1) instanceof STRING);

        ast = ASTHelper.mockErrorAssignOnDefinedPoly();
        ast.asf().infer();
        Poly p1 = cast(((Assignment) ast.program().body().nodes().get(1)).lhs().outline());
        VariableDeclarator declarator = cast(ast.program().body().nodes().get(2));
        Outline p2 = declarator.assignments().get(0).lhs().outline();
        assertEquals(2, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().get(0).errorCode());
        assertEquals(GCPErrCode.DUPLICATED_DEFINITION, ast.errors().get(1).errorCode());

        assertEquals(Outline.Integer.toString(), p1.options().get(0).toString());
        assertInstanceOf(INTEGER.class, p1.options().get(0));
        assertInstanceOf(STRING.class, p1.options().get(1));

        assertInstanceOf(UNKNOWN.class, p2);

//        assertEquals(Outline.Integer.toString(),p2.options().get(0).toString());
//        assertEquals(Outline.String.toString(),p2.options().get(1).toString());
//        assertEquals(Outline.Float,p2.options().get(2));
    }

    @Test
    void test_literal_inference() {
        AST ast = ASTHelper.mockDefinedLiteralUnion();
        ast.asf().infer();
        Identifier lhs = cast(((VariableDeclarator) ast.program().body().nodes().get(0)).assignments().get(0).lhs());
        LiteralUnion union = cast(lhs.outline());
        assertEquals("100", union.values().get(0).lexeme());
        assertEquals("\"some\"", union.values().get(1).lexeme());

        ast = ASTHelper.mockAssignOnDefinedLiteralUnion();
        ast.asf().infer();
        assertEquals(1, ast.errors().size());
        LiteralNode l200 = cast(ast.errors().get(0).node());
        assertEquals(200, l200.value());
    }

    @Test
    void test_function_inference() {
        AST ast = ASTHelper.mockAddFunc();
        ast.infer();
        VariableDeclarator declare = cast(ast.program().body().statements().get(0));
        Assignment assign = declare.assignments().get(0);
        assign.lhs().toString();
        assertTrue(assign.lhs().outline() instanceof FirstOrderFunction);
    }

    @Test
    void test_override_function_inference() {
        AST ast = ASTHelper.mockOverrideAddFunc();
        ast.asf().infer();
        Identifier getName = cast(ast.program().body().nodes().get(0).nodes().get(0).nodes().get(0));
//        Identifier getName2 = cast(ast.program().body().nodes().get(1).nodes().get(0).nodes().get(0));

//        assertTrue(getName1.outline() instanceof Function);
        assertTrue(getName.outline() instanceof Poly);
        assertEquals(2, ((Poly) getName.outline()).options().size());
        assertTrue(((Poly) getName.outline()).options().getFirst() instanceof Function<?, ?>);
    }


    @Test
    void test_function_call_inference() {
        //todo
    }

    @Test
    void test_function_override_call_inference() {
        //todo
    }

    @Test
    void test_binary_expression() {
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token("a"), LiteralNode.parse(ast, new Token(100d)));
        declare.declare(new Token("b"), LiteralNode.parse(ast, new Token(100)));
        declare.declare(new Token("c"), LiteralNode.parse(ast, new Token("some")));
        ast.addStatement(declare);
        //a+b should be double
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token("a")), new Identifier(ast, new Token("b"))
                , new OperatorNode(ast, BinaryOperator.ADD));
        ast.addStatement(new ExpressionStatement(add1));

        //a+c should be string
        BinaryExpression add2 = new BinaryExpression(new Identifier(ast, new Token("a")), new Identifier(ast, new Token("c"))
                , new OperatorNode(ast, BinaryOperator.ADD));
        ast.addStatement(new ExpressionStatement(add2));

        //a-b should be double
        BinaryExpression sub1 = new BinaryExpression(new Identifier(ast, new Token("a")), new Identifier(ast, new Token("b"))
                , new OperatorNode(ast, BinaryOperator.SUBTRACT));
        ast.addStatement(new ExpressionStatement(sub1));
        //a-c should be error
        BinaryExpression sub2 = new BinaryExpression(new Identifier(ast, new Token("a")), new Identifier(ast, new Token("c"))
                , new OperatorNode(ast, BinaryOperator.SUBTRACT));
        ast.addStatement(new ExpressionStatement(sub2));

        //a==b should be bool
        BinaryExpression compare = new BinaryExpression(new Identifier(ast, new Token("a")), new Identifier(ast, new Token("b"))
                , new OperatorNode(ast, BinaryOperator.EQUALS));
        ast.addStatement(new ExpressionStatement(compare));

        ast.asf().infer();
        assertTrue(add1.outline() instanceof DOUBLE);
        assertTrue(add2.outline() instanceof STRING);
        assertTrue(sub1.outline() instanceof DOUBLE);
        assertEquals(Outline.Boolean, compare.outline());
        assertEquals(1, ast.errors().size());
    }

    @Test
    void test_simple_person_entity() {
        //let person: Entity = {
        //  name = "Will",
        //  get_name = ()->{
        //    this.name
        //  }
        //  get_my_name = ()->{
        //    name
        //  }
        //};
        AST ast = ASTHelper.mockSimplePersonEntity();
        ast.asf().infer();
        VariableDeclarator var = cast(ast.program().body().statements().get(0));
        Entity person = cast(var.assignments().get(0).lhs().outline());
        assertEquals(3, person.members().size());
        EntityMember name = person.members().get(0);
        EntityMember getName = person.members().get(1);
        EntityMember getName2 = person.members().get(2);
        assertTrue(name.outline() instanceof STRING);
        assertTrue(getName.outline() instanceof Function);
        assertTrue(((Function) getName.outline()).returns().supposedToBe() instanceof STRING);
        assertTrue(((Function) getName2.outline()).returns().supposedToBe() instanceof STRING);
    }

    @Test
    void test_simple_person_entity_with_override_member() {
        AST ast = ASTHelper.mockSimplePersonEntityWithOverrideMember();
        ast.asf().infer();
        VariableDeclarator var = cast(ast.program().body().statements().getFirst());
        Entity person = cast(var.assignments().getFirst().lhs().outline());
        assertEquals(2, person.members().size());
        EntityMember getName = person.members().get(1);
        assertInstanceOf(Poly.class, getName.outline());
        Poly outline = cast(getName.outline());
        Function<?, ?> f1 = cast(outline.options().get(0));
        assertInstanceOf(STRING.class, f1.returns().supposedToBe());
        FirstOrderFunction f2 = cast(outline.options().get(1));
        assertInstanceOf(Option.class, f2.argument().definedToBe());
        assertInstanceOf(STRING.class, f2.returns().supposedToBe());
    }


    @Test
    void test_inherited_person_entity() {
        AST ast = ASTHelper.mockInheritedPersonEntity();
        ast.asf().infer();
        Entity person = cast(ast.program().body().statements().get(3).nodes().get(0).nodes().get(0).outline());
        assertEquals(4, person.members().size());
        EntityMember getFullName = person.members().get(0);
        assertTrue(getFullName.outline() instanceof Function);
        assertTrue(((Function) getFullName.outline()).returns().supposedToBe() instanceof STRING);
    }

    @Test
    void test_inherited_person_entity_with_override_member() {
        AST ast = ASTHelper.mockInheritedPersonEntityWithOverrideMember();
        ast.asf().infer();
        Entity person = cast(ast.program().body().statements().get(3).nodes().getFirst().nodes().getFirst().outline());
        List<EntityMember> members = person.members();
        assertEquals(4, members.size());
        assertInstanceOf(STRING.class, members.get(1).outline());
        assertInstanceOf(Poly.class, members.get(2).outline());
        Poly getName = cast(members.get(2).outline());
        assertSame(Outline.Unit, ((Generic) ((Function<?, ?>) getName.options().get(0)).argument()).declaredToBe());
        Function<?, ?> overrides = cast(getName.options().get(1));
        assertInstanceOf(Option.class, ((Generic) overrides.argument()).definedToBe());
        assertInstanceOf(INTEGER.class, overrides.returns().supposedToBe());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void test_inherited_person_entity_with_override_call() {

    }

    private static AST mockGCPTestAst() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token("test"));
        ast.setNamespace(namespace);
        return ast;
    }

}
