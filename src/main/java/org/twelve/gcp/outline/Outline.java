package org.twelve.gcp.outline;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.*;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.projectable.Projectable;
import org.twelve.gcp.outline.projectable.Reference;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.twelve.gcp.common.Tool.cast;

/**
 * Root interface of the GCP type system, representing a type "outline".
 *
 * <h2>Type Relation Semantics</h2>
 * GCP uses structural typing. Type relationships are expressed through {@code is} / {@code canBe}:
 * <ul>
 *   <li><b>is(another)</b>: Duck-typing subtype relation ("I am you").
 *       {@code this.is(another)==true} means {@code this} can be used wherever {@code another} is expected.
 *       Used for function argument matching: if {@code y.is(x)==true}, calling {@code f(x)} with {@code f(y)} is valid.</li>
 *   <li><b>canBe(another)</b>: Assignment compatibility ("I can become you").
 *       {@code b.canBe(a)==true} means the assignment {@code a = b} is legal.
 *       {@code is} implies {@code canBe}, but not vice versa (Poly types relax the condition).</li>
 *   <li><b>maybe(another)</b>: Weaker form of {@code is} that only checks basic structure,
 *       ignoring extended attributes. For non-ProductADT types, {@code maybe == is}.</li>
 * </ul>
 *
 * <h2>Type Hierarchy</h2>
 * <pre>
 *   Outline
 *   ├── Primitive  (ANY, NOTHING, STRING, NUMBER, ...)
 *   ├── ADT        (Entity, Tuple, Array, Poly, Option, ...)
 *   ├── Projectable (Genericable, Function, ...)
 *   └── BuildInOutline (UNKNOWN, UNIT, ERROR, ...)
 * </pre>
 *
 * <h2>Projection Mechanism</h2>
 * Projection is the core mechanism for generic instantiation in GCP:
 * it substitutes type variables with concrete types.
 * {@link #project(Reference, OutlineWrapper)} replaces a reference type with its actual type.
 *
 * @author huizi 2025
 */
public interface Outline extends Serializable {

    /**
     * Thread-local set of "{id_a}:{id_b}" pairs currently being compared via {@link #is}.
     * Prevents infinite recursion when two structurally equivalent types reference each other
     * through their built-in method signatures (e.g. {@code Number.min: Number → Number}).
     */
    ThreadLocal<Set<String>> IS_COMPARING = ThreadLocal.withInitial(HashSet::new);

    AST ast();

    Node node();

