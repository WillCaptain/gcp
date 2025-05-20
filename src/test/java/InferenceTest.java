import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.*;
import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ERROR;
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
        ASF asf = ASTHelper.educationAndHuman();
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
        assertEquals(Outline.Integer, ((Variable) age.lhs()).declared().outline());
        assertEquals(Outline.Integer, age.lhs().outline());
        //name = "Will"
        Assignment name = var.assignments().get(1);
        assertInstanceOf(STRING.class, name.lhs().outline());
        //height:Decimal = 1.68
        Assignment height = var.assignments().get(2);
        assertInstanceOf(DOUBLE.class, height.lhs().outline());
        assertEquals(ast.errors().getFirst().node(), age.lhs());
    }

    @Test
    void test_declared_assignment_type_mismatch_inference() {
        /*
        module me
        var age: Integer = "some";
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast,new Token<>("me", 10)));
        ast.program().setNamespace(namespace);

        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.VAR);
        var.declare(new Identifier(ast,new Token<>("age")), new IdentifierTypeNode(new Identifier(ast,new Token<>("Integer"))), LiteralNode.parse(ast, new Token("some")));
        ast.program().body().addStatement(var);
        asf.infer();
        assertTrue(asf.inferred());
        assertInstanceOf(INTEGER.class, var.assignments().getFirst().lhs().outline());
        assertEquals(1, ast.errors().size());
        assertEquals(var.assignments().getFirst(), ast.errors().getFirst().node());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().getFirst().errorCode());
    }

    @Test
    void test_assign_mismatch_inference() {
        /*
        module me
        var age = "some";
        age = 100;
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        LiteralNode<String> str = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<Integer> num = LiteralNode.parse(ast, new Token<>(100));

        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast,new Token<>("me", 10)));
        ast.program().setNamespace(namespace);

        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.VAR);
        var.declare(new Identifier(ast,new Token<>("age")), str);
        ast.program().body().addStatement(var);
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("age")), num);
        ast.addStatement(assignment);
        asf.infer();
        assertTrue(asf.inferred());
        assertEquals(1, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().getFirst().errorCode());
        assertEquals(assignment, ast.errors().getFirst().node());
    }

    @Test
    void test_declared_poly_mismatch_assignment_inference() {
        /*
        module me
        var age = "some"&100;
        age = 100.0;
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        LiteralNode<String> str = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<Float> f = LiteralNode.parse(ast, new Token<>(100f));
        LiteralNode<Integer> i = LiteralNode.parse(ast, new Token<>(100));

        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast,new Token<>("me", 10)));
        ast.program().setNamespace(namespace);

        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.VAR);
        var.declare(new Identifier(ast,new Token<>("age")), new PolyNode(ast, str, i));
        ast.program().body().addStatement(var);
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("age")), f);
        ast.addStatement(assignment);
        asf.infer();
        assertEquals(1, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().getFirst().errorCode());
    }

    @Test
    void test_poly_cant_be_assigned_more_outline_inference() {
        /*
        var add = (x,y)->y->x+y & (x,y,z)->x+y+z;
        var forError = "any";
        add = forError;
         */
        AST ast = ASTHelper.mockErrorPoly();
        ast.asf().infer();
        Assignment assignment = cast(ast.program().body().nodes().get(0).nodes().getFirst());
        Outline add = assignment.lhs().outline();
        assertInstanceOf(Poly.class, add);
        Poly poly = cast(add);
        assertEquals(2, poly.options().size());
        assertEquals(1, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().getFirst().errorCode());//id doesn't math any poly options
        assertEquals(ast.program().body().nodes().get(2), ast.errors().getFirst().node());
    }

    @Test
    void test_explicit_poly_inference() {
        /*
        module default
        var poly: Integer&String = 100&"some";
        poly = 10.0;//poly doesn't have float
        let poly = 10.0;//duplicated definition
         */
        AST ast = ASTHelper.mockDefinedPoly();
        ast.asf().infer();
        Identifier lhs = cast(((VariableDeclarator) ast.program().body().nodes().get(0)).assignments().get(0).lhs());
        Poly poly = cast(lhs.outline());
        assertEquals(Outline.Integer.toString(), poly.options().get(0).toString());
        assertInstanceOf(STRING.class, poly.options().get(1));

        ast = ASTHelper.mockErrorAssignOnDefinedPoly();
        ast.asf().infer();
        assertTrue(ast.asf().inferred());
        Poly p1 = cast(((Assignment) ast.program().body().nodes().get(1)).lhs().outline());
        VariableDeclarator declarator = cast(ast.program().body().nodes().get(2));
        Outline p2 = declarator.assignments().get(0).lhs().outline();
        assertEquals(2, ast.errors().size());
        assertEquals(GCPErrCode.OUTLINE_MISMATCH, ast.errors().get(0).errorCode());
        assertEquals(GCPErrCode.DUPLICATED_DEFINITION, ast.errors().get(1).errorCode());

        assertEquals(Outline.Integer.toString(), p1.options().get(0).toString());
        assertInstanceOf(INTEGER.class, p1.options().get(0));
        assertInstanceOf(STRING.class, p1.options().get(1));

        assertInstanceOf(ERROR.class, p2);
    }

    @Test
    void test_literal_inference() {
        /*
        module default
        var union = 100|"some";//union can only be 100 or "some"
        union = 100;
        union = "some";
        union = 200;//literal union doesn't match
         */
        AST ast = ASTHelper.mockDefinedLiteralUnion();
        ast.asf().infer();
        Identifier lhs = cast(((VariableDeclarator) ast.program().body().nodes().get(0)).assignments().get(0).lhs());
        LiteralUnion union = cast(lhs.outline());
        assertEquals("100", union.values().get(0).lexeme());
        assertEquals("\"some\"", union.values().get(1).lexeme());

        ast = ASTHelper.mockAssignOnDefinedLiteralUnion();
        ast.asf().infer();
        assertTrue(ast.asf().inferred());
        assertEquals(1, ast.errors().size());
        assertEquals(ast.program().body().statements().get(3), ast.errors().getFirst().node());
    }

    @Test
    void test_function_definition_inference() {
        /*
        module default
        let add = (x,y)->x+y;
         */
        AST ast = ASTHelper.mockAddFunc();
        ast.infer();
        assertTrue(ast.asf().inferred());
        VariableDeclarator declare = cast(ast.program().body().statements().getFirst());
        Assignment assign = declare.assignments().getFirst();
        assertInstanceOf(FirstOrderFunction.class, assign.lhs().outline());
    }

    @Test
    void test_override_function_definition_inference() {
        /*
        module default
        var add = (x,y)->x+y & (x,y,z)->x+y+z;
         */
        AST ast = ASTHelper.mockOverrideAddFunc();
        ast.asf().infer();
        assertTrue(ast.asf().inferred());
        Identifier add = cast(ast.program().body().nodes().getFirst().nodes().getFirst().nodes().getFirst());
        assertInstanceOf(Poly.class, add.outline());
        assertEquals(2, ((Poly) add.outline()).options().size());
        assertTrue(((Poly) add.outline()).options().getFirst() instanceof Function<?, ?>);
    }

    @Test
    void test_binary_expression() {
        /*
        module test
        let a = 100.0, b = 100, c = "some";
        a+b;
        a+c;
        a-b;
        a-c;
        a==b;
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast,new Token<>("a")), LiteralNode.parse(ast, new Token<>(100d)));
        declare.declare(new Identifier(ast,new Token<>("b")), LiteralNode.parse(ast, new Token<>(100)));
        declare.declare(new Identifier(ast,new Token<>("c")), LiteralNode.parse(ast, new Token<>("some")));
        ast.addStatement(declare);
        //a+b should be double
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token<>("a")), new Identifier(ast, new Token<>("b"))
                , new OperatorNode<>(ast, BinaryOperator.ADD));
        ast.addStatement(new ExpressionStatement(add1));

        //a+c should be string
        BinaryExpression add2 = new BinaryExpression(new Identifier(ast, new Token<>("a")), new Identifier(ast, new Token<>("c"))
                , new OperatorNode<>(ast, BinaryOperator.ADD));
        ast.addStatement(new ExpressionStatement(add2));

        //a-b should be double
        BinaryExpression sub1 = new BinaryExpression(new Identifier(ast, new Token<>("a")), new Identifier(ast, new Token<>("b"))
                , new OperatorNode<>(ast, BinaryOperator.SUBTRACT));
        ast.addStatement(new ExpressionStatement(sub1));
        //a-c should be error
        BinaryExpression sub2 = new BinaryExpression(new Identifier(ast, new Token<>("a")), new Identifier(ast, new Token<>("c"))
                , new OperatorNode<>(ast, BinaryOperator.SUBTRACT));
        ast.addStatement(new ExpressionStatement(sub2));

        //a==b should be bool
        BinaryExpression compare = new BinaryExpression(new Identifier(ast, new Token<>("a")), new Identifier(ast, new Token<>("b"))
                , new OperatorNode<>(ast, BinaryOperator.EQUALS));
        ast.addStatement(new ExpressionStatement(compare));

        ast.asf().infer();
        assertTrue(ast.asf().inferred());
        assertInstanceOf(DOUBLE.class, add1.outline());
        assertInstanceOf(STRING.class, add2.outline());
        assertInstanceOf(DOUBLE.class, sub1.outline());
        assertEquals(Outline.Boolean, compare.outline());
        assertEquals(1, ast.errors().size());
        assertEquals(sub2, ast.errors().getFirst().node());
    }

    @Test
    void test_simple_person_entity() {
        /*
        let person: Entity = {
          name = "Will",
          get_name = ()->this.name;
          get_my_name = ()->name;
        };
        let name_1 = person.name;
        let name_2 = person.get_name();
         */
        AST ast = ASTHelper.mockSimplePersonEntity();
        ast.asf().infer();
        assertTrue(ast.asf().inferred());
        VariableDeclarator var = cast(ast.program().body().statements().getFirst());
        Entity person = cast(var.assignments().getFirst().lhs().outline());
        assertEquals(3, person.members().size());
        EntityMember name = person.members().get(0);
        EntityMember getName = person.members().get(1);
        EntityMember getName2 = person.members().get(2);
        assertInstanceOf(STRING.class, name.outline());
        assertInstanceOf(Function.class, getName.outline());
        assertInstanceOf(STRING.class, ((Function<?, ?>) getName.outline()).returns().supposedToBe());
        assertInstanceOf(STRING.class, ((Function<?, ?>) getName2.outline()).returns().supposedToBe());

        VariableDeclarator name1 = cast(ast.program().body().statements().get(1));
        VariableDeclarator name2 = cast(ast.program().body().statements().get(2));
        assertInstanceOf(STRING.class, name1.assignments().getFirst().lhs().outline());
        assertInstanceOf(STRING.class, name2.assignments().getFirst().lhs().outline());
    }

    @Test
    void test_simple_person_entity_with_override_member() {
        /*
        module test
        let person = {
            get_name = ()->this.name,
            name = "Will",
            mute get_name = ()->this.name&last_name->this.name+last_name
        };
        let name_1 = person.name;
        let name_2 = person.get_name();
         */
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
        //todo
    }

    @Test
    void test_option_is_as() {
        /*let result = {
            var some:String|Integer = "string";
            if(some is Integer){
                some
            }else if(some is String as str){
                str
            }else{100}
        };*/
        AST ast = ASTHelper.mockOptionIsAs();
        ast.asf().infer();
        Assignment assignment = ((VariableDeclarator) ast.program().body().nodes().getFirst()).assignments().getFirst();
        Outline result = assignment.lhs().outline();
        assertInstanceOf(Option.class, result);
        assertInstanceOf(INTEGER.class, ((Option) result).options().getFirst());
        assertInstanceOf(STRING.class, ((Option) result).options().getLast());
        Node rootSome = assignment.rhs().nodes().getFirst().nodes().getFirst().nodes().getFirst();
        assertInstanceOf(Option.class, rootSome.outline());
        Node some = assignment.rhs().nodes().get(1).nodes().getFirst().nodes().getFirst().nodes().getLast().nodes().getFirst().nodes().getFirst();
        assertInstanceOf(INTEGER.class, some.outline());
        Node str = assignment.rhs().nodes().get(1).nodes().getFirst().nodes().get(1).nodes().getLast().nodes().getFirst().nodes().getFirst();
        assertInstanceOf(STRING.class, str.outline());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void test_poly_is_as() {
        /*let result = {
            var some = 100&"string";
            if(some is Integer){
                some
            }else if(some is String as str){
                str
            }else{100}
        };*/
        AST ast = ASTHelper.mockPolyIsAs();
        ast.asf().infer();
        Assignment assignment = ((VariableDeclarator) ast.program().body().nodes().getFirst()).assignments().getFirst();
        Outline result = assignment.lhs().outline();
        assertInstanceOf(Option.class, result);
        assertInstanceOf(INTEGER.class, ((Option) result).options().getFirst());
        assertInstanceOf(STRING.class, ((Option) result).options().getLast());
        Node rootSome = assignment.rhs().nodes().getFirst().nodes().getFirst().nodes().getFirst();
        assertInstanceOf(Poly.class, rootSome.outline());
        Node some = assignment.rhs().nodes().get(1).nodes().getFirst().nodes().getFirst().nodes().getLast().nodes().getFirst().nodes().getFirst();
        assertInstanceOf(INTEGER.class, some.outline());
        Node str = assignment.rhs().nodes().get(1).nodes().getFirst().nodes().get(1).nodes().getLast().nodes().getFirst().nodes().getFirst();
        assertInstanceOf(STRING.class, str.outline());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void test_generic_is_as() {
        /*let result = some->{
            if(some is Integer){
                some
            }else if(some is String as str){
                str
            }else{100}
        }(100);*/
        AST ast = ASTHelper.mockGenericIsAs();
        ast.asf().infer();
        Assignment assignment = ((VariableDeclarator) ast.program().body().nodes().getFirst()).assignments().getFirst();
        Outline result = assignment.lhs().outline();
        assertInstanceOf(Option.class, result);
        assertInstanceOf(INTEGER.class, ((Option) result).options().getFirst());
        assertInstanceOf(STRING.class, ((Option) result).options().getLast());

        FunctionNode function = cast(((FunctionCallNode)assignment.rhs()).function());
        assertEquals(1,ast.errors().size());
        Generic arg = cast(function.argument().outline());
        assertInstanceOf(INTEGER.class, arg.couldBe());
    }

    @Test
    void test_inference_of_reference_in_function() {
        /*
        let f = func<a,b>(x:a)->{
           let y:b = 100;
           y
        }*/
        AST ast = ASTHelper.mockReferenceInFunction();
        ast.infer();
        Assignment assignment = ((VariableDeclarator)ast.program().body().statements().getFirst()).assignments().getFirst();
        FirstOrderFunction f = cast(assignment.lhs().outline());
        assertInstanceOf(Reference.class,f.argument().declaredToBe());
        assertEquals("a", f.argument().declaredToBe().name());
        assertInstanceOf(Reference.class, f.returns().supposedToBe());
        assertEquals("b", f.returns().supposedToBe().name());
        assertInstanceOf(INTEGER.class, ((Reference)f.returns().supposedToBe()).extendToBe());
    }

    private static AST mockGCPTestAst() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast,new Token<>("test")));
        ast.setNamespace(namespace);
        return ast;
    }


}
