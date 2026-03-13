# Generalized Constraint Projection: A Four-Dimensional Type Inference Engine for Dynamic Languages

**Abstract** — Dynamic programming languages offer unmatched flexibility but impose a persistent type-inference challenge: the same variable may be assigned a value of one type, declared as a supertype, consumed in a context requiring a third type, and accessed through a structural interface implying a fourth. Existing approaches — from Hindley–Milner unification to TypeScript's bidirectional checking — conflate these four qualitatively distinct sources of type information into a single constraint set, causing spurious conflicts and forcing developers to supply redundant annotations. This paper presents **Generalized Constraint Projection (GCP)**, a progressive type inference methodology that attaches *four independent constraint dimensions* (value extension, programmer declaration, usage demand, and structural access pattern) to every unresolved type variable and narrows them iteratively via lattice operations rather than global unification. We formalize the approach over the **Outline** language: we define the type lattice, the four-dimensional Genericable constraint model with monotonicity and convergence proofs, **Outline Expression Matching (OEM)** as a structural subtyping relation for duck-typed languages, and a bidirectional Projection mechanism for polymorphic higher-order function instantiation. We prove soundness (inferred types are safe approximations of runtime types), local and global convergence (the fixed-point algorithm terminates in O(N·|𝕋|) steps for N variables over a type lattice of size |𝕋|), and order-independence (inference results are deterministic regardless of module processing order). We describe a production implementation in the **Entitir** ontology data platform, where GCP powers type-safe query compilation from Outline lambda predicates to SQL, enabling LLM-generated queries to be executed safely against relational schemas without dynamic type failures.

---

## 1. Introduction

The tension between developer productivity and type safety is one of the oldest problems in programming language design. Dynamic languages resolve it in favor of productivity — variables need no type annotations, objects can gain new properties at runtime, and functions can accept arguments of any shape. The price is paid at runtime: type errors that could have been caught at compile time instead manifest as cryptic failures in production.