    default String name() {
        String name = this.getClass().getSimpleName();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    /**
     * Structural equality: bidirectional {@code is} relation with no uninstantiated generic variables.
     * Note: this is type-semantic equality, not Java's {@link Object#equals}.
     */
    default boolean equals(Outline another) {
        return this.is(another) && another.is(this) && !(another instanceof Projectable && ((Projectable) another).containsGeneric());
    }

    /**
     * Weaker form of {@link #is}: checks only the basic structure, ignoring extended attributes.
     * For non-ProductADT types, {@code maybe == is}.
     *
     * @param another the target type
     * @return whether this type structurally may match the target
     */
    default boolean maybe(Outline another) {
        return this.is(another);
    }

    /**
     * Duck-typing subtype relation ("I am you").
     * <p>
     * {@code this.is(another)==true} means {@code this} is a subtype of {@code another}
     * and can be used in any position that expects {@code another}.
     * Implemented via bidirectional delegation: first tries {@code this.tryIamYou(another)},
     * then {@code another.tryYouAreMe(this)}, allowing each side to define its own protocol.
     * <p>
     * The identity short-circuit ({@code this == another → true}) is essential for correctness
     * when built-in methods reference singleton types (e.g. {@code ast.Number}).
     * Without it, comparing two numeric types whose {@code min}/{@code max} members both reference
     * {@code ast.Number} would recurse infinitely through structural member comparison.
     *
     * @param another the target type
     * @return whether this type is a subtype of the target
     */
    default boolean is(Outline another) {
        if (this == another) return true;
        // Cycle detection: when structural member comparison recurses back to the same
        // (this, another) pair (e.g. Number.min: Number→Number causes Number.is(Number)
        // to recurse), break the cycle by assuming compatibility for that pair.
        // Key is ASYMMETRIC (this.id:another.id) so that A.is(B) and B.is(A) are tracked
        // independently; only a genuine re-entry of the exact same directed call is treated
        // as a cycle, preventing false positives that would suppress real type errors.
        String key = this.id() + ":" + another.id();
        Set<String> guard = IS_COMPARING.get();
        if (!guard.add(key)) return true;
        try {
            return this.tryIamYou(another) || another.tryYouAreMe(this);
        } finally {
            guard.remove(key);
        }
    }

    /**
     * Assignment compatibility ("I can become you").
     * <p>
     * {@code b.canBe(a)==true} means the assignment {@code a = b} is legal.
     * {@link #is} implies {@code canBe}, but not vice versa.
     * For Poly (union) types, {@code canBe} is more permissive: it only requires
     * this type to be one of the union's options, not a full structural match.
     *
     * @param another the assignment target type
     * @return whether this type is assignable to the target
     */
    default boolean canBe(Outline another) {
        return this.tryIamYou(another) || another.tryYouCanBeMe(this);
    }

    /** Active subtype check initiated by {@code this}. Subclasses override as needed. */
    default boolean tryIamYou(Outline another) {
        return false;
    }

    /** Passive subtype check from {@code another}'s perspective. Subclasses override as needed. */
    default boolean tryYouAreMe(Outline another) {
        return false;
    }

    /** Passive assignment-compatibility check. Defaults to {@link #tryYouAreMe}; subclasses may override for looser semantics. */
    default boolean tryYouCanBeMe(Outline another) {
        return this.tryYouAreMe(another);
    }

    /**
     * Shallow copy. Returns {@code this} by default (immutable types need no copy).
     * Mutable types such as {@link org.twelve.gcp.outline.projectable.Genericable} must override this.
     */
    default <T extends Outline> T copy() {
        return cast(this);
    }

    /**
     * Cache-aware deep copy that prevents infinite recursion from circular references.
     *
     * @param cache map of already-copied objects (key=original, value=copy)
     */
    default Outline copy(Map<Outline, Outline> cache) {
        Outline copied = cast(cache.get(this));
        if (copied == null) {
            copied = this.copy();
            cache.put(this, copied);
        }
        return copied;
    }

    default boolean beAssignedAble() {
        return true;
    }

    /** Returns {@code true} if this type has been fully inferred (i.e. not UNKNOWN). */
    default boolean inferred() {
        return !(this instanceof UNKNOWN);
    }

    long id();

    /**
     * Substitutes one {@link Reference} (type parameter) with a concrete type — the fundamental
     * operation of generic instantiation. The default returns {@code this} for non-generic types.
     *
     * @param reference  the type parameter to replace
     * @param projection the concrete type to substitute (with its AST node)
     * @return the resulting type after substitution
     */
    default Outline project(Reference reference, OutlineWrapper projection) {
        return this;
    }

    /**
     * Forces evaluation of {@link org.twelve.gcp.outline.decorators.Lazy}-wrapped types
     * and returns the final resolved type. Non-lazy types return {@code this}.
     */
    default Outline eventual() {
        return this;
    }

    /**
     * Expands a generic type containing {@link org.twelve.gcp.outline.projectable.Reference}
     * after a concrete value has been assigned. Non-generic types return {@code this}.
     */
    default Outline instantiate() {
        return this;
    }

    default boolean containsUnknown() {
        return false;
    }

    default boolean containsIgnore() {
        return false;
    }

    default boolean beAssignable() {
        return true;
    }

    default Outline alternative() {
        return this;
    }

    default String type(){
        return this.getClass().getSimpleName();
    }

    default boolean containsReference(){return false;};
    default boolean containsLazyAble(){return false;};

    /**
     * Merges the constraints of another outline of the same kind into this one.
     * Used for constraint convergence across multiple inference paths.
     * The default returns {@code outline} directly (unconstrained types are simply overwritten).
     */
    default Outline melt(Outline outline){return outline;};

    /**
     * Called when the host {@link ProductADT} has been resolved to a concrete type,
     * so that any internal {@code ~this} self-reference can be bound lazily.
     */
    default void updateThis(ProductADT me){

    }

}
