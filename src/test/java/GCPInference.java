import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.exception.GCPErrCode;
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

import static org.twelve.gcp.common.Tool.cast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GCPInference {
    @Test
    void test_gcp_declare_to_be() {
        //let f = x->x
        OAST ast = mockGCPTestAst();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"), Outline.Integer);
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token("x"))));
        FunctionNode f = FunctionNode.from(body, x);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);
        //f("some")
        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("f"), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), LiteralNode.parse(ast, new Token(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();
        //f.outline is a function
        assertTrue(f.outline() instanceof FirstOrderFunction);
        //f.argument is a Generic outline
        assertTrue(f.argument().outline() instanceof Generic);
        //f.argument.declared_to_be = Integer
        assertEquals(Outline.Integer, f.argument().outline().declaredToBe());
        //f.return is a Return outline
        assertTrue(f.body().outline() instanceof Return);
        //f.return = f.argument  (return x;)
        assertEquals(f.argument().outline(), ((Return) f.body().outline()).supposedToBe());
        //call1: gcp error
        assertEquals(1, ast.errors().size());
        assertEquals(some, ast.errors().get(0).node());
        assertEquals(GCPErrCode.PROJECT_FAIL, ast.errors().get(0).errorCode());
        //call2: Integer
        assertEquals(Outline.Integer.toString(), call2.outline().toString());

    }

    @Test
    void test_gcp_extend_to_be() {
        //let f = fn(x){x=10; x}
        OAST ast = mockGCPTestAst();
        Argument x = new Argument(ast, new Token("x"));
        FunctionBody body = new FunctionBody(ast);

        Assignment assignment = new Assignment(new Identifier(ast, new Token("x")), LiteralNode.parse(ast, new Token(10)));
        body.addStatement(assignment);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token("x"))));
        FunctionNode f = FunctionNode.from(body, x);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);

        //f("some")
        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("f"), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), LiteralNode.parse(ast, new Token(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();

        Return returns = cast(((FirstOrderFunction) f.outline()).returns());
        assertEquals(Outline.Integer.toString(), ((Generic) returns.supposedToBe()).extendToBe().toString());

        //f("some") project fail
        assertTrue( call1.outline() instanceof INTEGER);
        assertEquals(GCPErrCode.PROJECT_FAIL, ast.errors().get(0).errorCode());

        //f(10)
        assertEquals(Outline.Integer.toString(), call2.outline().toString());

    }

    @Test
    void test_gcp_has_to_be() {
        //let f = fn(x){var y="str"; y = x; x}
        OAST ast = mockGCPTestAst();
        Argument x = new Argument(ast, new Token("x"));
        FunctionBody body = new FunctionBody(ast);

        VariableDeclarator yDeclare = new VariableDeclarator(ast, VariableKind.VAR);
        yDeclare.declare(new Token("y"), LiteralNode.parse(ast, new Token("str")));
        body.addStatement(yDeclare);


        Assignment assignment = new Assignment(new Identifier(ast, new Token("y")), new Identifier(ast, new Token("x")));
        body.addStatement(assignment);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token("x"))));
        FunctionNode f = FunctionNode.from(body, x);

        VariableDeclarator fDeclare = new VariableDeclarator(ast, VariableKind.LET);
        fDeclare.declare(new Token("f"), f);
        ast.addStatement(fDeclare);

        //f("some")
        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), some);
        ast.addStatement(new ExpressionStatement(call2));
        //f(10)
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("f"), LiteralNode.parse(ast, new Token(100)));
        ast.addStatement(new ExpressionStatement(call1));
        ast.asf().infer();