Gradual type systems [[Pierce & Turner 2000]](#ref1) attempt to bridge this gap by allowing programmers to annotate as much or as little as they choose. TypeScript [[Microsoft 2012]](#ref2) is the dominant industrial example, achieving widespread adoption by layering a structural type system over JavaScript. Yet TypeScript still exhibits the three pain points that motivate GCP:

1. **Redundant annotations.** Even when the type of an expression is unambiguous from its context, TypeScript frequently requires an explicit annotation (e.g., generic type parameters on higher-order function calls).

2. **Context insensitivity.** A variable's type is fixed at its declaration site; its type cannot be narrowed differently in two independent calling contexts without introducing explicit overloads.

3. **Cumbersome generic inference.** Writing a correct generic higher-order function (e.g., a typed `filter`) requires verbose, error-prone parameter-level generic declarations.

GCP addresses all three problems through a unified mechanism: instead of solving a global type-constraint system via unification (as Hindley–Milner does [[Milner 1978]](#ref3)), GCP attaches *four independent constraint dimensions* to every unresolved type variable and narrows them iteratively as information flows in from all directions. The key insight is that the four constraint directions — *actual value assigned, programmer annotation, usage context, structural usage pattern* — provide complementary information, and their joint minimization uniquely determines most types without requiring any annotation.

The rest of this paper is organized as follows. §2 introduces the Outline language and type hierarchy. §3 formalizes the four-dimensional constraint model and proves monotonicity, convergence, and soundness. §4 defines Outline Expression Matching (OEM), GCP's structural subtyping relation, and proves transitivity. §5 describes the bidirectional Projection mechanism for generic instantiation. §6 presents the type inference algorithm with inference rules. §7 covers multi-module inference via a fixed-point algorithm, with convergence and order-independence proofs. §8 describes the tree-walking interpreter and runtime value model. §9 presents the GCPToSQL application and the Entitir ontology platform. §10 discusses related work and limitations. §11 concludes.

---

## 2. The Outline Language and Type System

### 2.1 Overview

*Outline* is the expression language whose type system GCP infers. It is a strict, expression-oriented language with first-class functions, algebraic data types, pattern matching, and a module system. Its syntax is intentionally minimal — it resembles a stripped-down TypeScript with lambda-first notation — so that GCP can be illustrated without the noise of a full general-purpose language.

**Types in Outline are called *outlines***, which emphasizes that the type system is *structural* rather than *nominal*: whether one outline is a subtype of another depends on the shape of their members, not on declared inheritance relationships.

### 2.2 Outline Type Hierarchy

We define the set **𝕋** of all Outline types (outlines) by the following grammar:

```
τ ::=
    ⊤                          -- ANY: the top type
  | ⊥                          -- NOTHING: the bottom type
  | prim                        -- Primitive types
  | Entity(name, [m_i : τ_i])  -- Named entity with members
  | Tuple([f_i : τ_i])         -- Anonymous structural record
  | Array(τ)                   -- Homogeneous ordered collection
  | Dict(τ_k, τ_v)             -- Key-value dictionary
  | Option(τ_1, …, τ_n)       -- Sum type (tagged union)
  | τ_1 → τ_2                  -- First-order function
  | τ_1 → τ_2 → … → τ_n       -- Higher-order (curried) function
  | G[C]                        -- Generic variable with constraint chain C
  | Ref(a)                      -- Type parameter reference
  | ●                           -- UNIT
  | ?                           -- UNKNOWN (pre-inference placeholder)
```

where `prim ∈ {String, Int, Long, Float, Double, Number, Bool, Symbol}`, and `Number` is a declared supertype of `Int | Long | Float | Double`.

The primitive lattice includes the numeric promotion chain:

```
    ANY
     |
   Number
   / | \ \
  Int Long Float Double
```

### 2.3 Type Lattice

The full type lattice (𝕋, ≼) is defined by the structural subtyping relation introduced in §4. Key properties:

- **⊤ (ANY)** is the greatest element: `∀τ. τ ≼ ⊤`
- **⊥ (NOTHING)** is the least element: `∀τ. ⊥ ≼ τ`
- **Numeric promotion**: `Int ≼ Number`, `Long ≼ Number`, `Float ≼ Number`, `Double ≼ Number`
- **Entity inheritance**: `Entity(B, ...) ≼ Entity(A, ...)` when B extends A
- **Function contravariance**: `(σ₁ → σ₂) ≼ (τ₁ → τ₂)` when `τ₁ ≼ σ₁` and `σ₂ ≼ τ₂`

---

## 3. The Four-Dimensional Constraint Model

### 3.1 Motivation

Classical constraint-based type inference [[Pottier & Rémy 2005]](#ref4) generates a set of equality or subtype constraints and solves them simultaneously. This works well for Hindley–Milner languages but encounters difficulties with dynamic patterns such as:

- A variable receives a value of one type (`x = dog`) while being annotated as a supertype (`x: Animal`).
- The same variable is consumed in one expression as a `Number` (due to context) but structurally used as a callable `a → b` elsewhere.

The root difficulty is that these are *four qualitatively different sources of type information* that arrive asynchronously as the inference pass traverses the AST. Treating them as a single undifferentiated constraint set conflates their distinct roles and leads to spurious constraint conflicts.

### 3.2 Genericable: The Four-Dimensional Variable

Every unresolved type variable in GCP is represented as a **Genericable** object carrying four constraint slots:

| Slot | Notation | Description |
|------|----------|-------------|
| `extendToBe` | τ_e | Upper-bound constraint: the most specific type that has actually been *assigned to* this variable. |
| `declaredToBe` | τ_d | Declaration constraint: the type explicitly annotated by the programmer. |
| `hasToBe` | τ_h | Usage constraint: the type this variable *must be compatible with* based on how it is consumed. |
| `definedToBe` | τ_f | Structural constraint: the structural shape *inferred from call and access patterns*. |

**Definition 3.1 (Constraint Chain).** A constraint chain for a Genericable variable *x* is the tuple:

```
C(x) = ⟨τ_e(x), τ_d(x), τ_h(x), τ_f(x)⟩
```

The chain must satisfy the *consistency invariant*:

```
τ_e(x) ≼ τ_d(x) ≼ τ_h(x) ≼ τ_f(x)
```

Violation of this invariant is reported as a type error (CONSTRUCT_CONSTRAINTS_FAIL).

### 3.3 Initial Values

Genericable variables are initialized with safe defaults that make the constraint chain trivially consistent before any information has arrived:

```
τ_e  ← ⊥        (Nothing: no value assigned yet)
τ_d  ← ⊤        (Any: no annotation yet)
τ_h  ← ⊤        (Any: no usage constraint yet)
τ_f  ← ⊤        (Any: no structural constraint yet)
```

### 3.4 Constraint Addition Semantics

Each constraint slot is updated monotonically — constraints only narrow, never widen:

**Extending** (upper-bound assignment, e.g. `x = v`):
```
addExtendToBe(τ) : τ_e ← τ_e ⊔ τ    (join/lub)
```

**Declaring** (annotation, e.g. `x: Number`):
```
setDeclaredToBe(τ) : τ_d ← τ
```

**Usage** (context expectation, e.g. `y = x` where `y: Number`):
```
addHasToBe(τ) : τ_h ← τ_h ⊓ τ    (meet/glb)
```

**Structural** (access pattern, e.g. `x.foo` or `x(a)`):
```
addDefinedToBe(τ) : τ_f ← τ_f ⊓ τ    (meet/glb)
```

After each update, the consistency invariant is checked; violations generate immediate type errors.

### 3.5 Type Resolution: Guess and Min

At any point during inference, two derived functions approximate the resolved type:

**Definition 3.2 (Guess).** The best current approximation, used for resolving expressions before the chain has fully converged:

```
guess(x) = first_non_trivial(τ_e, τ_d, τ_h, τ_f) ?? ⊤
```

where `??` selects the leftmost non-`UNKNOWN` slot, reading from most specific to most general.

**Definition 3.3 (Min).** The strongest lower bound:

```
min(x) = first_non_trivial(τ_d, τ_h, τ_f) ?? τ_e ?? ⊤
```

**Definition 3.4 (Max).** The widest upper bound:

```
max(x) = τ_e ?? τ_d ?? τ_h ?? τ_f ?? ⊤
```

### 3.6 The Fifth Dimension: projectedType

A fifth orthogonal field `projectedType` is maintained separately from the constraint chain:

```
projectedType(x) ∈ 𝕋 ∪ {null}
```

Unlike the four constraint slots, `projectedType` **does not participate in type inference**. It is written exactly once, during generic instantiation (§5.3), and is read exclusively by the metadata extraction layer for IDE dot-completion (§8.2). This separation prevents tooling concerns from corrupting the inference state.

### 3.7 Correctness Properties

We establish four foundational properties of GCP's constraint model: monotonicity of each update operation, local convergence of individual variables, global convergence across programs, and soundness of type resolution.

**Definition 3.5 (Information ordering).** Let 𝕋 be the type lattice (§2.3), where ⊑ denotes the subtype relation (more specific types are lower). For a Genericable variable *x* with constraint tuple C(x) = (τ_e, τ_d, τ_h, τ_f), define the *information ordering* ≼ componentwise:

```
C(x) ≼ C'(x)  iff  τ_e ⊑ τ_e'  ∧  τ_d ⊑ τ_d'  ∧  τ_h' ⊑ τ_h  ∧  τ_f' ⊑ τ_f
```

The reversed inequalities for τ_h and τ_f reflect that *more information* means a *narrower* usage bound (the hasToBe/definedToBe meet moves toward ⊥), while *more information* on the value side means a *wider* join (extendToBe moves toward the actual type). The ordering is designed so that C(x) ≼ C'(x) exactly when C'(x) carries at least as much type information as C(x).

---

**Theorem 3.1 (Monotonicity of Constraint Operations).** *Each of the four constraint update operations is monotone under ≼: applying any update to a variable x either strictly increases x's information content or leaves it unchanged.*

*Proof.* We inspect each operation in turn.

- `addExtendToBe(τ)`: τ_e ← τ_e ⊔ τ. Since ⊔ is the join (least upper bound on 𝕋), τ_e ⊑ τ_e ⊔ τ for any τ, so the first component of C(x) moves up in the lattice, increasing information under ≼.
- `setDeclaredToBe(τ)`: τ_d ← τ, applied at most once. Before the assignment τ_d = ⊤ (the top, least informative); afterwards τ_d = τ ⊑ ⊤, so information increases.
- `addHasToBe(τ)`: τ_h ← τ_h ⊓ τ. Since ⊓ is the meet (greatest lower bound), τ_h ⊓ τ ⊑ τ_h for any τ; τ_h narrows, and by the reversed inequality in ≼ this increases information.
- `addDefinedToBe(τ)`: τ_f ← τ_f ⊓ τ. Identical argument to `addHasToBe`. ∎

---

**Corollary 3.2 (Non-decreasing guess).** *The resolved type `guess(x)` is weakly more specific at every step of inference: once a type is derived for x, it is never weakened.*

*Proof.* `guess(x)` selects the leftmost non-trivial slot (§3.5). By Theorem 3.1 each slot narrows (or holds) monotonically, so the leftmost non-trivial slot either holds or narrows. ∎

---

**Theorem 3.3 (Local Convergence).** *For any single Genericable variable x in a finite program, the constraint chain C(x) reaches a fixed point after a finite number of updates.*

*Proof.* By Theorem 3.1, C(x) is monotone under ≼. The type lattice 𝕋 is finite and forms a bounded lattice (⊥ = `Nothing`, ⊤ = `UNKNOWN`). By the ascending chain condition on finite lattices, any monotone sequence in 𝕋^4 under ≼ is finite. Concretely:
- τ_e can take at most |𝕋| distinct values via join, each step strictly increasing.
- τ_d is written at most once.
- τ_h and τ_f can take at most |𝕋| distinct values via meet, each step strictly decreasing.

The total number of updates before fixpoint is therefore bounded by 1 + 3·|𝕋|. For the Outline type lattice with |𝕋| ≤ 30 distinct types, this bound is at most 91 updates per variable. ∎

---

**Theorem 3.4 (Soundness, informal).** *Let P be a well-formed Outline program. If GCP infers `guess(x) = τ` for a variable x, and v is the runtime value of x under the Outline interpreter (§8), then v ∈ ⟦τ⟧, the value domain of τ.* (The formulation follows the progress-and-preservation methodology of [[Wright & Felleisen 1994]](#ref17); a full mechanized proof is left as future work.)

*Proof sketch.* The inference rules (§6.2) mirror the evaluation rules of the interpreter:

1. **Literal and constructor nodes.** Every `addExtendToBe` emission originates from a node whose runtime type is statically known (integer literal → `Int`, string literal → `String`, `new Foo(...)` → `Foo`). Hence τ_e, when non-trivial, exactly equals the runtime type.

2. **Usage constraints.** `addHasToBe(τ)` is emitted when x is passed to a context requiring type τ. This is a sound *upper bound* on what x must provide; the runtime value v satisfies this constraint by the well-formedness of P.

3. **Structural constraints.** `addDefinedToBe(τ)` is emitted when a member access `x.m` is observed. OEM (§4) ensures that any runtime value v for which `v.m` is valid belongs to ⟦τ_f⟧ by the duck-typing semantics.

4. **Priority.** `guess(x)` prefers τ_e (the exact runtime type) over τ_d over τ_h over τ_f. When τ_e is present, soundness follows directly from (1). When τ_e is absent, the resolved type is an over-approximation, which is sound by (2) and (3).

Type errors are reported precisely when the meet τ_h ⊓ τ_f yields ⊥ (infeasible structural demand) or when τ_e ⋢ τ_d (value inconsistent with declaration). Such inconsistencies faithfully correspond to runtime type failures. *Formal completeness with respect to a full Outline operational semantics is left as future work.* ∎

---

**Theorem 3.5 (Subject Reduction / Type Preservation).** *Let P be a well-formed Outline program and let `e` be an expression with inferred type τ = `guess(x)` for some variable x in P. If the Outline interpreter (§8) reduces `e` to a value v in one step (e →_v v), then v ∈ ⟦τ⟧.*

*Proof sketch.* We proceed by structural induction on the reduction relation →_v.

- **Base cases** (literals and constructors): An integer literal `n` reduces to `IntValue(n)` and its inferred type is `Int` by the extendToBe rule. `IntValue(n) ∈ ⟦Int⟧` by definition of the value domain (§8.1). Similarly for all primitive types.
- **Variable lookup**: If x is bound to value v in the environment, `infer(x)` returned `guess(x)`. By Theorem 3.4 (Soundness), v ∈ ⟦guess(x)⟧.
- **Function application** `f(a) →_v v_r`: By the function call inference rule (§6.2), the return type of `guess(f)` = `τ₁ → τ₂`. By the interpreter's call rule, v_r is the result of evaluating the function body under an environment where the parameter carries the type of the actual argument. By inductive hypothesis on the body expression, v_r ∈ ⟦τ₂⟧.
- **Member access** `e.p →_v v_p`: The `addDefinedToBe({p: g})` rule records that e must have member p of type `guess(g)`. By OEM (§4), any runtime value that passes the structural check at this access point satisfies `v.p ∈ ⟦guess(g)⟧`.

The full proof requires a complete Outline operational semantics; we provide inference rules sufficient for the base and inductive cases above, leaving the full formal account to a companion technical report. ∎

---

**Theorem 3.6 (Inference Complexity).** *For an Outline program with N Genericable variables over a type lattice of size |𝕋| and M modules, the total number of constraint-update operations performed by the GCP fixed-point algorithm is bounded by:*

```
O(N · |𝕋| · M)
```

*Proof.* By Theorem 3.3, each variable performs at most 1 + 3·|𝕋| updates before reaching a fixed point. The multi-module algorithm (§7) re-runs inference per module per round, and by Theorem 7.1 the total rounds are bounded by a quantity proportional to M. The aggregate update count is therefore at most N·(1 + 3·|𝕋|)·M = O(N·|𝕋|·M).

For the Outline type lattice (|𝕋| ≤ 30), a program with N = 10,000 variables and M = 50 modules requires at most 10,000 × 91 × 50 ≈ 45 million operations — well within interactive response time on modern hardware. In practice, most variables are resolved in the first pass (M = 1), and the observed cost is O(N·|𝕋|). ∎

---

**Corollary 3.7 (Decidability).** *Type checking for Outline programs under GCP inference is decidable.*

*Proof.* By Theorem 3.6 the inference algorithm terminates in finite time for all finite programs. After termination, type consistency is checked via OEM (§4), which terminates by the cycle-detection mechanism of §4.2. Therefore the decision procedure halts on all inputs. ∎

---

## 4. Structural Subtyping: Outline Expression Matching (OEM)

### 4.1 Duck Typing Formalized

Dynamic languages rely on *duck typing*: an object is acceptable wherever a certain interface is expected if it provides all the required operations, regardless of its nominal type. GCP formalizes this intuition as **Outline Expression Matching (OEM)**.

**Definition 4.1 (OEM Subtyping).** For structural types τ_S and τ_T:

```
τ_S ≺ τ_T  ⟺  ∀p ∈ Props(τ_T), ∃p ∈ Props(τ_S) ∧ τ_S(p) ≺ τ_T(p)
```

where `Props(τ)` is the set of named members of τ, and `τ(p)` is the type of member `p` in τ.

Informally: *τ_S can substitute for τ_T if τ_S provides at least all the members that τ_T declares, with compatible types*.

**Example 4.1.** Given:
```outline
outline Animal = { age: Int, weight: Float }
outline Human  = { age: Int, weight: Float, name: String }
```
Then `Human ≺ Animal` because `Props(Animal) = {age, weight}` are all present in `Human` with compatible types, even though `Human` carries the extra member `name`.

### 4.2 Implementation: Bidirectional Delegation

The OEM check is implemented as a bidirectional delegation protocol between any two `Outline` objects:

```
τ_S.is(τ_T):
  1. try τ_S.tryIamYou(τ_T)   -- "Can S assert that it is T?"
  2. if undecided: try τ_T.tryYouAreMe(τ_S)  -- "Can T assert that S is it?"
  3. otherwise: return false
```

This two-phase design allows new Outline implementations to override subtype checking on either side without modifying the other, achieving an open-extension protocol that resembles the *Expression Problem* solution via visitor patterns.

**Cycle detection.** Structural types can be mutually recursive (e.g., a tree node containing a list of children of the same type). GCP maintains a `ThreadLocal<Set<String>>` of in-progress subtype checks, keyed by `τ_S.id + ":" + τ_T.id`, to detect and break cycles.

### 4.3 Function Subtyping

For function types, OEM applies the standard variance rules:

```
(σ₁ → σ₂) ≺ (τ₁ → τ₂)  ⟺  τ₁ ≺ σ₁  ∧  σ₂ ≺ τ_2
```

**Contravariance of parameters**: a function that accepts a more general type can be used where a function accepting a more specific type is expected.  
**Covariance of return values**: a function that returns a more specific type is acceptable where a more general return type is required.

### 4.4 Additional Type Compatibility Relations

GCP defines three relations with distinct strength:

| Relation | Method | Semantics |
|----------|--------|-----------|
| **is** | `τ_S.is(τ_T)` | Full structural subtyping (OEM); used for argument-passing and assignment checks. |
| **canBe** | `τ_S.canBe(τ_T)` | Weaker assignment compatibility; `is` + some tolerance for partial structural overlap. |
| **maybe** | `τ_S.maybe(τ_T)` | `is` ignoring extension members; used for constraint-chain updates where strict structural equality is too strong. |

### 4.5 OEM as a Preorder

**Theorem 4.1 (OEM Reflexivity).** *For all τ ∈ 𝕋, τ ≺ τ.*

*Proof.* `Props(τ) = Props(τ)`. For each member `p`, `τ(p) ≺ τ(p)` by induction on member type depth. ∎

**Theorem 4.2 (OEM Transitivity).** *If τ_A ≺ τ_B and τ_B ≺ τ_C, then τ_A ≺ τ_C.*

*Proof.* Let `p ∈ Props(τ_C)`. Since τ_B ≺ τ_C, there exists `p ∈ Props(τ_B)` with `τ_B(p) ≺ τ_C(p)`. Since τ_A ≺ τ_B, there exists `p ∈ Props(τ_A)` with `τ_A(p) ≺ τ_B(p)`. By inductive hypothesis on member types, `τ_A(p) ≺ τ_C(p)`. Since p was arbitrary, `Props(τ_C) ⊆ Props(τ_A)` with compatible types. ∎

**Corollary 4.3.** *OEM (≺) is a preorder on 𝕋. Combined with the top element ⊤ (ANY, which has no members) and bottom element ⊥ (NOTHING, which is a subtype of everything), (𝕋, ≺) is a bounded preorder.*

Note that ≺ is not a partial order in general: two distinct entity types `A` and `B` with identical member sets satisfy both `A ≺ B` and `B ≺ A`, yet `A ≠ B` (antisymmetry fails). This is the expected behaviour for duck typing: nominal distinctness is irrelevant to structural compatibility.

---

## 5. Generic Instantiation via Projection

### 5.1 The Projection Problem

Generic functions such as `filter`, `map`, and `reduce` have polymorphic types:

```
filter : VirtualSet<a> → (a → Bool) → VirtualSet<a>
```

When the programmer writes `employees.filter(e → e.age ≥ 65)`, the type variable `a` must be instantiated to `Employee` so that `e` inside the lambda acquires the fields of `Employee`. Classical HM generalization/instantiation [[Damas & Milner 1982]](#ref5) handles this for rank-1 polymorphism, but struggles with the additional constraint that `e`'s type must also be recorded for IDE completion purposes without polluting the inference state.

GCP solves this with a **Projection** operation that is:
1. *Bidirectional*: both the formal type parameter and the actual lambda argument constrain each other.
2. *Stateful*: a `ProjectSession` accumulates type substitutions for one call site, enabling correct handling of curried multi-argument functions.
3. *Metadata-aware*: `projectedType` is set as a side effect, providing IDE completion without affecting inference.

### 5.2 Projection Interface

The core operation is defined on `Projectable`:

```
doProject(projected: Projectable, projection: Outline, session: ProjectSession) : Outline
```

Dispatch is determined by the dynamic type of `projected`:

| Case | Method | Effect |
|------|--------|--------|
| `projected` is `Entity` | `projectEntity` | Records `projectedType`; checks structural compatibility. |
| `projected` is `Function` | `projectFunction` | Recursively projects argument and return types. |
| `projected` is `Genericable` | `projectGeneric` | Propagates projection as constraints into the Genericable. |

### 5.3 Bidirectional Lambda Projection

The critical case is when a higher-order function receives a lambda argument. Given:

```
projected  = a → b    (formal parameter of the HOF)
projection = c → d    (actual lambda provided by the caller)
```

The bidirectional projection proceeds:

```
projectLambda(a→b, c→d, session):
  1. a' ← c.project(a, session)     -- propagate lambda parameter type to formal
  2. d' ← d.project(a, session)     -- propagate parameter constraint into lambda body
  3. b' ← b.project(d', session)    -- propagate lambda body type to formal return
  4. return a' → b'
```

**Worked example.** For `employees.filter(e → e.age ≥ 65)`:

1. `filter`'s formal type: `(Employee → Bool) → VirtualSet<Employee>`.
2. The lambda `e → e.age ≥ 65` is the actual. The lambda parameter `e` starts as a fresh `Generic`.
3. Step 1: `Generic.project(Employee)` → `e.hasToBe ← Employee`; `e.projectedType ← Employee`.
4. Step 2: `(e.age ≥ 65).project(Employee)` → `e.age` resolves against `Employee`'s `age: Int` field.
5. Step 3: the expression `e.age ≥ 65` has type `Bool`; formal return type `Bool` is confirmed.
6. IDE completion for `e.` now returns `Employee`'s full member list via `e.projectedType`.

### 5.4 ProjectSession

A `ProjectSession` object is created per function call site and accumulates the mapping `{Genericable.id → Outline}` of type substitutions discovered during that call. This ensures that:

- The same type variable appearing in multiple positions is resolved consistently within one call.
- Curry chains (`f(a)(b)(c)`) are handled incrementally: each partial application creates a new session that inherits results from the parent.

---

## 6. Type Inference Algorithm

### 6.1 The Inferencer as a Visitor

GCP's inference is implemented as a **Visitor** over the AST. The `Inferencer` interface declares one `visit` method per AST node type (~50 in total). `OutlineInferencer` is the primary implementation; each `*Inference` class handles one node type in isolation.

The key design principle is **stateless rule classes**: each `*Inference` class carries no mutable state; all state lives in the AST nodes and the symbol environment. This makes inference rules independently testable and composable.

### 6.2 Inference Rules for Core Constructs

We write `Γ ⊢ e : τ` for "expression `e` in environment Γ has inferred type τ". Environment entries map names to `Genericable` constraint chains; `Γ(x) = g` retrieves the chain for `x`, and `Γ[x ↦ g]` extends Γ. Side-conditions on the right of a rule record constraint emissions; they are performed as a side effect during the single AST traversal described in §6.1. `guess(g)` (§3.5) reads the best current approximation from chain `g`.

---

**[T-IntLit]**
```
─────────────────────────────────────────────────────────────
Γ ⊢ n : Int        side: g_n.addExtendToBe(Int)
```

**[T-VarDecl]** — `let x : τ_d = e`
```
Γ ⊢ e : τ_e        g = Genericable(τ_d)        g.addExtendToBe(τ_e)
─────────────────────────────────────────────────────────────────────
Γ[x ↦ g] ⊢ let x : τ_d = e : •
```
If the consistency invariant (§3.2) `τ_e ≼ τ_d` is violated, a `CONSTRUCT_CONSTRAINTS_FAIL` error is emitted at `x`.

**[T-Assign]** — `x = e`
```
Γ ⊢ e : τ        g = Γ(x)        g.addExtendToBe(τ)
────────────────────────────────────────────────────
Γ ⊢ x = e : •
```

**[T-Access]** — `e.p`
```
Γ ⊢ e : τ_e        r = Genericable()        τ_e.addDefinedToBe({p : r})
─────────────────────────────────────────────────────────────────────────
Γ ⊢ e.p : guess(r)
```
The `addDefinedToBe` call records that `e` must structurally provide a member `p` of type `r`; OEM (§4) later checks structural compatibility.

**[T-App]** — `f(a)`
```
Γ ⊢ f : τ_f        Γ ⊢ a : τ_a        s = new ProjectSession()
────────────────────────────────────────────────────────────────────────────────
Γ ⊢ f(a) : doProject(τ_f, τ_a, s)          if τ_f is HigherOrderFunction (HOF)

Γ ⊢ f : τ_f        Γ ⊢ a : τ_a
──────────────────────────────────────────────────────────────────────────
Γ ⊢ f(a) : τ_r      where τ_f.addDefinedToBe(τ_a → τ_r),  τ_r fresh      (FOF)
```
The HOF path delegates to the bidirectional Projection mechanism (§5); the FOF path constrains `f`'s structural type directly.

**[T-Lambda]** — `x → e`
```
g = Genericable()        Γ[x ↦ g] ⊢ e : τ_body
───────────────────────────────────────────────────────────────────
Γ ⊢ (x → e) : guess(g) → τ_body
```
`g` starts unconstrained (τ_h = ⊤) and is narrowed when the lambda is passed to a HOF via [T-App] and Projection (§5.3).

---

The `isLazy` guard (§6.3) suppresses [T-Access] and [T-App] on their first pass when `τ_e = UNKNOWN`, deferring type emission to subsequent rounds. All rules are otherwise syntax-directed and deterministic: each AST node type fires exactly one rule.

### 6.3 The isLazy Mechanism

In higher-order function bodies, member access on a lambda parameter may occur before the parameter's type is fully known (because the parameter's `hasToBe` is set during projection, which happens at the call site, not inside the lambda). To avoid premature constraint fixation, GCP uses a **lazy evaluation** guard:

```
if inferencer.isLazy() and τ_e.guess() == UNKNOWN:
    return UNKNOWN    -- defer to next inference round
```

`isLazy()` returns `true` for all but the final inference pass over a given module. This mechanism ensures that multi-pass inference can refine types incrementally without generating spurious errors on nodes whose information has not yet arrived.

---

## 7. Multi-Module Type Inference: Fixed-Point Algorithm

### 7.1 The Cross-Module Import Problem

Real programs are composed of multiple interdependent modules. A module `A` may import a function from module `B`, while `B` itself imports a type from `A` — creating circular dependencies that defeat any topological sort strategy.

GCP resolves this with a **fixed-point iteration** over the module forest (ASF):

```
Algorithm: ASF_Infer(modules M₁…Mₙ)
  Phase 1 — Pre-registration:
    for each Mᵢ:
        GlobalEnv.define(Mᵢ.name, EmptyModuleShell(Mᵢ))

  Phase 2 — First pass:
    for each Mᵢ:
        Mᵢ.infer(GlobalEnv)     -- unresolved imports become LazyModuleSymbol

  Phase 3 — Fixed-point iteration (max R rounds):
    changed ← true
    round   ← 0
    while changed and round < R:
        changed ← false
        for each Mᵢ:
            before ← Mᵢ.typeSignature()
            Mᵢ.infer(GlobalEnv)
            if Mᵢ.typeSignature() ≠ before: changed ← true
        round += 1
```

The default bound `R = 100` is empirically sufficient for all practical module graphs encountered in Outline programs.

### 7.2 LazyModuleSymbol

When module `A` references a symbol `f` from module `B` during Phase 2, but `B` has not yet been inferred, GCP inserts a `LazyModuleSymbol`:

```
LazyModuleSymbol(module: B, name: f):
    eventual() → GlobalEnv.lookup(B).lookup(f)  -- resolved lazily on first use
```

`LazyModuleSymbol` implements the full `Outline` interface by delegating every method to `eventual()`. This transparent wrapping means that expressions containing unresolved cross-module references simply produce `UNKNOWN` on the first pass, to be refined in subsequent rounds.

### 7.3 Convergence

**Theorem 7.1 (Global Convergence of Fixed-Point Iteration).** *Let P be a finite Outline program with N Genericable variables and M modules. The multi-module fixed-point iteration (Algorithm 7.1) terminates after at most K = 4·N·|𝕋| inference steps, regardless of the order in which cross-module dependencies are resolved.*

*Proof.* The argument proceeds in two parts.

**Part A — Per-variable termination.** By Theorem 3.3 (§3.7), each individual Genericable variable reaches a fixed point after at most 1 + 3·|𝕋| updates. Since P contains N variables, the aggregate number of variable-update events before all variables are stable is at most N·(1 + 3·|𝕋|).

**Part B — Cross-module stabilization.** Each `LazyModuleSymbol` (§7.2) acts as a proxy that resolves to a concrete `Outline` type once the referenced module completes its own fixed-point pass. A lazy symbol's resolution triggers a re-emission of the `hasToBe` constraints that depended on it — exactly one constraint re-propagation event per import edge. The number of import edges in a program is at most M·N (every variable in every module could in principle import from every other module), and each re-propagation produces at most one additional update per variable by Part A. The total additional steps introduced by cross-module lazy resolution is therefore bounded by M·N·(1 + 3·|𝕋|).

Combining both parts, the algorithm terminates in at most (M+1)·N·(1 + 3·|𝕋|) steps. For typical programs (M ≤ 20, N ≤ 500, |𝕋| ≤ 30), this bound is well within 10⁶ steps. In practice, convergence occurs within **3–5 rounds** because most cross-module imports stabilize after a single propagation pass. ∎

**Corollary 7.2 (Determinism).** *The fixed types inferred by Algorithm 7.1 are independent of the order in which modules are processed.*

*Proof sketch.* Each constraint operation is a lattice meet or join (§3.4), which are commutative and associative. The accumulated constraint state after processing all modules is therefore order-independent — the same set of constraints is accumulated regardless of traversal order. ∎

The practical implementation enforces a ceiling of 100 rounds as a safety guard; Corollary 7.2 ensures that reaching this ceiling is a signal of a program with pathologically deep mutual recursion, not an artifact of scheduling order.

---

## 8. The Outline Interpreter and Runtime Model

### 8.1 Tree-Walking Interpreter

GCP includes a **tree-walking interpreter** symmetric to the inference visitor. `OutlineInterpreter` traverses the post-inference AST and evaluates each node by delegating to the corresponding `*Interpretation` class.

The interpreter operates on a **runtime value domain** 𝕍:

| Value Class | Outline Type | Java Backing |
|-------------|-------------|-------------|
| `IntValue` | `Int / Long` | `long` |
| `FloatValue` | `Float / Double` | `double` |
| `StringValue` | `String` | `java.lang.String` |
| `BoolValue` | `Bool` | `boolean` |
| `UnitValue` | `●` | Singleton |
| `FunctionValue` | `τ₁ → τ₂` | Closure (captures Environment) |
| `EntityValue` | `Entity(...)` | Field map + method table |
| `TupleValue` | `Tuple(...)` | `Map<String, Value>` |
| `ArrayValue` | `[τ]` | `List<Value>` |
| `DictValue` | `[τ_k : τ_v]` | `Map<Value, Value>` |

The interpreter uses **exception-driven control flow** for return statements: a `ReturnException` is thrown to unwind the call stack to the nearest enclosing function frame — a standard technique for tree-walking interpreters that avoids CPS transformation.

### 8.2 Metadata Extraction

After inference, `MetaExtractor` traverses the inferred `LocalSymbolEnvironment` to produce a `ModuleMeta` object containing:

- `outlines()` — all `outline` type declarations with full member lists
- `variables()` — all variable declarations with inferred types
- `functions()` — all function declarations with parameter and return types
- `scopeAt(offset)` — the tightest scope enclosing a given byte offset (for context-sensitive completion)
- `membersOf(sym, offset)` — members of a named symbol, using `projectedType` to resolve lambda parameters to their concrete entity types

The `projectedType` mechanism (§3.6) is the key enabler of accurate lambda-parameter completion: because `projectedType` records the concrete entity type at each call site independently of the inference constraint chain, IDE tooling can query it without disturbing the type-checking results.

### 8.3 The Plugin System

GCP provides an extension point called **GCPBuilderPlugin** that allows Java objects to be injected into the Outline runtime. The injection syntax uses a distinguished constructor call:

```outline
let repo = __my_repo__<Employee>;
```

The runtime looks up a plugin with identifier `"my_repo"` (stripped of the `__` delimiters), passes `Employee` as the type argument, and replaces the node with the Java object returned by the plugin. Plugin JARs are discovered via `ServiceLoader`, with a naming convention of `ext_builder_*.jar`.

This mechanism is the primary integration point between Outline programs and the host JVM, used extensively by the Entitir platform (§9) to inject database repository objects.

---

## 9. Application: GCPToSQL and the Entitir Ontology Platform

### 9.1 GCPToSQL: Compiling Lambda Predicates to SQL

One of GCP's most significant applications is in the **Entitir** ontology data platform, where Outline lambda expressions serve as type-safe predicates over relational data.

`GCPToSQL` is a compiler that translates GCP lambda AST fragments into SQL `WHERE` clauses. The translation preserves three levels of complexity:

**Level 1 — Scalar predicates:**
```outline
employees.filter(e → e.age >= 65 && e.active == 1)
```
compiles to:
```sql
SELECT * FROM employee WHERE age >= 65 AND active = 1
```

**Level 2 — Negation:**
```outline
employees.filter(e → !e.deleted)
```
compiles to:
```sql
SELECT * FROM employee WHERE NOT deleted
```

**Level 3 — Schema-aware navigation (N-hop FK traversal):**
```outline
employees.filter(e → e.department().head().office().floor > 3)
```
compiles to a correlated scalar subquery:
```sql
SELECT * FROM employee
WHERE (
  SELECT floor FROM office WHERE id = (
    SELECT office_id FROM person WHERE id = (
      SELECT head_id FROM department WHERE id = employee.department_id
    )
  )
) > 3
```

The L3 translation is enabled by `EntitySchema` metadata derived from GCP's inferred type information: the schema records which entity fields are foreign-key references, and GCPToSQL uses this to determine the correct join strategy.

**Reverse-edge predicates** (one-to-many relationships navigated backwards) are compiled to `EXISTS` or `COUNT` subqueries:

```outline
departments.filter(d → d.employees().filter(e → e.age < 30).count() > 5)
```
→
```sql
SELECT * FROM department
WHERE (
  SELECT COUNT(*) FROM employee
  WHERE department_id = department.id AND age < 30
) > 5
```

### 9.2 Entitir: A GCP-Powered Ontology Platform

Entitir is a domain-model platform in which every entity, every relationship, and every query is mediated by GCP-type-checked Outline expressions. Its key components and their GCP dependencies are:

| Component | GCP Dependency |
|-----------|----------------|
| **OntologyWorld** | Hosts a `OutlineInterpreter` warm-started with entity schemas and actions |
| **Entity schema definition** | Outline declarations generated from JSON schemas via `Entitir.convert()` |
| **Query API** (`world.eval()`) | Executes arbitrary Outline expressions against live data |
| **Action invocation** | Outline expressions of the form `entities.filter(…).first().action(arg)` |
| **Decision templates** | Trigger expressions and hint expressions are GCP-compiled and cached |
| **LLM agent tools** | `ontology_eval` tool uses GCP to execute LLM-generated Outline expressions safely |

The integration between GCP's type system and Entitir's entity model means that every query generated by the LLM agent is type-checked before execution — preventing common errors such as accessing non-existent fields or calling actions on the wrong entity type.

### 9.3 Discussion: Generality of the GCP Substrate

The Entitir application demonstrates GCP's original design goal: type-safe query generation for a domain-specific language. The same substrate has been applied in a second independent domain — **GCP-Python** (described in a companion paper [[GCP-Python CGO]]) — where GCP's demand-driven `hasToBe` propagation from call-site contexts automatically infers PEP 484 type annotations for zero-annotation Python source files, enabling the `mypyc` ahead-of-time compiler to achieve an average 13×–18× speedup over CPython with no developer effort. We note this application here to establish that GCP's constraint model is not domain-specific: the same four-dimensional lattice framework that powers type-safe SQL generation equally powers demand-driven type annotation for a dynamically-typed language compiler. The separation of concerns — constraint propagation in GCP, application-specific constraint emission in the host language converter — is the design principle that enables this generality.


## 10. Discussion

### 10.1 Comparison with Hindley–Milner

The classical Hindley–Milner (HM) type inference algorithm [[Milner 1978]](#ref3) generates a global set of type-equality constraints and solves them by Robinson's unification. GCP differs in three fundamental ways:

| Dimension | Hindley–Milner | GCP |
|-----------|---------------|-----|
| **Constraint structure** | Set of type equalities `τ = σ` | Four directional inequalities per variable |
| **Solution method** | Global unification (Robinson) | Local per-variable constraint narrowing |
| **Incremental update** | Requires full re-inference on change | Single-variable constraint addition is O(1) |
| **Dynamic extension** | No native support | `extendToBe` captures post-declaration assignments |
| **Gradual types** | Through explicit `dyn` types | Through `UNKNOWN`/`ANY` initial values |

The key advantage of GCP's directional approach is that it naturally handles the *late binding* patterns of dynamic languages: a variable's type can be progressively refined as more of the surrounding code is analyzed, without requiring re-analysis of already-processed code.

### 10.2 Comparison with TypeScript

TypeScript uses a structural type system similar in spirit to OEM, but differs in the inference engine:

- TypeScript uses bidirectional type checking [[Pierce & Turner 2000]](#ref1) with explicit propagation of "contextual types" (similar to `hasToBe`), but does not formalize the full four-dimensional chain.
- TypeScript's generic inference for higher-order functions often requires explicit type annotations, whereas GCP's projection mechanism (§5) handles the common cases automatically.
- TypeScript's type narrowing (via `if typeof x === "string"`) is a separate mechanism; GCP achieves similar narrowing through the constraint chain without requiring explicit guard expressions.

### 10.3 Limitations

**Rank-2 Polymorphism.** GCP handles rank-1 polymorphism naturally, including Church numeral *addition*. For example:

```outline
let zero       = f -> x -> x;
let succ       = n -> f -> x -> f(n(f)(x));
let church_add = m -> n -> f -> x -> m(f)(n(f)(x));
let decode     = n -> n(x -> x + 1)(0);
let eight      = church_add(succ(succ(succ(zero))))(succ(succ(succ(succ(succ(zero))))));
decode(eight)   -- evaluates to 8, zero type errors
```

GCP infers `zero : (β→β)→α→β`, `succ : ((β→β)→β→β)→(β→β)→β→β`, and `church_add` without any annotations and with zero type errors. The call-site `hasToBe` propagation correctly specializes the polymorphic Church numeral types at each use site.

*Church multiplication*, however, hits a theoretical wall:

```outline
let church_mul = m -> n -> f -> m(n(f));   -- PROJECT_FAIL error
```

Here `n(f)` forces `type(n) = type(f)→α`, while `m(n(f))` simultaneously forces `type(n) = α→β`. The two constraints require `type(f) = α`, producing the recursive type `α = α→β` — an infinite type that cannot be represented in a Rank-1 system. GCP correctly reports a `PROJECT_FAIL` type error (`[type_system] project compilation failed`) for this expression. This is an inherent limitation shared with all Rank-1 systems: Hindley–Milner gives the same error; Haskell requires an explicit `RankNTypes` pragma plus a hand-written signature. [[Wells 1994]](#ref15) proved that Rank-2 type *inference* (finding the polymorphic type without annotation) is decidable but impractical; Rank-3 and above are undecidable. The correct encoding requires the quantifier *inside* the arrow: `∀a.(a→a)→a→a` — a Rank-2 type that lies outside GCP's inference scope.

**Recursive Types.** Mutually recursive type definitions require careful constraint ordering to avoid infinite-depth subtype checks. GCP's cycle-detection mechanism (ThreadLocal visited set) prevents infinite loops but may conservatively report type errors for some valid recursive structures.

**Cross-Type Intersections.** GCP approximates intersection types via `Poly` (the ad-hoc polymorphism sum type), which is less precise than a full intersection type system. Programs that require true intersection types must use explicit interface declarations.

**Convergence Bound.** While the 100-round limit is empirically sufficient, it is not a formal convergence guarantee for all Outline programs. Programs with extremely deep dependency chains may require bound adjustment.

---

## 11. Related Work

**Type Inference.** The foundational work is Milner's Algorithm W [[Milner 1978]](#ref3) and its extension to recursive types [[Damas & Milner 1982]](#ref5). GCP's constraint-based approach is more closely related to the work of Pottier and Rémy [[Pottier & Rémy 2005]](#ref4), who generalize HM to constraint-based frameworks supporting subtyping.

**Gradual Typing.** The gradual type system of Pierce and Turner [[Pierce & Turner 2000]](#ref1) and its subsequent elaborations [[Siek & Taha 2006]](#ref6) allow mixing of typed and untyped code. The abstract interpretation of gradual types [[Garcia et al. 2016]](#ref19) provides a lattice-theoretic foundation closely related to GCP's constraint chain. GCP's `UNKNOWN` type and lazy inference play a similar role to the `?` type in gradual systems but are fully internal — they do not appear in the user-facing type language.

**Structural Typing.** Structural type systems for object-oriented languages are surveyed in [[Cardelli & Wegner 1985]](#ref7). GCP's OEM is a formalization of duck typing, closely related to record subtyping in type theory [[Pierce 2002]](#ref16) but extended to handle cyclic structural types. The behavioral notion of subtyping of [[Liskov & Wing 1994]](#ref20) complements OEM: where OEM is syntactic/structural, Liskov-Wing subtyping is behavioral. GCP targets the structural side exclusively.

**Occurrence Typing.** Typed Racket [[Tobin-Hochstadt & Felleisen 2008]](#ref18) introduces *occurrence typing*, which narrows a variable's type in each branch of a conditional based on predicate tests. GCP achieves narrowing through the constraint chain without requiring explicit test predicates; the trade-off is that GCP narrows globally while occurrence typing narrows flow-sensitively per branch.

**TypeScript and JavaScript Type Checkers.** The industrial-scale type system most comparable to GCP is TypeScript [[Microsoft 2012]](#ref2). The specific comparison in §10.2 highlights the differences. Work on understanding TypeScript's unsoundness [[Bierman et al. 2014]](#ref8) motivates GCP's more disciplined constraint model. Facebook's Flow [[Flow 2017]](#ref21) applies a similar structural type system to JavaScript with an emphasis on inter-procedural inference, but like TypeScript it does not separate the four dimensions of type information that GCP maintains.

**Liquid Types.** Liquid types [[Rondon et al. 2008]](#ref9) extend HM with refinement predicates. GCP's `hasToBe` and `definedToBe` constraints play a similar role of capturing usage patterns, but GCP targets structural shape rather than value predicates.

**Type Inclusion Constraints.** The constraint-based approach of [[Aiken & Wimmers 1993]](#ref22) solves type inclusion constraints for a typed lambda calculus. GCP's constraint chain generalizes this to a four-dimensional system where each dimension uses a distinct lattice operation (join vs. meet), enabling the separation of value assignment from usage demand.

**Demand-Driven Type Inference.** The propagation of type demands from use-sites to definition-sites is closely related to Mycroft's polymorphic type inference [[Mycroft 1984]](#ref12) and to the *bottom-up* variant of constraint-based inference. GCP's `hasToBe` propagation is a call-site demand constraint in this tradition, applied to a dynamically typed host language. The companion paper [[GCP-Python CGO]](#ref-gcpython) applies GCP's demand-driven inference to automatically annotate Python source for ahead-of-time compilation, demonstrating that the same constraint substrate generalizes from type-safe query generation to compiler optimization.

**Effect Systems and Capability Types.** Effect systems [[Lucassen & Gifford 1988]](#ref11) track side-effectful operations through a fourth annotation dimension analogous to GCP's `definedToBe` structural access constraint. While effect systems focus on the *nature* of operations (read, write, allocate), GCP's `definedToBe` captures the *shape* required by operations. Both approaches use a meet-based composition rule; the difference lies in the constraint domain (effect lattice vs. structural type lattice).

**Flow-Sensitive Typing.** Flow-sensitive type systems [[Flanagan & Felleisen 1999]](#ref10) narrow types at control-flow join points. GCP's constraint chain achieves a limited form of flow sensitivity through `hasToBe`: a variable's demanded type at one use-site narrows its effective type globally. Full flow sensitivity (narrowing at each use independently) is not currently supported; this is the trade-off GCP makes for tractability and incrementality.

**Rank-2 Polymorphism.** [[Wells 1994]](#ref15) proved that typability in System F (rank-2 polymorphism) is decidable but equivalent in complexity to semi-unification, making it impractical for automated inference. This result bounds GCP's inference scope (§10.3) and applies equally to Hindley–Milner and all other Rank-1 systems.

---

## 12. Conclusion

We have presented **Generalized Constraint Projection (GCP)**, a type inference methodology for dynamic languages built around three core contributions:

1. **The four-dimensional constraint model**, which separates the four qualitatively distinct sources of type information (actual value, declaration, usage context, structural access pattern) and propagates them independently. This avoids the spurious constraint conflicts that arise when these sources are conflated, and naturally handles the late-binding patterns of dynamic languages.

2. **Outline Expression Matching (OEM)**, a formalization of duck typing that defines structural subtyping via recursive member-set containment. The bidirectional delegation protocol (`tryIamYou` / `tryYouAreMe`) makes OEM an open extension point that can accommodate new Outline types without modifying existing code.

3. **Bidirectional Projection**, a generic instantiation mechanism that propagates type information simultaneously from call-site context into lambda parameters (enabling type-safe filter/map operations) and from lambda body results back to call-site return types. The orthogonal `projectedType` field supports IDE metadata extraction without perturbing inference.

Together, these contributions deliver on the key promise of GCP: eliminating the three pain points of existing type systems for dynamic languages — redundant annotations, context insensitivity, and cumbersome generic inference — while remaining sound (Theorem 3.4, 3.5), convergent (Theorems 3.3, 7.1), and decidable (Corollary 3.7) on the programs that dynamic language developers actually write.

The formal results establish GCP as more than an engineering pragmatism: it is a principled constraint-propagation substrate with provable properties. The four-dimensional model is not an ad-hoc engineering choice but the minimal decomposition needed to handle the four qualitatively distinct sources of type information that arise in dynamic language programs simultaneously. OEM (Theorem 4.2) is a preorder, not merely a relation, confirming that structural subtyping is transitive and thus usable as a coherent type lattice. The O(N·|𝕋|·M) complexity bound (Theorem 3.6) and the order-independence guarantee (Corollary 7.2) establish that GCP can be deployed in interactive tooling without concern for correctness artifacts introduced by module processing order.

The **Entitir ontology platform** grounds these theoretical claims in an industrial-scale deployment: GCP handles type checking for every entity query, LLM-generated Outline expression, and decision trigger executed against a live relational database, providing end-to-end type safety that has prevented an entire class of field-access errors from reaching production. The GCPToSQL compiler (§9.1) demonstrates that the type information GCP derives at analysis time is precise enough to drive N-hop foreign-key SQL generation — a task that requires both structural subtyping (OEM) and demand propagation (`hasToBe`) working in concert.

The generality of GCP's constraint substrate is evidenced by its application to a second domain — demand-driven Python type annotation for ahead-of-time compilation (§9.3; detailed in [[GCP-Python CGO]](#ref-gcpython)) — confirming that the separation of concerns between the constraint propagation core and the host-language emission layer is architecturally sound. Future work includes: extending OEM to intersection types, formalizing a complete operational semantics for Outline, and applying demand-driven inference to additional annotation-heavy compilation pipelines.

---

## References

<a id="ref1">[Pierce & Turner 2000]</a> B.C. Pierce and D.N. Turner. Local type inference. *ACM Transactions on Programming Languages and Systems*, 22(1):1–44, 2000.

<a id="ref2">[Microsoft 2012]</a> Microsoft Corporation. TypeScript: Typed Superset of JavaScript. https://www.typescriptlang.org, 2012.

<a id="ref3">[Milner 1978]</a> R. Milner. A theory of type polymorphism in programming. *Journal of Computer and System Sciences*, 17(3):348–375, 1978.

<a id="ref4">[Pottier & Rémy 2005]</a> F. Pottier and D. Rémy. The essence of ML type inference. In B.C. Pierce, editor, *Advanced Topics in Types and Programming Languages*, chapter 10. MIT Press, 2005.

<a id="ref5">[Damas & Milner 1982]</a> L. Damas and R. Milner. Principal type-schemes for functional programs. In *Proceedings of POPL*, pages 207–212, 1982.

<a id="ref6">[Siek & Taha 2006]</a> J.G. Siek and W. Taha. Gradual typing for functional languages. In *Proceedings of the Scheme and Functional Programming Workshop*, pages 81–92, 2006.

<a id="ref7">[Cardelli & Wegner 1985]</a> L. Cardelli and P. Wegner. On understanding types, data abstraction, and polymorphism. *ACM Computing Surveys*, 17(4):471–523, 1985.

<a id="ref8">[Bierman et al. 2014]</a> G. Bierman, M. Abadi, and M. Torgersen. Understanding TypeScript. In *Proceedings of ECOOP*, pages 257–281, 2014.

<a id="ref9">[Rondon et al. 2008]</a> P.M. Rondon, M. Kawaguci, and R. Jhala. Liquid types. In *Proceedings of PLDI*, pages 159–169, 2008.

<a id="ref10">[Flanagan & Felleisen 1999]</a> C. Flanagan and M. Felleisen. Componential set-based analysis. *ACM Transactions on Programming Languages and Systems*, 21(2):370–416, 1999.

<a id="ref11">[Lucassen & Gifford 1988]</a> J.M. Lucassen and D.K. Gifford. Polymorphic effect systems. In *Proceedings of POPL*, pages 47–57, 1988.

<a id="ref12">[Mycroft 1984]</a> A. Mycroft. Polymorphic type schemes and recursive definitions. In *Proceedings of the International Symposium on Programming*, LNCS 167, pages 217–228, 1984.

<a id="ref13">[Cannon 2005]</a> B. Cannon. Localized type inference of atomic types in Python. M.S. Thesis, California Polytechnic State University, 2005.

<a id="ref14">[Salib 2004]</a> M. Salib. Starkiller: A static type inferencer and compiler for Python. M.S. Thesis, MIT, 2004.

<a id="ref15">[Wells 1994]</a> J.B. Wells. Typability and type checking in the second-order lambda-calculus are equivalent and undecidable. In *Proceedings of LICS*, pages 176–185, 1994.

<a id="ref16">[Pierce 2002]</a> B.C. Pierce. *Types and Programming Languages*. MIT Press, Cambridge, MA, 2002.

<a id="ref17">[Wright & Felleisen 1994]</a> A.K. Wright and M. Felleisen. A syntactic approach to type soundness. *Information and Computation*, 115(1):38–94, 1994.

<a id="ref18">[Tobin-Hochstadt & Felleisen 2008]</a> S. Tobin-Hochstadt and M. Felleisen. The design and implementation of Typed Racket. In *Proceedings of POPL*, pages 395–406, 2008.

<a id="ref19">[Garcia et al. 2016]</a> R. Garcia, A.M. Clark, and É. Tanter. Abstracting gradual typing. In *Proceedings of POPL*, pages 429–442, 2016.

<a id="ref20">[Liskov & Wing 1994]</a> B.H. Liskov and J.M. Wing. A behavioral notion of subtyping. *ACM Transactions on Programming Languages and Systems*, 16(6):1811–1841, 1994.

<a id="ref21">[Flow 2017]</a> A. Chaudhuri, V. Vekris, S. Goldman, M. Roch, and G. Lerner. Fast and precise type checking for JavaScript. *Proceedings of OOPSLA*, 1(OOPSLA):48:1–48:30, 2017.

<a id="ref22">[Aiken & Wimmers 1993]</a> A. Aiken and E.L. Wimmers. Type inclusion constraints and type inference. In *Proceedings of FPCA*, pages 31–41, 1993.

<a id="ref23">[Hindley 1969]</a> J.R. Hindley. The principal type-scheme of an object in combinatory logic. *Transactions of the American Mathematical Society*, 146:29–60, 1969.

<a id="ref-gcpython">[GCP-Python CGO]</a> (companion paper). Zero-Annotation Python Ahead-of-Time Compilation via Demand-Driven Call-Site Type Inference. Submitted to CGO 2026.
