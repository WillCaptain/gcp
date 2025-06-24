import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.*;
import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.LONG;
import org.twelve.gcp.outline.primitive.NUMBER;
import org.twelve.gcp.outline.primitive.STRING;
import org.twelve.gcp.outline.projectable.*;

import javax.print.DocFlavor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.twelve.gcp.common.Tool.cast;

public class GCPInference {
    @Test
    void test_gcp_declare_to_be() {
        /*
        let f = x:Integer->x;
        f("some");
        f(100);
         */
        AST ast = mockGCPTestAst();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")), new IdentifierTypeNode(new Identifier(ast,new Token<>("Integer"))));
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode f = FunctionNode.from(body, x);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);
        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();
        //f.outline is a function
        assertInstanceOf(FirstOrderFunction.class, f.outline());
        //f.argument is a Generic outline
        assertInstanceOf(Genericable.class, f.argument().outline());
        //f.argument.declared_to_be = Integer
        assertEquals(Outline.Integer, f.argument().outline().declaredToBe());
        //f.return is a Return outline
        assertInstanceOf(Return.class, f.body().outline());
        //f.return = f.argument  (return x;)
        assertEquals(f.argument().outline(), ((Return) f.body().outline()).supposedToBe());
        //call1: gcp error
        assertEquals(1, ast.errors().size());
        assertEquals(some, ast.errors().getFirst().node());
        assertEquals(GCPErrCode.PROJECT_FAIL, ast.errors().getFirst().errorCode());
        //call2: Integer
        assertEquals(Outline.Integer.toString(), call2.outline().toString());

    }

    @Test
    void test_gcp_extend_to_be() {
        /*
         let f = x->{
           x = 10;
           x
         };
         f("some");
         f(100);
         */
        AST ast = mockGCPTestAst();
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        FunctionBody body = new FunctionBody(ast);

        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("x")), LiteralNode.parse(ast, new Token<>(10)));
        body.addStatement(assignment);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode f = FunctionNode.from(body, x);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);

        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();

        Return returns = cast(((FirstOrderFunction) f.outline()).returns());
        assertEquals(Outline.Integer.toString(), ((Genericable<?,?>) returns.supposedToBe()).extendToBe().toString());

        //f("some") project fail
        assertInstanceOf(INTEGER.class, call1.outline());
        assertEquals(GCPErrCode.PROJECT_FAIL, ast.errors().getFirst().errorCode());

        //f(10)
        assertEquals(Outline.Integer.toString(), call2.outline().toString());

    }

    @Test
    void test_gcp_has_to_be() {
        /*
        let f = x->{
            var y = "str";
            y = x;
            x
        };
        f("some");
        f(100);
         */
        AST ast = mockGCPTestAst();
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        FunctionBody body = new FunctionBody(ast);

        VariableDeclarator yDeclare = new VariableDeclarator(ast, VariableKind.VAR);
        yDeclare.declare(new Identifier(ast,new Token<>("y")), LiteralNode.parse(ast, new Token<>("str")));
        body.addStatement(yDeclare);


        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        body.addStatement(assignment);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode f = FunctionNode.from(body, x);

        VariableDeclarator fDeclare = new VariableDeclarator(ast, VariableKind.LET);
        fDeclare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(fDeclare);

        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), some);
        ast.addStatement(new ExpressionStatement(call2));
        //f(10)
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call1));
       assertTrue(ast.asf().infer());

        Return returns = cast(((FirstOrderFunction) f.outline()).returns());
        assertInstanceOf(STRING.class, ((Generic) returns.supposedToBe()).hasToBe());

        //f(100) project fail
        assertInstanceOf(STRING.class, call1.outline());
        assertEquals(GCPErrCode.PROJECT_FAIL, ast.errors().getFirst().errorCode());

        //f("some")
        assertInstanceOf(STRING.class, call2.outline());
    }

    @Test
    void test_gcp_defined_to_be() {
        /*
         let f = x->{
         x+1
         };
         f("some");
         f(100);
         */
        AST ast = mockGCPTestAst();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        FunctionBody body = new FunctionBody(ast);
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                LiteralNode.parse(ast, new Token<>(1)), new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add1));
        FunctionNode f = FunctionNode.from(body, x);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);
        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();
        //f.outline is a function
        assertInstanceOf(FirstOrderFunction.class, f.outline());
        //f.argument is a Generic outline
        assertInstanceOf(Generic.class, f.argument().outline());
        //f.return is a Return outline
        assertInstanceOf(Addable.class, ((Return) f.body().outline()).supposedToBe());
        //f.return.suppose_to_be = f.argument  (return x;)
        assertTrue(f.argument().outline().definedToBe().is(Option.StringOrNumber));
        //call1: gcp error
        assertEquals(0, ast.errors().size());
        //call2: Integer
        assertInstanceOf(STRING.class, call1.outline());
        assertEquals(Outline.Integer.toString(), call2.outline().toString());

    }

    @Test
    void test_gcp_add_expression() {
        //let f = (x,y)->x+y
        AST ast = mockGCPTestAst();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        FunctionBody body = new FunctionBody(ast);
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                new Identifier(ast, new Token<>("y")), new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add1));
        FunctionNode f = FunctionNode.from(body, x, y);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);

        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<String> people = LiteralNode.parse(ast, new Token<>("people"));
        LiteralNode<Integer> intNum = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Float> floatNum = LiteralNode.parse(ast, new Token<>(10f));
        //f("some",10)
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), some, intNum);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10,10f)
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), intNum, floatNum);
        ast.addStatement(new ExpressionStatement(call2));

        //let z = f("some");
        declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast,new Token<>("z")), new FunctionCallNode(new Identifier(ast, new Token<>("f")), some));
        ast.addStatement(declare);
        //z("people");
        FunctionCallNode call3 = new FunctionCallNode(new Identifier(ast, new Token<>("z")), people);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        //f.outline is a function
        assertInstanceOf(FirstOrderFunction.class, f.outline());
        //f.argument is a Generic outline
        assertInstanceOf(Generic.class, f.argument().outline());
        //f.return is a Return outline
        assertInstanceOf(Return.class, f.body().outline());
        //f.return.suppose_to_be = f.argument  (return x;)
        assertTrue(f.argument().outline().definedToBe().is(Option.StringOrNumber));
        //call1: gcp error
        assertEquals(0, ast.errors().size());
        //call2: Integer
        assertInstanceOf(STRING.class, call1.outline());
        assertEquals(Outline.Float.toString(), call2.outline().toString());
        assertInstanceOf(STRING.class, call3.outline());
    }

    @Test
    void test_generic_refer_each_other() {
        /*
        let f = (x,y,z)->{
            y = x;
            z = y;
            x+y+z
        };
        f("some","people",10.0);
        f(10,10,10);
         */
        AST ast = mockGCPTestAst();
        //f = (x,y,z)->{y = x; z=y; x+y;}
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        Argument z = new Argument(new Identifier(ast, new Token<>("z")));
        FunctionBody body = new FunctionBody(ast);
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        body.addStatement(assignment);
        assignment = new Assignment(new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("y")));
        body.addStatement(assignment);

        BinaryExpression add = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                new Identifier(ast, new Token<>("y")), new OperatorNode<>(ast, BinaryOperator.ADD));
        add = new BinaryExpression(add, new Identifier(ast, new Token<>("z")), new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add));
        FunctionNode f = FunctionNode.from(body, x, y, z);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);

        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<String> people = LiteralNode.parse(ast, new Token<>("people"));
        LiteralNode<Integer> intNum = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Float> floatNum = LiteralNode.parse(ast, new Token<>(10f));
        //f("some","people",10)
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), some, people, floatNum);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10,10,10)
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), intNum, intNum, intNum);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertEquals(1, ast.errors().size());
        assertEquals(floatNum, ast.errors().getFirst().node());
        assertEquals(Outline.Integer.toString(), call2.outline().toString());
        assertInstanceOf(STRING.class, call1.outline());
    }

    @Test
    void test_gcp_hof_projection_1() {
        /*
        let f = (x,y)->y(x);
        f(10,x->x*5);
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        body.addStatement(new ReturnStatement(call1));
        FunctionNode f = FunctionNode.from(body, x, y);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);
        //f(10,x->x*5);
        LiteralNode<Integer> ten = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Integer> five = LiteralNode.parse(ast, new Token<>(5));
        x = new Argument(new Identifier(ast, new Token<>("x")));
        body = new FunctionBody(ast);
        BinaryExpression tenTimes = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                five, new OperatorNode<>(ast, BinaryOperator.MULTIPLY));
        body.addStatement(new ReturnStatement(tenTimes));
        FunctionNode lambda = FunctionNode.from(body, x);
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), ten, lambda);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertTrue(ast.errors().isEmpty());
        assertEquals(Outline.Integer.toString(), call2.outline().toString());
        assertTrue(ast.inferred());
    }

    @Test
    void test_gcp_hof_projection_2() {
        /*
        let f = (y,x)->y(x);
        f(x->x+5,"10");
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        body.addStatement(new ReturnStatement(call1));
        FunctionNode f = FunctionNode.from(body, y, x);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);
        //f(x->x+5,10);
        LiteralNode<String> ten = LiteralNode.parse(ast, new Token<>("10"));
        LiteralNode<Integer> five = LiteralNode.parse(ast, new Token<>(5));
        x = new Argument(new Identifier(ast, new Token<>("x")));
        body = new FunctionBody(ast);
        BinaryExpression tenTimes = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                five, new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(tenTimes));
        FunctionNode lambda = FunctionNode.from(body, x);
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), lambda, ten);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertTrue(ast.inferred());
        assertTrue(ast.errors().isEmpty());
        assertInstanceOf(STRING.class, call2.outline());
    }

    @Test
    void test_gcp_hof_projection_3() {
        /*
        let f = (x,y,z)->z(y(x));
        f(10,x->x+"some",y->y+100);
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        Argument z = new Argument(new Identifier(ast, new Token<>("z")));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("z")), call1);
        body.addStatement(new ReturnStatement(call2));
        FunctionNode f = FunctionNode.from(body, x, y, z);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);
        //f(10,x->x+"some",y->y+100)
        LiteralNode<Integer> ten = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Integer> hundred = LiteralNode.parse(ast, new Token<>(100));
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        y = new Argument(new Identifier(ast, new Token<>("y")));
        x = new Argument(new Identifier(ast, new Token<>("x")));
        //x+"some"
        BinaryExpression addSome = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                some, new OperatorNode<>(ast, BinaryOperator.ADD));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(addSome));
        FunctionNode arg1 = FunctionNode.from(body, x);
        //y+100
        BinaryExpression add100 = new BinaryExpression(new Identifier(ast, new Token<>("y")),
                hundred, new OperatorNode<>(ast, BinaryOperator.ADD));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(add100));
        FunctionNode arg2 = FunctionNode.from(body, y);
        FunctionCallNode call3 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), ten, arg1, arg2);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        assertTrue(ast.inferred());
        assertTrue(ast.errors().isEmpty());
        assertInstanceOf(STRING.class, call3.outline());
    }

    @Test
    void test_gcp_hof_projection_4() {
        /*
        let f = (z,y,x)->z(y(x));
        f(y->y+100,x->x,10);
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        Argument z = new Argument(new Identifier(ast, new Token<>("z")));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        FunctionCallNode call2 = new FunctionCallNode(new Identifier(ast, new Token<>("z")), call1);
        body.addStatement(new ReturnStatement(call2));
        FunctionNode f = FunctionNode.from(body, z, y, x);
        declare.declare(new Identifier(ast,new Token<>("f")), f);
        ast.addStatement(declare);
        //f(y->y+100,x->x,"some")
        LiteralNode<Integer> ten = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Integer> hundred = LiteralNode.parse(ast, new Token<>(100));
        y = new Argument(new Identifier(ast, new Token<>("y")));
        x = new Argument(new Identifier(ast, new Token<>("x")));
        //x->x
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode arg1 = FunctionNode.from(body, x);
        //y+100
        BinaryExpression add100 = new BinaryExpression(new Identifier(ast, new Token<>("y")),
                hundred, new OperatorNode<>(ast, BinaryOperator.ADD));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(add100));
        FunctionNode arg2 = FunctionNode.from(body, y);
        FunctionCallNode call3 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), arg2, arg1, ten);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        assertTrue(ast.inferred());
        assertTrue(ast.errors().isEmpty());
        assertInstanceOf(INTEGER.class, call3.outline());
    }

    @Test
    void test_gcp_declared_hof_projection(){
        /*
         * let f = fx<a>(x:a->{name:?,age:Integer})->{
         *   x("Will").name
         * }
         * f<Integer>;
         * f(n->{name=n,age=30})
         */
        AST ast = ASTHelper.mockDeclaredHofProjection();
        assertTrue(ast.asf().infer());
//        Function f = cast(ast.program().body().statements().get(1).get(0).outline());
        assertEquals(1,ast.errors().size());
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().getFirst().errorCode());
        Outline name = ast.program().body().statements().get(2).outline();
        assertInstanceOf(STRING.class,name);
    }

    @Test
    void test_gcp_extend_hof_projection(){
        /*
         * let f = x->{
         *   x = a->{name=a};
         *   x("Will").name
         * }
         * f(n->{name=n})
         */
        AST ast = ASTHelper.mockExtendHofProjection();
        assertTrue(ast.asf().infer());
        Outline name = ast.program().body().statements().get(1).outline();
        assertInstanceOf(STRING.class,name);;
    }

    @Test
    void test_entity_hof_projection_1() {
        /*
        let f = (x,z,y)->z.combine(x,y);
        f(20,{combine = (x,y)->{{age = x,name = y.name}},{name = "Will"})
         */
        for (int i = 1; i < 4; i++) {
            Node call = ASTHelper.mockEntityProjection1(i, ASTHelper::mockEntityProjectionNode1);
            call.ast().asf().infer();
            Entity result = cast(call.outline());
            assertEquals("name", result.members().get(0).name());
            assertInstanceOf(STRING.class, result.members().get(0).outline());
            assertEquals("age", result.members().get(1).name());
            assertInstanceOf(INTEGER.class, result.members().get(1).outline());
            assertTrue(call.ast().errors().isEmpty());
            assertTrue(call.ast().inferred());
        }

    }

    @Test
    void test_entity_hof_projection_2() {
        /*
        let f = (x,z,y)->z.combine(x,y).name;
        f(20,{combine = (x,y)->{{age = x,name = y.name}},{name = "Will",})
         */
        for (int i = 1; i < 4; i++) {
            Node call = ASTHelper.mockEntityProjection1(i, ASTHelper::mockEntityProjectionNode2);
            call.ast().asf().infer();
            assertInstanceOf(STRING.class, call.outline());
        }
    }

    @Test
    void test_entity_hof_projection_3() {
        /*
        let f = (x,z,y)->z.combine(x,y).gender;
        f(20,{combine = (x,y)->{{age = x,name = y.name}},{name = "Will"})
         */
        for (int i = 1; i < 4; i++) {
            Node call = ASTHelper.mockEntityProjection1(i, ASTHelper::mockEntityProjectionNode3);
            call.ast().asf().infer();
            assertFalse(call.ast().errors().isEmpty());
            assertInstanceOf(AccessorGeneric.class, call.outline());
        }
    }

    @Test
    void test_entity_hof_projection_4() {
        /*
        let f = (x,z,y)->{
          var w = z;
          w.combine(x,y)
        };
        f(20,{combine = (x,y)->{{age = x,name = y.name}},{name = "Will"})
         */
        for (int i = 1; i < 4; i++) {
            Node call = ASTHelper.mockEntityProjection1(i, ASTHelper::mockEntityProjectionNode4);
            call.ast().asf().infer();
            Entity result = cast(call.outline());
            assertEquals("name", result.members().get(0).name());
            assertInstanceOf(STRING.class, result.members().get(0).outline());
            assertEquals("age", result.members().get(1).name());
            assertInstanceOf(INTEGER.class, result.members().get(1).outline());
        }

    }

    @Test
    void test_entity_hof_projection_5() {
        /*
        let f = (x,z,y)->{
          var w: {combine: Integer->{name: Integer}->{name: Integer}} = z;
          w.combine(x,y)
        };
        f(20,{combine = (x,y)->{{age = x,name = y.name}},{name = "Will"})
         */
        for (int i = 1; i < 4; i++) {
            Node call = ASTHelper.mockEntityProjection1(i, ASTHelper::mockEntityProjectionNode5);
            call.ast().asf().infer();
            call.lexeme();
            Entity result = cast(call.outline());
            assertEquals("name", result.members().getFirst().name());
            assertInstanceOf(INTEGER.class, result.members().getFirst().outline());
            assertFalse(call.ast().errors().isEmpty());
        }

    }
    @Test
    void test_extend_entity_projection(){
        /**
         * let f = fx<a,b>(x, y:a, z:{age:b})->{
         * 	 x = {name="Will”, age = y};
         * 	 z = x;
         * 	 x.age
         * }
         * let g = f({age=10});
         * let h = g(20);
         * f({age="10"},20);
         * h{age="100"})
         */
        AST ast = ASTHelper.mockExtendEntityProjection();
        assertTrue(ast.asf().infer());
        //assertTrue(false);
    }

    @Test
    void test_gcp_recursive_projection(){
        /*
        let factorial = n -> n==0?1:n*factorial(n-1);
        factorial(100);
        factorial(100);
         */
        AST ast = ASTHelper.mockRecursive();
        ast.asf().infer();
        VariableDeclarator declarator = cast(ast.program().body().statements().getFirst());
        Function<?,?> f = cast(declarator.assignments().getFirst().lhs().outline());
        Genericable<?,?> argument = cast(f.argument());
        Returnable returns = f.returns();
        assertInstanceOf(NUMBER.class, argument.definedToBe());
        assertInstanceOf(INTEGER.class, returns.supposedToBe());
        assertEquals(1,ast.errors().size());
    }

    @Test
    void test_complicated_hof_projection(){
        AST ast = ASTHelper.mockComplicatedHofProjection();
        assertTrue(ast.asf().infer());
        Assignment f = cast(ast.program().body().statements().getFirst().nodes().getFirst());
        Outline l = f.lhs().outline();
        FunctionBody body =  cast(f.nodes().get(1).nodes().get(1).nodes().get(0).nodes().get(0).nodes().get(1).nodes().get(0).nodes().get(0).nodes().get(1));
        Outline x = body.nodes().get(0).nodes().get(0).outline();
        assertEquals("`<a>-><a>`",x.toString());
        Outline y = body.nodes().get(1).nodes().get(0).outline();
        assertEquals("`<a>-><a>`",y.toString());
        Outline z = body.nodes().get(2).nodes().get(1).outline();
        assertEquals("`<a>-><a>`",z.toString());
        Outline fstr = ast.program().body().statements().get(1).nodes().getFirst().outline();
        assertEquals("(String->String)->(String->String)->(String->String)->String->String",fstr.toString());
        Node refInt = ast.program().body().statements().get(2).nodes().getFirst();
        Outline fint = refInt.outline();
        assertEquals("(Integer->Integer)->(Integer->Integer)->(Integer->Integer)->Integer->Integer",fint.toString());
        assertEquals(1,ast.errors().size());
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().get(0).errorCode());
        assertEquals(refInt,ast.errors().get(0).node());
        Outline fcallx = ast.program().body().statements().get(3).nodes().get(0).outline();
        assertTrue(true);
    }

    @Test
    void test_multi_extend_projection(){
        AST ast = ASTHelper.mockMultiExtendProjection();
        assertTrue(ast.asf().infer());
        Outline outline = ast.program().body().statements().getLast().outline();
        assertEquals("String",outline.toString());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void test_gcp_only_reference_for_simple_function(){
        /*
        let f = fx<a,b>(x:a)->{
           let y:b = 100;
           y
        }
        let f1 = f<String,Long>;
        f<String,String>;//String(b) doesn't match Integer(y:b=100)
        f1(100);
        */
        AST ast = ASTHelper.mockReferenceInFunction();
        VariableDeclarator declarator = new VariableDeclarator(ast,VariableKind.LET);
        ReferenceCallNode rCall = new ReferenceCallNode(new Identifier(ast,new Token<>("f")),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("String"))),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("Long"))));
        declarator.declare(new Identifier(ast,new Token<>("f1")),rCall);
        ast.addStatement(declarator);
        rCall = new ReferenceCallNode(new Identifier(ast,new Token<>("f")),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("String"))),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("String"))));
        ast.addStatement(new ExpressionStatement(rCall));
        FunctionCallNode fCall = new FunctionCallNode(new Identifier(ast,new Token<>("f1")),LiteralNode.parse(ast,new Token<>(100)));
        ast.addStatement(new ReturnStatement(fCall));
        assertTrue(ast.asf().infer());
        Outline f1 = ast.program().body().statements().get(1).nodes().getFirst().nodes().getFirst().outline();
        assertInstanceOf(FirstOrderFunction.class,f1);
        assertInstanceOf(STRING.class,((FirstOrderFunction)f1).argument().declaredToBe());
        assertInstanceOf(LONG.class,((FirstOrderFunction)f1).returns().supposedToBe());
        Outline ret = ast.program().body().statements().getLast().outline();
        assertInstanceOf(LONG.class,ret);
        assertEquals(2,ast.errors().size());
        assertEquals(GCPErrCode.REFERENCE_MIS_MATCH,ast.errors().getFirst().errorCode());
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().getLast().errorCode());
    }

    @Test
    void test_gcp_argument_reference_for_simple_function(){
        /*
        let f = func<a,b>(x:a)->{
           let y:b = 100;
           y
        }
        f(100)
        */
        AST ast = ASTHelper.mockReferenceInFunction();
        FunctionCallNode fCall = new FunctionCallNode(new Identifier(ast,new Token<>("f")),LiteralNode.parse(ast,new Token<>(100)));
        ast.addStatement(new ReturnStatement(fCall));
        ast.asf().infer();
        Outline ret = ast.program().body().statements().getLast().outline();
        assertInstanceOf(INTEGER.class,ret);
    }

    @Test
    void test_gcp_only_reference_for_entity_return_function(){
        /*
        let g = fx<a,b>()->{
           {
                z:a = 100,
                f = fx<c>(x:b,y:c)->y
            }
        }*/
        AST ast = ASTHelper.mockReferenceInEntity();
        //g<Integer,String>
        ReferenceCallNode rCall = new ReferenceCallNode(new Identifier(ast,new Token<>("g")),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("Integer"))),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("String"))));
        FunctionCallNode fCall = new FunctionCallNode(rCall);
        //g<Integer,String>.f
        MemberAccessor accessor = new MemberAccessor(fCall,new Identifier(ast,new Token<>("f")));
        VariableDeclarator f1Declare = new VariableDeclarator(ast,VariableKind.LET);
        f1Declare.declare(new Identifier(ast,new Token<>("f1")),accessor);
        ast.addStatement(f1Declare);
        //g<Integer,String>.f<Long>
        ReferenceCallNode call = new ReferenceCallNode(new Identifier(ast,new Token<>("f1")),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("Long"))));
        VariableDeclarator f2Declare = new VariableDeclarator(ast,VariableKind.LET);
        f2Declare.declare(new Identifier(ast,new Token<>("f2")),call);
        ast.addStatement(f2Declare);
        ast.asf().infer();
        //check initialed
        VariableDeclarator g = cast(ast.program().body().statements().getFirst());
        Function<?,?> goutline = cast(g.assignments().getFirst().lhs().outline());
        Entity originEntity = cast(goutline.returns().supposedToBe());
        EntityMember originZ = originEntity.members().getLast();
        assertEquals("<a>",originZ.outline().toString());
        assertEquals("<a>",originZ.node().outline().toString());

        //check g<Integer,String>
        Entity entity = cast(((Function<?,Genericable<?,?>>)rCall.outline()).returns().supposedToBe());
        EntityMember z = entity.members().getLast();
        assertInstanceOf(INTEGER.class,z.outline());
        assertInstanceOf(INTEGER.class,z.node().outline());
        //let f1 = g<Integer,String>().f;
        FirstOrderFunction f1 = cast(f1Declare.assignments().getFirst().lhs().outline());
        Function<?,Genericable<?,?>> retF1 = cast(f1.returns().supposedToBe());
        assertInstanceOf(STRING.class, f1.argument().declaredToBe());
        assertInstanceOf(Reference.class, retF1.argument());
        assertEquals("c", retF1.argument().name());
        assertEquals("<c>", ((Genericable<?,?>)retF1.returns().supposedToBe()).toString());

        //let f2 = f1<Long>;
        FirstOrderFunction f2 = cast(f2Declare.assignments().getFirst().lhs().outline());
        Function<?,Genericable<?,?>> retF2 = cast(f2.returns().supposedToBe());
        assertInstanceOf(LONG.class, retF2.argument().declaredToBe());
        assertInstanceOf(LONG.class, retF2.returns().supposedToBe());
    }
    @Test
    void test_array_projection(){
        AST ast = ASTHelper.mockArrayAsArgument();
        assertTrue(ast.asf().infer());
        //f([{name = "Will"}]) : {name: String};
        Entity outline_1 = cast(ast.program().body().statements().get(3).get(0).outline());
        assertEquals("name",outline_1.members().get(0).name());
        assertInstanceOf(STRING.class,outline_1.members().get(0).outline());
        assertEquals(4,ast.errors().size());
        //f(100) : `any`;
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().get(1).errorCode());
        assertEquals("100",ast.errors().get(1).node().toString());
        //g(["a","b"],0) : String;
        Outline outline_2 = ast.program().body().statements().get(5).get(0).outline();
        assertInstanceOf(STRING.class,outline_2);
        //g([1],"idx") : Integer; plus "idx" mis match error
        Outline outline_3 = ast.program().body().statements().get(5).get(0).outline();
        assertInstanceOf(STRING.class,outline_3);
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().get(2).errorCode());
        assertEquals("[1]",ast.errors().get(2).node().toString());
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().get(3).errorCode());
        assertEquals("\"idx\"",ast.errors().get(3).node().toString());
        //r1([1,2]) : Integer;
        Outline outline_4 = ast.program().body().statements().get(8).get(0).outline();
        assertInstanceOf(INTEGER.class,outline_4);
        //let r2 = r<String>;
        assertEquals(GCPErrCode.PROJECT_FAIL,ast.errors().get(0).errorCode());
        assertEquals("r<String>",ast.errors().get(0).node().toString());
//        Function<?,?> outline_5 = cast(ast.program().body().statements().get(9).get(0).get(0).outline());
//        assertInstanceOf(STRING.class,outline_5.returns().supposedToBe());
//        Array arr = cast(((Genericable)outline_5.argument()).declaredToBe());
//        assertInstanceOf(STRING.class,arr.itemOutline());
        //r([1,2]) : Integer;
        Outline outline_6 = ast.program().body().statements().get(10).get(0).outline();
        assertInstanceOf(INTEGER.class,outline_6);
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