//        Generic returns = cast(((Return)((Function) f.outline()).returns()).supposedToBe());
        Return returns = cast(((FirstOrderFunction) f.outline()).returns());
        assertTrue(((Generic) returns.supposedToBe()).hasToBe() instanceof STRING);

        //f(100) project fail
        assertTrue( call1.outline() instanceof STRING);
        assertEquals(GCPErrCode.PROJECT_FAIL, ast.errors().get(0).errorCode());

        //f("some")
        assertTrue(call2.outline() instanceof STRING);
    }

    @Test
    void test_gcp_defined_to_be() {
        //let f = x->x+1
        OAST ast = mockGCPTestAst();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        FunctionBody body = new FunctionBody(ast);
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token("x")),
                LiteralNode.parse(ast, new Token(1)), new OperatorNode(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add1));
        FunctionNode f = FunctionNode.from(body, x);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);
        //f("some")
        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("f"), some);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), LiteralNode.parse(ast, new Token(100)));
        ast.addStatement(new ExpressionStatement(call2));
        ast.asf().infer();
        //f.outline is a function
        assertTrue(f.outline() instanceof FirstOrderFunction);
        //f.argument is a Generic outline
        assertTrue(f.argument().outline() instanceof Generic);
        //f.return is a Return outline
        assertTrue(((Return) f.body().outline()).supposedToBe() instanceof Addable);
        //f.return.suppose_to_be = f.argument  (return x;)
        assertTrue(f.argument().outline().definedToBe().is(Option.StringOrNumber));
        //call1: gcp error
        assertEquals(0, ast.errors().size());
        //call2: Integer
        assertTrue(call1.outline() instanceof STRING);
        assertEquals(Outline.Integer.toString(), call2.outline().toString());

    }

    @Test
    void test_gcp_add_expression() {
        //let f = (x,y)->x+y
        OAST ast = mockGCPTestAst();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        Argument y = new Argument(ast, new Token("y"));
        FunctionBody body = new FunctionBody(ast);
        BinaryExpression add1 = new BinaryExpression(new Identifier(ast, new Token("x")),
                new Identifier(ast, new Token("y")), new OperatorNode(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add1));
        FunctionNode f = FunctionNode.from(body, x, y);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);

        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        LiteralNode people = LiteralNode.parse(ast, new Token("people"));
        LiteralNode intNum = LiteralNode.parse(ast, new Token(10));
        LiteralNode floatNum = LiteralNode.parse(ast, new Token(10f));
        //f("some",10)
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("f"), some, intNum);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10,10f)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), intNum, floatNum);
        ast.addStatement(new ExpressionStatement(call2));

        //let z = f("some");
        declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token("z"), new FunctionCallNode(ast, new Token("f"), some));
        ast.addStatement(declare);
        //z("people");
        FunctionCallNode call3 = new FunctionCallNode(ast, new Token("z"), people);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        //f.outline is a function
        assertTrue(f.outline() instanceof FirstOrderFunction);
        //f.argument is a Generic outline
        assertTrue(f.argument().outline() instanceof Generic);
        //f.return is a Return outline
        assertTrue(f.body().outline() instanceof Return);
        //f.return.suppose_to_be = f.argument  (return x;)
        assertTrue(f.argument().outline().definedToBe().is(Option.StringOrNumber));
        //call1: gcp error
        assertEquals(0, ast.errors().size());
        //call2: Integer
        assertTrue(call1.outline() instanceof STRING);
        assertEquals(Outline.Float.toString(), call2.outline().toString());
        assertTrue(call3.outline() instanceof STRING);
    }

    @Test
    void test_generic_refer_each_other() {
        OAST ast = mockGCPTestAst();
        //f = (x,y,z)->{y = x; z=y; x+y;}
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        Argument y = new Argument(ast, new Token("y"));
        Argument z = new Argument(ast, new Token("z"));
        FunctionBody body = new FunctionBody(ast);
        Assignment assignment = new Assignment(new Identifier(ast, new Token("y")), new Identifier(ast, new Token("x")));
        body.addStatement(assignment);
        assignment = new Assignment(new Identifier(ast, new Token("z")), new Identifier(ast, new Token("y")));
        body.addStatement(assignment);

        BinaryExpression add = new BinaryExpression(new Identifier(ast, new Token("x")),
                new Identifier(ast, new Token("y")), new OperatorNode(ast, BinaryOperator.ADD));
        add = new BinaryExpression(add, new Identifier(ast, new Token("z")), new OperatorNode(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add));
        FunctionNode f = FunctionNode.from(body, x, y, z);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);

        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        LiteralNode people = LiteralNode.parse(ast, new Token("people"));
        LiteralNode intNum = LiteralNode.parse(ast, new Token(10));
        LiteralNode floatNum = LiteralNode.parse(ast, new Token(10f));
        //f("some","people",10)
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("f"), some, people, floatNum);
        ast.addStatement(new ExpressionStatement(call1));
        //f(10,10,10)
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), intNum, intNum, intNum);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertEquals(1, ast.errors().size());
        assertEquals(floatNum, ast.errors().get(0).node());
        assertTrue(call1.outline() instanceof STRING);
