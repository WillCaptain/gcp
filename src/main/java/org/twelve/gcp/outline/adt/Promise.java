package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.Modifier;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Promise_;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

/**
 * Promise&lt;T&gt; – the type produced by an {@code async} expression.
 *
 * <p>Modelled after JavaScript's {@code Promise<T>} / Rust's {@code Future<Output=T>}.
 * Extends {@link ProductADT} so that its two built-in callback methods are reachable
 * through the standard member-access inference path.
 *
 * <h2>Structure (GCP type notation)</h2>
 * <pre>
 *   Promise&lt;T&gt; = {
 *     then  : (T      -&gt; b) -&gt; Unit   // success callback  (mirrors JS Promise.then)
 *     catch : (String -&gt; b) -&gt; Unit   // failure callback  (mirrors JS Promise.catch)
 *   }
 * </pre>
 *
 * <h2>Subtyping</h2>
 * {@code Promise<A>.is(Promise<B>)} iff {@code A.is(B)}.
 */
public class Promise extends ProductADT {

    private final Node node;
    private final Outline innerOutline;

    private Promise(Node node, AST ast, Outline innerOutline) {
        super(ast, Promise_.instance());
        this.node = node;
        this.innerOutline = innerOutline;
    }

    public static Promise from(Node node, Outline innerOutline) {
        return new Promise(node, node.ast(), innerOutline);
    }

    public static Promise from(AST ast, Outline innerOutline) {
        return new Promise(null, ast, innerOutline);
    }

    /** Returns the type that the async computation will eventually produce. */
    public Outline innerOutline() {
        return innerOutline;
    }

    // ── Outline interface ─────────────────────────────────────────────────────

    @Override
    public Node node() {
        return node;
    }

    /**
     * Subtype rule: {@code Promise<A>.is(Promise<B>)} iff {@code A.is(B)}.
     * We override rather than relying on member-level matching so that correctness
     * is preserved even before {@link #loadBuiltInMethods()} has been called.
     */
    @Override
    public boolean tryIamYou(Outline another) {
        if (!(another instanceof Promise)) return false;
        return this.innerOutline.is(((Promise) another).innerOutline);
    }

    // ── Built-in methods ──────────────────────────────────────────────────────

    /**
     * Lazily registers the two built-in callback methods once per instance.
     *
     * <p>The callback return type is a free reference variable ({@code b}) so that any
     * user-provided callback is accepted (the return value is ignored at runtime).
     * This mirrors how {@code Array.each} is defined.
     *
     * <pre>
     *   then  : (T      -&gt; b) -&gt; Unit   // success callback  (mirrors JS Promise.then)
     *   catch : (String -&gt; b) -&gt; Unit   // failure callback  (mirrors JS Promise.catch)
     * </pre>
     */
    @Override
    public boolean loadBuiltInMethods() {
        if (!super.loadBuiltInMethods()) return false;
        AST ast = this.ast();

        // then : (T -> b) -> Unit
        members.put("then", buildCallbackMethod(ast, "then_b", innerOutline));

        // catch : (String -> b) -> Unit
        members.put("catch", buildCallbackMethod(ast, "catch_b", ast.String));

        return true;
    }

    /**
     * Builds a member of the form {@code (ArgType -> b) -> Unit} where {@code b} is a free
     * reference variable (the callback's return type is unconstrained and ignored).
     *
     * @param refName unique name for the free reference variable (must be unique per AST)
     * @param argType the type of the value passed into the callback
     */
    private EntityMember buildCallbackMethod(AST ast, String refName, Outline argType) {
        Identifier refId = new Identifier(ast, new Token<>(refName));
        Reference callbackRet = Reference.from(new ReferenceNode(refId, null), null);
        FirstOrderFunction callback = FirstOrderFunction.from(ast, callbackRet, argType);
        Generic arg = cast(Generic.from(ast, callback));
        List<Reference> refs = new ArrayList<>();
        refs.add(callbackRet);
        Returnable ret = Return.from(ast);
        ret.addReturn(ast.Unit);
        return EntityMember.from(refName.split("_")[0],
                FirstOrderFunction.from(ast, arg, ret, refs),
                Modifier.PUBLIC, false, null, true);
    }

    // ── Generic projection ────────────────────────────────────────────────────

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        Outline projected = innerOutline.project(reference, projection);
        return new Promise(node, ast(), projected);
    }

    // ── Inference helpers ─────────────────────────────────────────────────────

    @Override
    public boolean inferred() {
        return innerOutline.inferred();
    }

    @Override
    public boolean containsUnknown() {
        return innerOutline.containsUnknown();
    }

    @Override
    public boolean containsIgnore() {
        return innerOutline.containsIgnore();
    }

    // ── Copying ───────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Promise copy() {
        return new Promise(node, ast(), innerOutline);
    }

    @Override
    public Promise copy(Map<Outline, Outline> cache) {
        Promise copied = cast(cache.get(this));
        if (copied == null) {
            copied = new Promise(node, ast(), innerOutline.copy(cache));
            cache.put(this, copied);
        }
        return copied;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Promise<" + innerOutline + ">";
    }
}
