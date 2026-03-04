package org.twelve.gcp.outline.decorators;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Module;

/**
 * A lazy placeholder for a module symbol that has not been exported yet.
 * <p>
 * When two modules mutually import from each other (A ↔ B), one of them
 * will always be processed before the other's symbols are available.  Instead
 * of failing immediately, {@link org.twelve.gcp.inference.ImportInference}
 * creates a {@code LazyModuleSymbol} that stores a reference to the source
 * {@link Module} and the symbol name.  The placeholder is treated as
 * "not yet inferred" (its {@link #inferred()} returns {@code false}) so the
 * {@code AbstractNode.fullyInferred} cache is never set, which forces every
 * subsequent inference round to re-evaluate the import until the symbol
 * actually appears in the module.
 * <p>
 * Once the source module exports the symbol, all type operations on this
 * placeholder transparently delegate to the concrete type via
 * {@link #eventual()}, making the lazy resolution invisible to the rest of
 * the type system.
 */
public class LazyModuleSymbol implements Outline {

    private final Module module;
    private final Identifier symbol;

    public LazyModuleSymbol(Module module, Identifier symbol) {
        this.module = module;
        this.symbol = symbol;
    }

    // ── resolution ──────────────────────────────────────────────────────────

    /** Returns the concrete outline once the source module has exported it, {@code null} otherwise. */
    public Outline resolve() {
        return module.getSymbol(symbol.name());
    }

    /**
     * Returns the resolved type if available, or {@code this} if the symbol
     * has not been exported yet.  Callers that receive {@code this} back can
     * detect an unresolved state by checking {@link #inferred()}.
     */
    @Override
    public Outline eventual() {
        Outline resolved = resolve();
        return resolved != null ? resolved : this;
    }

    // ── Outline contract ────────────────────────────────────────────────────

    /**
     * Always {@code false}.
     * <p>
     * A {@code LazyModuleSymbol} is a placeholder: it is never itself "inferred".
     * Only when {@link org.twelve.gcp.inference.ImportInference} replaces it with
     * the concrete type (via {@link org.twelve.gcp.outlineenv.EnvSymbol#update}) and
     * {@link org.twelve.gcp.inference.ImportSpecifierInference} picks up that concrete
     * type does the owning {@link org.twelve.gcp.node.imexport.ImportSpecifier} become
     * truly inferred.
     * <p>
     * Returning {@code false} here guarantees that
     * {@link org.twelve.gcp.node.imexport.ImportExportSpecifier#inferred()} stays
     * {@code false} as long as this placeholder is still in play, which prevents
     * {@link org.twelve.gcp.ast.AbstractNode#infer} from short-circuiting the import
     * re-evaluation via its {@code fullyInferred} cache.
     */
    @Override
    public boolean inferred() {
        return false;
    }

    /**
     * Always {@code true}: while this placeholder exists the containing node
     * is still waiting for a lazy resolution.
     */
    @Override
    public boolean containsLazyAble() {
        return true;
    }

    // ── subtype / assignment delegation ─────────────────────────────────────

    @Override
    public boolean is(Outline another) {
        Outline resolved = resolve();
        if (resolved == null) return false;
        return resolved.is(another);
    }

    @Override
    public boolean tryIamYou(Outline another) {
        Outline resolved = resolve();
        if (resolved == null) return false;
        return resolved.tryIamYou(another);
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        Outline resolved = resolve();
        if (resolved == null) return false;
        return resolved.tryYouAreMe(another);
    }

    @Override
    public boolean tryYouCanBeMe(Outline another) {
        Outline resolved = resolve();
        if (resolved == null) return false;
        return resolved.tryYouCanBeMe(another);
    }

    @Override
    public boolean canBe(Outline another) {
        Outline resolved = resolve();
        if (resolved == null) return false;
        return resolved.canBe(another);
    }

    @Override
    public boolean maybe(Outline another) {
        Outline resolved = resolve();
        if (resolved == null) return false;
        return resolved.maybe(another);
    }

    // ── identity ─────────────────────────────────────────────────────────────

    /** Uses the identifier's id so the cycle-detection guard in {@link Outline#is} works. */
    @Override
    public long id() {
        return symbol.id();
    }

    @Override
    public AST ast() {
        return symbol.ast();
    }

    @Override
    public AbstractNode node() {
        return null;
    }

    @Override
    public String name() {
        Outline resolved = resolve();
        return resolved != null ? resolved.name() : symbol.name();
    }

    // ── display ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        Outline resolved = resolve();
        return resolved != null ? resolved.toString() : "Lazy{" + symbol.name() + "}";
    }
}