//        assertEquals(0, ast.errors().size());
//        assertEquals(Outline.String, call1.outline());
        assertEquals(Outline.Integer.toString(), call2.outline().toString());
    }

    @Test
    void test_gcp_hof_projection_1() {
        //let f = (x,y)->y(x)
        OAST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        Argument y = new Argument(ast, new Token("y"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("y"), new Identifier(ast, new Token("x")));
        body.addStatement(new ReturnStatement(call1));
        FunctionNode f = FunctionNode.from(body, x, y);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);
        //f(10,x->x*5);
        LiteralNode ten = LiteralNode.parse(ast, new Token(10));
        LiteralNode five = LiteralNode.parse(ast, new Token(5));
        x = new Argument(ast, new Token("x"));
        body = new FunctionBody(ast);
        BinaryExpression tenTimes = new BinaryExpression(new Identifier(ast, new Token("x")),
                five, new OperatorNode(ast, BinaryOperator.MULTIPLY));
        body.addStatement(new ReturnStatement(tenTimes));
        FunctionNode lambda = FunctionNode.from(body, x);
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), ten, lambda);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertEquals(Outline.Integer.toString(), call2.outline().toString());
    }

    @Test
    void test_gcp_hof_projection_2() {
        //let f = (y,x)->y(x)
        OAST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        Argument y = new Argument(ast, new Token("y"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("y"), new Identifier(ast, new Token("x")));
        body.addStatement(new ReturnStatement(call1));
        FunctionNode f = FunctionNode.from(body, y, x);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);
        //f(x->x+5,10);
        LiteralNode ten = LiteralNode.parse(ast, new Token("10"));
        LiteralNode five = LiteralNode.parse(ast, new Token(5));
        x = new Argument(ast, new Token("x"));
        body = new FunctionBody(ast);
        BinaryExpression tenTimes = new BinaryExpression(new Identifier(ast, new Token("x")),
                five, new OperatorNode(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(tenTimes));
        FunctionNode lambda = FunctionNode.from(body, x);
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("f"), lambda, ten);
        ast.addStatement(new ExpressionStatement(call2));

        ast.asf().infer();
        assertTrue(call2.outline() instanceof STRING);
    }

    @Test
    void test_gcp_hof_projection_3() {
        //let f = (x,y,z)->z(y(x))
        OAST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        Argument y = new Argument(ast, new Token("y"));
        Argument z = new Argument(ast, new Token("z"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("y"), new Identifier(ast, new Token("x")));
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("z"), call1);
        body.addStatement(new ReturnStatement(call2));
        FunctionNode f = FunctionNode.from(body, x, y, z);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);
        //f(10,x->x+"some",y->y+100)
        LiteralNode ten = LiteralNode.parse(ast, new Token(10));
        LiteralNode hundred = LiteralNode.parse(ast, new Token(100));
        LiteralNode some = LiteralNode.parse(ast, new Token("some"));
        y = new Argument(ast, new Token("y"));
        x = new Argument(ast, new Token("x"));
        //x+"some"
        BinaryExpression addSome = new BinaryExpression(new Identifier(ast, new Token("x")),
                some, new OperatorNode(ast, BinaryOperator.ADD));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(addSome));
        FunctionNode arg1 = FunctionNode.from(body, x);
        //y+100
        BinaryExpression add100 = new BinaryExpression(new Identifier(ast, new Token("y")),
                hundred, new OperatorNode(ast, BinaryOperator.ADD));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(add100));
        FunctionNode arg2 = FunctionNode.from(body, y);
        FunctionCallNode call3 = new FunctionCallNode(ast, new Token("f"), ten, arg1, arg2);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        assertTrue(call3.outline() instanceof STRING);
    }

    @Test
    void test_gcp_hof_projection_4() {
        //let f = (z,y,x)->z(y(x))
        OAST ast = mockGCPTestAst();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token("x"));
        Argument y = new Argument(ast, new Token("y"));
        Argument z = new Argument(ast, new Token("z"));
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call1 = new FunctionCallNode(ast, new Token("y"), new Identifier(ast, new Token("x")));
        FunctionCallNode call2 = new FunctionCallNode(ast, new Token("z"), call1);
        body.addStatement(new ReturnStatement(call2));
        FunctionNode f = FunctionNode.from(body, z, y, x);
        declare.declare(new Token("f"), f);
        ast.addStatement(declare);
        //f(y->y+100,x->x,"some")
        LiteralNode ten = LiteralNode.parse(ast, new Token(10));
        LiteralNode hundred = LiteralNode.parse(ast, new Token(100));
        y = new Argument(ast, new Token("y"));
        x = new Argument(ast, new Token("x"));
        //x->x
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token("x"))));
        FunctionNode arg1 = FunctionNode.from(body, x);
        //y+100
        BinaryExpression add100 = new BinaryExpression(new Identifier(ast, new Token("y")),
                hundred, new OperatorNode(ast, BinaryOperator.ADD));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(add100));
        FunctionNode arg2 = FunctionNode.from(body, y);
        FunctionCallNode call3 = new FunctionCallNode(ast, new Token("f"), arg2, arg1, ten);
        ast.addStatement(new ExpressionStatement(call3));

        ast.asf().infer();
        assertTrue(call3.outline() instanceof INTEGER);
    }


    @Test
    void test_entity_hof_projection_1() {
        //let f = (x,z,y)-> z.combine(x,y);
        //f(20,{combine = (x,y)->{{
        //        age = x1,
        //        name = y1.name,
        //      }},{name = "Will"})
        for(int i=1; i<4; i++) {
            ONode call = ASTHelper.mockEntityProjection1(i,body->ASTHelper.mockEntityProjectionNode1(body));
            call.ast().asf().infer();
            Entity result = cast(call.outline());
            assertEquals("name", result.members().get(0).name());
            assertTrue(result.members().get(0).outline() instanceof STRING);
            assertEquals("age", result.members().get(1).name());
            assertTrue(result.members().get(1).outline() instanceof INTEGER);
        }

    }
    @Test
    void test_entity_hof_projection_2() {
        for(int i=1; i<4; i++) {
            ONode call = ASTHelper.mockEntityProjection1(i,body->ASTHelper.mockEntityProjectionNode2(body));
            call.ast().asf().infer();
            assertTrue(call.outline() instanceof STRING);
        }
    }
    @Test
    void test_entity_hof_projection_3() {
        for(int i=1; i<4; i++) {
            ONode call = ASTHelper.mockEntityProjection1(i,body->ASTHelper.mockEntityProjectionNode3(body));
            call.ast().asf().infer();
            assertTrue(call.ast().errors().size()>0);
            assertTrue(call.outline() instanceof AccessorGeneric);
        }
    }
    @Test
    void test_entity_hof_projection_4() {
        //let f = (x,z,y)-> var w = z; w.combine(x,y);
        //f(20,{combine = (x,y)->{{
        //        age = x1,
        //        name = y1.name,
        //      }},{name = "Will"})
        for(int i=1; i<4; i++) {
            ONode call = ASTHelper.mockEntityProjection1(i,body->ASTHelper.mockEntityProjectionNode4(body));
            call.ast().asf().infer();
            Entity result = cast(call.outline());
            assertEquals("name", result.members().get(0).name());
            assertTrue(result.members().get(0).outline() instanceof STRING);
            assertEquals("age", result.members().get(1).name());
            assertTrue(result.members().get(1).outline() instanceof INTEGER);
        }

    }

    @Test
    void test_entity_hof_projection_5() {
        //let f = (x,z,y)-> var w = z; w.combine(x,y);
        //f(20,{combine = (x,y)->{{
        //        age = x1,
        //        name = y1.name,
        //      }},{name = "Will"})
        for(int i=1; i<4; i++) {
            ONode call = ASTHelper.mockEntityProjection1(i,body->ASTHelper.mockEntityProjectionNode5(body));
            call.ast().asf().infer();
            assertTrue(call.ast().errors().size()==0);
            Entity result = cast(call.outline());
            assertEquals("name", result.members().get(0).name());
            assertTrue(result.members().get(0).outline() instanceof STRING);

        }

    }

    @Test
    void test_gcp_complicated_hof_projection() {
//todo
    }

    private static OAST mockGCPTestAst() {
        ASF asf = new ASF();
        OAST ast = asf.newAST();
        List<Token> namespace = new ArrayList<>();
        namespace.add(new Token("test"));
        ast.setNamespace(namespace);
        return ast;
    }
}
