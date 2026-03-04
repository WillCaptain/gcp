import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.interpreter.OutlineInterpreter;
import org.twelve.gcp.interpreter.interpretation.BuiltinMethods;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.ReturnStatement;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Promise;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.primitive.STRING;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code async}/{@code await} feature:
 *
 * <ul>
 *   <li>Inference: {@code async expr} infers to {@code Promise<T>}</li>
 *   <li>Inference: {@code await promise} infers to {@code T}</li>
 *   <li>Inference: {@code await non-promise} reports a type error</li>
 *   <li>Interpretation: {@code async expr} evaluates to {@link PromiseValue}</li>
 *   <li>Interpretation: {@code await (async expr)} resolves to the inner value</li>
 *   <li>Interpretation: async captures the lexical environment correctly</li>
 * </ul>
 */
class AsyncTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private OutlineInterpreter run(AST ast) {
        ast.asf().infer();
        OutlineInterpreter interp = new OutlineInterpreter(ast.asf());
        interp.runAst(ast);
        return interp;
    }

    private Value get(OutlineInterpreter interp, String name) {
        return interp.currentEnv().lookup(name);
    }

    // =========================================================================
    // Inference tests
    // =========================================================================

    @Test
    void infer_async_integer_literal_is_promise_integer() {
        /*
         * let p = async 42
         * p : Promise<Integer>
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        asf.infer();

        VariableDeclarator decl = (VariableDeclarator) ast.program().body().statements().getFirst();
        Outline pOutline = decl.assignments().getFirst().lhs().outline();
        assertInstanceOf(Promise.class, pOutline);
        Promise promise = (Promise) pOutline;
        assertInstanceOf(INTEGER.class, promise.innerOutline());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void infer_async_string_literal_is_promise_string() {
        /*
         * let p = async "hello"
         * p : Promise<String>
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>("hello")));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        asf.infer();

        VariableDeclarator decl = (VariableDeclarator) ast.program().body().statements().getFirst();
        Outline pOutline = decl.assignments().getFirst().lhs().outline();
        assertInstanceOf(Promise.class, pOutline);
        Promise promise = (Promise) pOutline;
        assertInstanceOf(STRING.class, promise.innerOutline());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void infer_async_function_is_promise_function() {
        /*
         * let p = async (x) { 1 }
         * p : Promise<Integer -> Integer>
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>(1))));
        FunctionNode fn = FunctionNode.from(body, new Argument(new Identifier(ast, new Token<>("x"))));

        AsyncNode asyncNode = new AsyncNode(ast, fn);
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        asf.infer();

        VariableDeclarator decl = (VariableDeclarator) ast.program().body().statements().getFirst();
        Outline pOutline = decl.assignments().getFirst().lhs().outline();
        assertInstanceOf(Promise.class, pOutline);
        Promise promise = (Promise) pOutline;
        assertInstanceOf(FirstOrderFunction.class, promise.innerOutline());
        assertTrue(ast.errors().isEmpty());
    }

    @Test
    void infer_await_extracts_inner_type() {
        /*
         * let p = async 42
         * let x = await p
         * x : Integer
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("p")));
        ast.addStatement(ASTHelper.varLet(ast, "x", awaitNode));

        asf.infer();

        assertTrue(ast.errors().isEmpty());
        VariableDeclarator xDecl = (VariableDeclarator) ast.program().body().statements().get(1);
        assertInstanceOf(INTEGER.class, xDecl.assignments().getFirst().lhs().outline());
    }

    @Test
    void infer_await_on_non_promise_reports_error() {
        /*
         * let n = 42
         * let x = await n   // type error: Integer is not Promise<T>
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        ast.addStatement(ASTHelper.varLet(ast, "n", LiteralNode.parse(ast, new Token<>(42))));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("n")));
        ast.addStatement(ASTHelper.varLet(ast, "x", awaitNode));

        asf.infer();

        assertFalse(ast.errors().isEmpty(), "Expected a type error for await on non-Promise");
    }

    @Test
    void infer_promise_toString_matches_inner_type() {
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(99)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));
        asf.infer();

        VariableDeclarator decl = (VariableDeclarator) ast.program().body().statements().getFirst();
        Outline pOutline = decl.assignments().getFirst().lhs().outline();
        assertInstanceOf(Promise.class, pOutline);
        assertEquals("Promise<Integer>", pOutline.toString());
    }

    // =========================================================================
    // Interpretation tests
    // =========================================================================

    @Test
    void interpret_async_produces_promise_value() {
        /*
         * let p = async 42
         * p is a PromiseValue (pending or resolved)
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        OutlineInterpreter interp = run(ast);
        Value p = get(interp, "p");

        assertInstanceOf(PromiseValue.class, p);
    }

    @Test
    void interpret_await_resolves_promise_to_int() {
        /*
         * let p = async 42
         * let x = await p
         * x == 42
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("p")));
        ast.addStatement(ASTHelper.varLet(ast, "x", awaitNode));

        OutlineInterpreter interp = run(ast);

        assertInstanceOf(PromiseValue.class, get(interp, "p"));
        assertEquals(new IntValue(42L), get(interp, "x"));
    }

    @Test
    void interpret_await_resolves_promise_to_string() {
        /*
         * let p = async "world"
         * let s = await p
         * s == "world"
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>("world")));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("p")));
        ast.addStatement(ASTHelper.varLet(ast, "s", awaitNode));

        OutlineInterpreter interp = run(ast);

        assertEquals(new StringValue("world"), get(interp, "s"));
    }

    @Test
    void interpret_async_captures_lexical_environment() {
        /*
         * let n = 10
         * let p = async n     // captures current value of n
         * let r = await p
         * r == 10
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        ast.addStatement(ASTHelper.varLet(ast, "n", LiteralNode.parse(ast, new Token<>(10))));

        AsyncNode asyncNode = new AsyncNode(ast, new Identifier(ast, new Token<>("n")));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("p")));
        ast.addStatement(ASTHelper.varLet(ast, "r", awaitNode));

        OutlineInterpreter interp = run(ast);

        assertEquals(new IntValue(10L), get(interp, "r"));
    }

    @Test
    void interpret_inline_await_async_expression() {
        /*
         * let r = await (async 7)
         * r == 7
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(7)));
        AwaitNode awaitNode = new AwaitNode(ast, asyncNode);
        ast.addStatement(ASTHelper.varLet(ast, "r", awaitNode));

        OutlineInterpreter interp = run(ast);

        assertEquals(new IntValue(7L), get(interp, "r"));
    }

    @Test
    void interpret_async_binary_expression() {
        /*
         * let p = async (3 + 4)
         * let r = await p
         * r == 7
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        BinaryExpression addExpr = new BinaryExpression(
                LiteralNode.parse(ast, new Token<>(3)),
                LiteralNode.parse(ast, new Token<>(4)),
                new OperatorNode<>(ast, BinaryOperator.ADD));

        AsyncNode asyncNode = new AsyncNode(ast, addExpr);
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("p")));
        ast.addStatement(ASTHelper.varLet(ast, "r", awaitNode));

        OutlineInterpreter interp = run(ast);

        VariableDeclarator pDecl = (VariableDeclarator) ast.program().body().statements().getFirst();
        assertInstanceOf(Promise.class, pDecl.assignments().getFirst().lhs().outline());
        assertEquals(new IntValue(7L), get(interp, "r"));
    }

    @Test
    void interpret_async_function_and_call_result() {
        /*
         * let f = async (() -> 99)
         * let fn = await f      // fn is the function value
         * let r  = fn()         // call the resolved function
         * r == 99
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>(99))));
        FunctionNode fn = FunctionNode.from(body);

        AsyncNode asyncNode = new AsyncNode(ast, fn);
        ast.addStatement(ASTHelper.varLet(ast, "f", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("f")));
        ast.addStatement(ASTHelper.varLet(ast, "fn", awaitNode));

        ast.addStatement(ASTHelper.varLet(ast, "r",
                ASTHelper.call(ast, new Identifier(ast, new Token<>("fn")))));

        OutlineInterpreter interp = run(ast);

        assertInstanceOf(FunctionValue.class, get(interp, "fn"));
        assertEquals(new IntValue(99L), get(interp, "r"));
    }

    @Test
    void interpret_promise_display_shows_resolved() throws Exception {
        /*
         * let p = async 5
         * After await, the future is done → display shows "Promise<resolved:5>"
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(5)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        AwaitNode awaitNode = new AwaitNode(ast, new Identifier(ast, new Token<>("p")));
        ast.addStatement(ASTHelper.varLet(ast, "r", awaitNode));

        OutlineInterpreter interp = run(ast);

        PromiseValue pv = (PromiseValue) get(interp, "p");
        pv.future().get(); // ensure resolved
        assertTrue(pv.display().startsWith("Promise<resolved:"));
    }

    // =========================================================================
    // Callback pattern: done / error
    // =========================================================================

    @Test
    void infer_promise_then_method_type() {
        /*
         * let p = async 42
         * p.then  :  (Integer -> b) -> Unit
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        MemberAccessor thenAccess = new MemberAccessor(
                new Identifier(ast, new Token<>("p")),
                new Identifier(ast, new Token<>("then")));
        ast.addStatement(ASTHelper.varLet(ast, "then_fn", thenAccess));

        asf.infer();

        assertTrue(ast.errors().isEmpty(), "Expected no errors, got: " + ast.errors());
        Outline thenType = ((VariableDeclarator) ast.program().body().statements().get(1))
                .assignments().getFirst().lhs().outline();
        assertInstanceOf(FirstOrderFunction.class, thenType,
                "then should be a function, got: " + thenType);
    }

    @Test
    void infer_promise_catch_method_type() {
        /*
         * let p = async 42
         * p.catch  :  (String -> b) -> Unit
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        MemberAccessor catchAccess = new MemberAccessor(
                new Identifier(ast, new Token<>("p")),
                new Identifier(ast, new Token<>("catch")));
        ast.addStatement(ASTHelper.varLet(ast, "catch_fn", catchAccess));

        asf.infer();

        assertTrue(ast.errors().isEmpty(), "Expected no errors, got: " + ast.errors());
        Outline catchType = ((VariableDeclarator) ast.program().body().statements().get(1))
                .assignments().getFirst().lhs().outline();
        assertInstanceOf(FirstOrderFunction.class, catchType,
                "catch should be a function, got: " + catchType);
    }

    @Test
    void interpret_then_catch_methods_are_callable() {
        /*
         * At runtime, p.then and p.catch should evaluate to FunctionValues.
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(42)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        asf.infer();
        OutlineInterpreter interp = new OutlineInterpreter(asf);
        interp.runAst(ast);

        PromiseValue pv = (PromiseValue) interp.currentEnv().lookup("p");
        Value thenMethod  = BuiltinMethods.promise(pv, "then",  interp);
        Value catchMethod = BuiltinMethods.promise(pv, "catch", interp);

        assertInstanceOf(FunctionValue.class, thenMethod,  "then should be a FunctionValue");
        assertInstanceOf(FunctionValue.class, catchMethod, "catch should be a FunctionValue");
    }

    @Test
    void interpret_then_callback_receives_resolved_value() throws Exception {
        /*
         * Registering a 'then' callback delivers the resolved value to the callback.
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(99)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        OutlineInterpreter interp = run(ast);
        PromiseValue pv = (PromiseValue) get(interp, "p");

        CompletableFuture<Value> received = new CompletableFuture<>();
        Value thenMethod = BuiltinMethods.promise(pv, "then", interp);
        interp.apply(thenMethod, new FunctionValue(v -> {
            received.complete(v);
            return UnitValue.INSTANCE;
        }));

        Value result = received.get(2, TimeUnit.SECONDS);
        assertEquals(new IntValue(99L), result);
    }

    @Test
    void interpret_catch_callback_receives_error_message() throws Exception {
        /*
         * A failed future delivers the error message to the 'catch' callback.
         */
        CompletableFuture<Value> failedFuture = CompletableFuture.failedFuture(
                new RuntimeException("something went wrong"));
        PromiseValue pv = new PromiseValue(failedFuture);

        ASF asf = new ASF();
        AST ast = asf.newAST();
        OutlineInterpreter interp = new OutlineInterpreter(asf);

        CompletableFuture<Value> received = new CompletableFuture<>();
        Value catchMethod = BuiltinMethods.promise(pv, "catch", interp);
        interp.apply(catchMethod, new FunctionValue(v -> {
            received.complete(v);
            return UnitValue.INSTANCE;
        }));

        Value result = received.get(2, TimeUnit.SECONDS);
        assertInstanceOf(StringValue.class, result);
        assertTrue(((StringValue) result).value().contains("something went wrong"));
    }

    @Test
    void interpret_then_not_called_when_promise_fails() throws Exception {
        /*
         * A failed future must NOT invoke the 'then' callback.
         */
        CompletableFuture<Value> failedFuture = CompletableFuture.failedFuture(
                new RuntimeException("failure"));
        PromiseValue pv = new PromiseValue(failedFuture);

        ASF asf = new ASF();
        AST ast = asf.newAST();
        OutlineInterpreter interp = new OutlineInterpreter(asf);

        CopyOnWriteArrayList<Value> thenCalls   = new CopyOnWriteArrayList<>();
        CompletableFuture<Value> catchReceived  = new CompletableFuture<>();

        Value thenMethod  = BuiltinMethods.promise(pv, "then",  interp);
        Value catchMethod = BuiltinMethods.promise(pv, "catch", interp);

        interp.apply(thenMethod,  new FunctionValue(v -> { thenCalls.add(v); return UnitValue.INSTANCE; }));
        interp.apply(catchMethod, new FunctionValue(v -> { catchReceived.complete(v); return UnitValue.INSTANCE; }));

        catchReceived.get(2, TimeUnit.SECONDS);
        Thread.sleep(20);
        assertTrue(thenCalls.isEmpty(), "then callback must not be called when the promise fails");
    }

    @Test
    void interpret_catch_not_called_on_success() throws Exception {
        /*
         * A successful future must invoke 'then' and must NOT invoke 'catch'.
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        AsyncNode asyncNode = new AsyncNode(ast, LiteralNode.parse(ast, new Token<>(7)));
        ast.addStatement(ASTHelper.varLet(ast, "p", asyncNode));

        OutlineInterpreter interp = run(ast);
        PromiseValue pv = (PromiseValue) get(interp, "p");

        CompletableFuture<Value> thenReceived  = new CompletableFuture<>();
        CopyOnWriteArrayList<Value> catchCalls = new CopyOnWriteArrayList<>();

        Value thenMethod  = BuiltinMethods.promise(pv, "then",  interp);
        Value catchMethod = BuiltinMethods.promise(pv, "catch", interp);

        interp.apply(thenMethod,  new FunctionValue(v -> { thenReceived.complete(v); return UnitValue.INSTANCE; }));
        interp.apply(catchMethod, new FunctionValue(v -> { catchCalls.add(v); return UnitValue.INSTANCE; }));

        Value result = thenReceived.get(2, TimeUnit.SECONDS);
        Thread.sleep(20);

        assertEquals(new IntValue(7L), result);
        assertTrue(catchCalls.isEmpty(), "catch callback must not be called on success");
    }
}
