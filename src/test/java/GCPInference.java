import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inference;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.ExpressionStatement;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.STRING;
import org.twelve.gcp.outline.projectable.*;

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
        Argument x = new Argument(ast, new Token<>("x"), Outline.Integer);
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode f = FunctionNode.from(body, x);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);
        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("f"), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();
        //f.outline is a function
        assertInstanceOf(FirstOrderFunction.class, f.outline());
        //f.argument is a Generic outline
        assertInstanceOf(Generic.class, f.argument().outline());
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
        Argument x = new Argument(ast, new Token<>("x"));
        FunctionBody body = new FunctionBody(ast);

        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("x")), LiteralNode.parse(ast, new Token<>(10)));
        body.addStatement(assignment);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode f = FunctionNode.from(body, x);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);

        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("f"), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();

        Return returns = cast(((FirstOrderFunction) f.outline()).returns());
        assertEquals(Outline.Integer.toString(), ((Generic) returns.supposedToBe()).extendToBe().toString());

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
        Argument x = new Argument(ast, new Token<>("x"));
        FunctionBody body = new FunctionBody(ast);

        VariableDeclarator yDeclare = new VariableDeclarator(ast, VariableKind.VAR);
        yDeclare.declare(new Token<>("y"), LiteralNode.parse(ast, new Token<>("str")));
        body.addStatement(yDeclare);


        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("x")));
        body.addStatement(assignment);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("x"))));
        FunctionNode f = FunctionNode.from(body, x);

        VariableDeclarator fDeclare = new VariableDeclarator(ast, VariableKind.LET);
        fDeclare.declare(new Token<>("f"), f);
        ast.addStatement(fDeclare);

        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), some);
        ast.addStatement(new ExpressionStatement(call2));
        //f(10)
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("f"), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call1));
        ast.asf().infer();

//        Generic returns = cast(((Return)((Function) f.outline()).returns()).supposedToBe());
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
        Argument x = new Argument(ast, new Token<>("x"));
        FunctionBody body = new FunctionBody(ast);
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                LiteralNode.parse(ast, new Token<>(1)), new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add1));
        FunctionNode f = FunctionNode.from(body, x);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);
        //f("some")
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("f"), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), LiteralNode.parse(ast, new Token<>(100)));
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
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        FunctionBody body = new FunctionBody(ast);
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                new Identifier(ast, new Token<>("y")), new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add1));
        FunctionNode f = FunctionNode.from(body, x, y);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);

        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<String> people = LiteralNode.parse(ast, new Token<>("people"));
        LiteralNode<Integer> intNum = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Float> floatNum = LiteralNode.parse(ast, new Token<>(10f));
        //f("some",10)
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("f"), some, intNum);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10,10f)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), intNum, floatNum);
        ast.addStatement(new ExpressionStatement(call2));

        //let z = f("some");
        declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("z"), new FunctionCallNode(ast, new Token<>("f"), some));
        ast.addStatement(declare);
        //z("people");
        FunctionCallNode call3 = new FunctionCallNode(ast, new Token<>("z"), people);
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
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        Argument z = new Argument(ast, new Token<>("z"));
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
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);

        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        LiteralNode<String> people = LiteralNode.parse(ast, new Token<>("people"));
        LiteralNode<Integer> intNum = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Float> floatNum = LiteralNode.parse(ast, new Token<>(10f));
        //f("some","people",10)
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("f"), some, people, floatNum);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10,10,10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), intNum, intNum, intNum);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertEquals(1, ast.errors().size());
        assertEquals(floatNum, ast.errors().getFirst().node());
        assertInstanceOf(STRING.class, call1.outline());
        assertEquals(Outline.Integer.toString(), call2.outline().toString());
    }

    @Test
    void test_gcp_hof_projection_1() {
        /*
        let f = (x,y)->y(x);
        f(10,x->x*5);
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("y"), new Identifier(ast, new Token<>("x")));
        body.addStatement(new ReturnStatement(call1));
        FunctionNode f = FunctionNode.from(body, x, y);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);
        //f(10,x->x*5);
        LiteralNode<Integer> ten = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Integer> five = LiteralNode.parse(ast, new Token<>(5));
        x = new Argument(ast, new Token<>("x"));
        body = new FunctionBody(ast);
        BinaryExpression tenTimes = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                five, new OperatorNode<>(ast, BinaryOperator.MULTIPLY));
        body.addStatement(new ReturnStatement(tenTimes));
        FunctionNode lambda = FunctionNode.from(body, x);
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), ten, lambda);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertEquals(Outline.Integer.toString(), call2.outline().toString());
    }

    @Test
    void test_gcp_hof_projection_2() {
        /*
        let f = (y,x)->y(x);
        f(x->x+5,"10");
         */
        AST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("y"), new Identifier(ast, new Token<>("x")));
        body.addStatement(new ReturnStatement(call1));
        FunctionNode f = FunctionNode.from(body, y, x);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);
        //f(x->x+5,10);
        LiteralNode<String> ten = LiteralNode.parse(ast, new Token<>("10"));
        LiteralNode<Integer> five = LiteralNode.parse(ast, new Token<>(5));
        x = new Argument(ast, new Token<>("x"));
        body = new FunctionBody(ast);
        BinaryExpression tenTimes = new BinaryExpression(new Identifier(ast, new Token<>("x")),
                five, new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(tenTimes));
        FunctionNode lambda = FunctionNode.from(body, x);
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("f"), lambda, ten);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
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
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        Argument z = new Argument(ast, new Token<>("z"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("y"), new Identifier(ast, new Token<>("x")));
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("z"), call1);
        body.addStatement(new ReturnStatement(call2));
        FunctionNode f = FunctionNode.from(body, x, y, z);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);
        //f(10,x->x+"some",y->y+100)
        LiteralNode<Integer> ten = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Integer> hundred = LiteralNode.parse(ast, new Token<>(100));
        LiteralNode<String> some = LiteralNode.parse(ast, new Token<>("some"));
        y = new Argument(ast, new Token<>("y"));
        x = new Argument(ast, new Token<>("x"));
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
        FunctionCallNode call3 = new FunctionCallNode(ast, new Token<>("f"), ten, arg1, arg2);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
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
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        Argument z = new Argument(ast, new Token<>("z"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token<>("y"), new Identifier(ast, new Token<>("x")));
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token<>("z"), call1);
        body.addStatement(new ReturnStatement(call2));
        FunctionNode f = FunctionNode.from(body, z, y, x);
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);
        //f(y->y+100,x->x,"some")
        LiteralNode<Integer> ten = LiteralNode.parse(ast, new Token<>(10));
        LiteralNode<Integer> hundred = LiteralNode.parse(ast, new Token<>(100));
        y = new Argument(ast, new Token<>("y"));
        x = new Argument(ast, new Token<>("x"));
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
        FunctionCallNode call3 = new FunctionCallNode(ast, new Token<>("f"), arg2, arg1, ten);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        assertInstanceOf(INTEGER.class, call3.outline());
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
    void test_gcp_recursive_projection(){
        /*
        let chain = (f, x) -> x>0?chain(f, f(x)):“done”;
        chain(x->x-1,100);
        chain(x->x-1,"100");
         */
        AST ast = ASTHelper.mockRecursive();
//        ast.asf().infer();
//todo
    }

    private static AST mockGCPTestAst() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("test"));
        ast.setNamespace(namespace);
        return ast;
    }
}
