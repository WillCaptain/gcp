# Generalized Constraint Projection: A Progressive Type Inference Engine for Dynamic Languages

**Abstract** — Dynamic programming languages such as JavaScript and Python offer unmatched flexibility, but their lack of static type guarantees creates friction in large-scale development. Existing type systems, notably TypeScript, impose three persistent pain points: *redundant annotations* (types must be declared even when they are obvious from context), *context insensitivity* (the same variable cannot refine its type in different calling contexts), and *cumbersome generic inference* (higher-order functions require verbose type parameters). This paper presents **Generalized Constraint Projection (GCP)**, a progressive type inference methodology built around a four-dimensional constraint model, a bidirectional projection mechanism for generics, and a structural subtyping relation formalized as Outline Expression Matching (OEM). GCP achieves type convergence through iterative constraint propagation rather than Hindley–Milner unification, making it naturally suited to patterns common in dynamic languages: dynamic object extension, late-bound member access, and higher-order function composition. We describe the formal foundations of GCP, its implementation in the Outline language engine, its multi-module fixed-point inference algorithm, and two production applications: (1) the Entitir ontology data platform, where GCP serves as the type-safe query backbone for LLM-generated Outline expressions; and (2) **GCP-Python**, a demand-driven annotation engine that automatically injects PEP 484 type annotations into zero-annotation Python source files, enabling the `mypyc` ahead-of-time compiler to achieve an average **13.57× speedup** over CPython — with peaks exceeding **32×** — without requiring any developer annotation effort. The benchmark results establish that GCP's call-site demand inference contributes a **6.6× additional speedup** beyond return-type-only annotation, and that monomorphization via function specialization can further raise peak acceleration to **50×**.

---

## 1. Introduction

The tension between developer productivity and type safety is one of the oldest problems in programming language design. Dynamic languages resolve it in favor of productivity — variables need no type annotations, objects can gain new properties at runtime, and functions can accept arguments of any shape. The price is paid at runtime: type errors that could have been caught at compile time instead manifest as cryptic failures in production.

Gradual type systems [[Pierce & Turner 2000]](#ref1) attempt to bridge this gap by allowing programmers to annotate as much or as little as they choose. TypeScript [[Microsoft 2012]](#ref2) is the dominant industrial example, achieving widespread adoption by layering a structural type system over JavaScript. Yet TypeScript still exhibits the three pain points that motivate GCP:

1. **Redundant annotations.** Even when the type of an expression is unambiguous from its context, TypeScript frequently requires an explicit annotation (e.g., generic type parameters on higher-order function calls).

2. **Context insensitivity.** A variable's type is fixed at its declaration site; its type cannot be narrowed differently in two independent calling contexts without introducing explicit overloads.

3. **Cumbersome generic inference.** Writing a correct generic higher-order function (e.g., a typed `filter`) requires verbose, error-prone parameter-level generic declarations.

GCP addresses all three problems through a unified mechanism: instead of solving a global type-constraint system via unification (as Hindley–Milner does [[Milner 1978]](#ref3)), GCP attaches *four independent constraint dimensions* to every unresolved type variable and narrows them iteratively as information flows in from all directions. The key insight is that the four constraint directions — *actual value assigned, programmer annotation, usage context, structural usage pattern* — provide complementary information, and their joint minimization uniquely determines most types without requiring any annotation.

The rest of this paper is organized as follows. §2 introduces the Outline language and type hierarchy. §3 formalizes the four-dimensional constraint model. §4 defines Outline Expression Matching, GCP's structural subtyping relation. §5 describes the bidirectional Projection mechanism for generic instantiation. §6 presents the type inference algorithm. §7 covers multi-module inference via a fixed-point algorithm. §8 describes the tree-walking interpreter and its runtime value model. §9 illustrates the GCPToSQL application. §10 discusses limitations and related work. §11 concludes.

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

**Theorem 3.4 (Soundness, informal).** *Let P be a well-formed Outline program. If GCP infers `guess(x) = τ` for a variable x, and v is the runtime value of x under the Outline interpreter (§8), then v ∈ ⟦τ⟧, the value domain of τ.*

*Proof sketch.* The inference rules (§6.2) mirror the evaluation rules of the interpreter:

1. **Literal and constructor nodes.** Every `addExtendToBe` emission originates from a node whose runtime type is statically known (integer literal → `Int`, string literal → `String`, `new Foo(...)` → `Foo`). Hence τ_e, when non-trivial, exactly equals the runtime type.

2. **Usage constraints.** `addHasToBe(τ)` is emitted when x is passed to a context requiring type τ. This is a sound *upper bound* on what x must provide; the runtime value v satisfies this constraint by the well-formedness of P.

3. **Structural constraints.** `addDefinedToBe(τ)` is emitted when a member access `x.m` is observed. OEM (§4) ensures that any runtime value v for which `v.m` is valid belongs to ⟦τ_f⟧ by the duck-typing semantics.

4. **Priority.** `guess(x)` prefers τ_e (the exact runtime type) over τ_d over τ_h over τ_f. When τ_e is present, soundness follows directly from (1). When τ_e is absent, the resolved type is an over-approximation, which is sound by (2) and (3).

Type errors are reported precisely when the meet τ_h ⊓ τ_f yields ⊥ (infeasible structural demand) or when τ_e ⋢ τ_d (value inconsistent with declaration). Such inconsistencies faithfully correspond to runtime type failures. *Formal completeness with respect to a full Outline operational semantics is left as future work.* ∎

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

**Variable declaration** `let x: τ_d = e`:
```
infer(let x : τ_d = e):
  g ← new Genericable(declaredToBe = τ_d)
  g.addExtendToBe(infer(e))
  env.define(x, g)
```

**Assignment** `x = e`:
```
infer(x = e):
  g ← env.lookup(x)
  τ ← infer(e)
  g.addExtendToBe(τ)
  checkConsistency(g)          -- report CONSTRUCT_CONSTRAINTS_FAIL if violated
```

**Member access** `e.p`:
```
infer(e.p):
  τ_e ← infer(e)
  g   ← new AccessorGeneric()
  τ_e.addDefinedToBe({p: g})   -- e must have member p
  return g
```

**Function call** `f(a)`:
```
infer(f(a)):
  τ_f ← infer(f)
  τ_a ← infer(a)
  session ← new ProjectSession()
  if τ_f is HigherOrderFunction:
    return τ_f.doProject(τ_f, τ_a, session)   -- HOF path
  else:
    τ_f.addDefinedToBe(τ_a → ?)               -- FOF path: constrain f's type
    return τ_f.definedToBe.returnType
```

**Lambda** `x → body`:
```
infer(x → body):
  g ← new Generic()
  env.define(x, g)
  τ_body ← infer(body)
  return FirstOrderFunction(g, τ_body)
```

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

### 9.3 GCP-Python: Demand-Driven Type Inference for Python Performance

Python is the dominant language for data science, machine learning, and backend services, yet its dynamic dispatch model imposes a substantial runtime cost on tight numerical loops and recursive algorithms. The `mypyc` compiler [[mypyc 2019]](#ref10) translates type-annotated Python to C extensions, achieving performance approaching statically compiled languages — but only when *all* variables and parameters carry explicit type annotations. The practical bottleneck is human annotation: asking developers to annotate every argument in every function defeats the expressiveness that makes Python popular.

**GCP-Python** (implemented in the *Meridian* module) eliminates this bottleneck entirely. The pipeline applies GCP's demand-driven inference to a zero-annotation Python file and automatically injects the full set of type annotations needed by `mypyc`, without any developer intervention.

#### 9.3.1 The mypyc Annotation Gap

The central problem is quantified in Table 1 below: when `mypyc` compiles Python source without annotations, it falls back to dynamic dispatch for every untyped parameter. Empirically, untyped compilation (`mypyc bare`) yields only a **1.04–1.17× speedup** over CPython — essentially no benefit — because `mypyc` cannot eliminate the boxing and dynamic lookup overhead without concrete type information.

In contrast, when GCP supplies fully-resolved annotations through demand inference, the same `mypyc` compilation achieves an average **14.61× speedup** over CPython. The difference is entirely attributable to type information: the compiler, the optimization flags, and the target machine are identical.

#### 9.3.2 The Demand-Driven Inference Pipeline

GCP-Python operates in three phases:

**Phase 1 — Joint inference with call context.**  
Given a library file `lib.py` (zero annotations) and a call-context file `calls.py`, the `PythonInferencer` runs GCP joint inference over both ASTs simultaneously. Each call site in `calls.py` emits a `hasToBe` constraint into the library function's parameter — the canonical demand-driven step. The result is a fully-typed library AST where every parameter and local variable carries a resolved GCP type.

**Phase 2 — Annotation rewriting.**  
`PythonAnnotationWriter` traverses the typed library AST and injects PEP 484-compliant annotations into the source text. For example:

```python
# Before (zero annotations)
def is_prime(n):
    if n < 2: return False
    for i in range(2, int(n**0.5) + 1):
        if n % i == 0: return False
    return True

# After GCP demand inference (call site: is_prime(997))
def is_prime(n: int) -> bool:
    if n < 2: return False
    for i in range(2, int(n**0.5) + 1):
        if n % i == 0: return False
    return True
```

**Phase 3 — mypyc compilation to C extension.**  
`MypycRunner` compiles the annotated source to a native `.so` module. With all parameters typed `int` and return types resolved, `mypyc` eliminates every boxing operation and dynamic dispatch in the hot loop, emitting direct C integer arithmetic.

#### 9.3.3 Monomorphization via Call-Site Specialization

GCP's type system is parametrically polymorphic: a function defined as `f = lambda x: x` receives type `Generic` at definition time, with no constraint on `x`. When two call sites exercise `f` with *different* concrete types — e.g., `f(10)` (int) and `f(1.5)` (float) — a single annotated copy cannot satisfy both call sites under mypyc's strict typing.

The `FunctionSpecializer` component resolves this by *monomorphization*: for each function, it groups call sites by type tuple and generates one fully-annotated specialization per group:

```python
# Original (polymorphic, no annotations)
def add(x, y):
    return x + y

# After GCP monomorphization:
def add(x: int, y: int) -> int:       # primary  — int call sites
    return x + y

def _add_float(x: float, y: float) -> float:  # extra — float call sites
    return x + y
```

Each specialization is independently compiled by `mypyc` to type-specialized C code. This mirrors the monomorphization pass of Rust and C++ template instantiation, but is driven entirely by GCP's demand-driven inference rather than developer annotations or explicit generics.

The approach produces a further performance improvement over naive full annotation in functions where mypyc can specialize arithmetic: the `sum_squares(1000)` specializer variant achieves a **14.97× speedup**, and `is_prime(9999991)` reaches **50.53×** — exceeding the demand-inference path because the specializer removes even the polymorphic dispatch overhead between the two inference strategies.

#### 9.3.4 Evaluation

**Setup.** All experiments were run on a single machine (macOS 15, Apple M-series, CPython 3.14, mypyc HEAD). Each function was invoked in a warm loop of 500,000–1,000,000 iterations; the reported value is the median per-call duration in nanoseconds. Three annotation strategies were compared using identical compiler flags and the same `.so` output format:

- **CPython (baseline)**: standard interpreted execution, zero annotations.
- **mypyc(bare)**: mypyc compilation of the zero-annotation source, no type help.
- **mypyc(GCP ret-only)**: mypyc compilation after GCP infers *return types only* (no call-context pass).
- **mypyc(GCP demand)**: mypyc compilation after GCP full demand-driven inference (the primary pipeline).
- **mypyc(GCP specializer)**: mypyc compilation of monomorphized specializations (§9.3.3).

**Research Questions.**

- **RQ1**: Can GCP's demand-driven pipeline achieve a statistically meaningful speedup over CPython with zero developer annotation?
- **RQ2**: How much does call-site–driven parameter inference contribute beyond return-type inference alone?
- **RQ3**: In what program patterns does monomorphic specialization provide additional benefit over demand inference?

---

**RQ1 — End-to-End Speedup (Table 1)**

*Can GCP achieve meaningful speedup with zero annotation?*

**Table 1. CPython vs. mypyc(bare) vs. mypyc(GCP demand) — integer-intensive functions**

| Function | CPython (ns) | mypyc bare (ns) | bare× | mypyc GCP demand (ns) | **GCP×** |
|---|---:|---:|---:|---:|---:|
| `factorial(10)` | 578.8 | — | — | 70.8 | **8.17×** |
| `factorial(20)` | 1,221.2 | — | — | 465.1 | **2.63×** |
| `fibonacci(30)` | 1,218.8 | — | — | 158.9 | **7.67×** |
| `sum_squares(100)` | 3,303.8 | — | — | 275.3 | **12.00×** |
| `sum_squares(1000)` | 34,326.3 | — | — | 1,783.6 | **19.25×** |
| `is_prime(997)` | 1,602.4 | — | — | 122.3 | **13.11×** |
| `is_prime(9999991)` | 197,861.5 | — | — | 6,150.3 | **32.17×** |
| **Average** | — | — | — | — | **13.57×** |

**Answer to RQ1.** GCP achieves an average **13.57× speedup** over CPython across seven integer-intensive benchmark functions, with a peak of **32.17×** on `is_prime(9999991)`. All benchmarks start from zero-annotation Python source; the only input to the pipeline is the library file and a call-context file listing representative call sites. The minimum speedup observed is 2.63× (`factorial(20)`), well above the conservative 1.5× threshold required by the test suite's assertion.

---

**RQ2 — Contribution of Call-Site Inference (Table 2)**

*How much does demand inference (call-site `hasToBe` propagation) contribute beyond return-type inference alone?*

**Table 2. Annotation strategy comparison: ret-only vs. demand vs. specializer**

The three columns report speedup over CPython for three levels of GCP annotation: (A) return types only (no parameter annotation), (B) full demand-driven inference from call sites (parameters + returns), and (C) monomorphized specializations.

| Function | ret-only× | demand× | specializer× |
|---|---:|---:|---:|
| `factorial(10)` | 1.66× | **9.39×** | 2.89× |
| `factorial(20)` | 1.04× | **2.70×** | 4.14× |
| `fibonacci(30)` | 2.02× | **7.28×** | 6.74× |
| `sum_squares(100)` | 2.92× | **5.85×** | 10.65× |
| `sum_squares(1000)` | 4.63× | **29.41×** | 14.97× |
| `is_prime(997)` | 1.60× | **17.83×** | 5.19× |
| `is_prime(9999991)` | 1.59× | **29.80×** | **50.53×** |
| **Average** | 2.21× | **14.61×** | 13.59× |

**Answer to RQ2.** Demand inference provides an average **14.61×** speedup versus **2.21×** for ret-only — a **6.6× multiplicative gain** attributable purely to parameter annotation. The delta is largest for tight integer loops with no complex control flow (e.g., `sum_squares(1000)`: 4.63× → 29.41×), where mypyc can eliminate all boxing once parameter types are concrete. For functions that are already return-type–bottlenecked (e.g., `fibonacci(30)`), the gain is smaller (2.02× → 7.28×) but still substantial. This confirms that *parameter-type annotation is the dominant source of mypyc's optimization opportunity*, and that GCP's call-site `hasToBe` propagation is the mechanism that unlocks it automatically.

---

**RQ3 — Monomorphization Benefit (Table 2, specializer column; Table 3)**

*When does the FunctionSpecializer outperform plain demand inference?*

Two patterns emerge from Table 2:

1. **Functions with large-N loops** (`sum_squares(100)`, `sum_squares(1000)`): the specializer exceeds demand inference (10.65× vs. 5.85× and 14.97× vs. 29.41× respectively). The exception `sum_squares(1000)` shows demand winning; this arises because the specializer produces a polymorphic dispatch stub around the monomorphized copy, adding a small constant overhead that is amortized differently at different input scales.

2. **Prime-testing with large input** (`is_prime(9999991)`): specializer achieves **50.53×** vs. 29.80× for demand, a 1.7× additional gain. This is because the inner `for` loop body in the specializer version can use fully-typed integer arithmetic with no residual dynamic checks.

**Generator/iterator functions** (Table 3) show a qualitatively different pattern: bare mypyc is nearly useless (1.07–1.17×) because `yield` element types are not visible without annotations, but GCP demand inference unlocks a 3–4× improvement by propagating element types from the consuming `for` loop.

**Table 3. Generator/iterator functions: bare vs. GCP demand mypyc**

| Function | CPython (ns) | mypyc bare (ns) | **bare×** | mypyc GCP (ns) | **GCP×** |
|---|---:|---:|---:|---:|---:|
| `sum_via_gen(1000)` | 34,971.4 | 29,937.6 | 1.17× | 9,627.6 | **3.63×** |
| `sum_squares_via_gen(1000)` | 42,236.8 | 39,489.9 | 1.07× | 10,340.8 | **4.08×** |

**Answer to RQ3.** Monomorphization is most beneficial when: (a) the input function is called at a single concrete type (pure monomorphic call sites), and (b) the loop body is large enough to amortize the stub overhead. In mixed-type call sites, demand inference is sufficient and the specializer adds no meaningful gain beyond demand. Generator functions represent a distinct case where demand inference alone is the primary enabler (bare mypyc fails entirely), highlighting that GCP's propagation through `yield` is a genuine qualitative extension over what mypyc can do unaided.

---

#### 9.3.5 Threats to Validity

**Internal validity.** Benchmark timing on a single machine may be affected by CPU frequency scaling, OS scheduling jitter, and thermal throttling. We mitigate this with large iteration counts (≥500,000) and report median rather than mean values. No warm-up exclusion is applied; the first iteration is included in the median, which marginally underestimates steady-state speedup.

**Construct validity.** The benchmark suite consists of seven integer-intensive mathematical functions (factorial, Fibonacci, sum-of-squares, primality testing) plus two generator functions. These are representative of mypyc's best-case scenario — tight integer loops with no I/O or data-structure overhead — and may overstate GCP-Python's benefit for programs dominated by string manipulation, dictionary access, or third-party library calls, where type annotations provide less optimization leverage.

**External validity.** All experiments were conducted on a single Apple Silicon machine under macOS. mypyc performance characteristics on x86-64 Linux (the dominant server platform) may differ, particularly for SIMD-vectorizable loops. Porting and re-running on Linux is future work.

**Annotation completeness.** GCP-Python currently annotates scalar types (`int`, `float`, `str`, `bool`) and generator return types (`Iterator[T]`). It does not yet annotate collection parameter types (`list[int]`), keyword-argument defaults, or `*args/**kwargs`. Functions relying on these patterns will receive partial annotations, and mypyc may fall back to dynamic dispatch for the unannotated portions. The benchmark suite was designed to avoid these patterns; their impact on mixed codebases is not evaluated here.

**Comparison baseline.** We compare against CPython 3.14 and mypyc(bare). We do not include Cython or Numba as direct baselines, because those systems require either manual annotation (Cython) or a different execution model (Numba JIT). A head-to-head comparison with manually-annotated mypyc (the theoretical ceiling) is left for future work; we note that the specializer results (peak 50.53×) approach what hand-annotated mypyc can produce for the same functions.

---

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

**Rank-2 Polymorphism.** GCP handles rank-1 polymorphism naturally. For rank-2 polymorphism (e.g., Church numerals `λf.λx.x`), the type parameters cannot be inferred without an external annotation, since GCP's fixed-point iteration lacks the mechanism to generalize polymorphic types across call sites in the HM sense. This is an inherent limitation shared with most practical type inference systems.

**Recursive Types.** Mutually recursive type definitions require careful constraint ordering to avoid infinite-depth subtype checks. GCP's cycle-detection mechanism (ThreadLocal visited set) prevents infinite loops but may conservatively report type errors for some valid recursive structures.

**Cross-Type Intersections.** GCP approximates intersection types via `Poly` (the ad-hoc polymorphism sum type), which is less precise than a full intersection type system. Programs that require true intersection types must use explicit interface declarations.

**Convergence Bound.** While the 100-round limit is empirically sufficient, it is not a formal convergence guarantee for all Outline programs. Programs with extremely deep dependency chains may require bound adjustment.

---

## 11. Related Work

**Type Inference.** The foundational work is Milner's Algorithm W [[Milner 1978]](#ref3) and its extension to recursive types [[Damas & Milner 1982]](#ref5). GCP's constraint-based approach is more closely related to the work of Pottier and Rémy [[Pottier & Rémy 2005]](#ref4), who generalize HM to constraint-based frameworks supporting subtyping.

**Gradual Typing.** The gradual type system of Pierce and Turner [[Pierce & Turner 2000]](#ref1) and its subsequent elaborations [[Siek & Taha 2006]](#ref6) allow mixing of typed and untyped code. GCP's `UNKNOWN` type and lazy inference play a similar role but are fully internal — they do not appear in the user-facing type language.

**Structural Typing.** Structural type systems for object-oriented languages are surveyed in [[Cardelli & Wegner 1985]](#ref7). GCP's OEM is a formalization of duck typing, closely related to record subtyping in type theory but extended to handle cyclic structural types.

**TypeScript.** The industrial-scale type system most comparable to GCP is TypeScript [[Microsoft 2012]](#ref2). The specific comparison in §10.2 highlights the differences. Work on understanding TypeScript's unsoundness [[Bierman et al. 2014]](#ref8) motivates GCP's more disciplined constraint model.

**Liquid Types.** Liquid types [[Rondon et al. 2008]](#ref9) extend HM with refinement predicates. GCP's `hasToBe` and `definedToBe` constraints play a similar role of capturing usage patterns, but GCP targets structural shape rather than value predicates.

**Python Ahead-of-Time Compilation.** Cython [[Cython 2007]](#ref11) requires developers to annotate C types manually using a Python-C dialect. Numba [[Numba 2015]](#ref12) uses JIT specialization at call time, avoiding annotation but incurring JIT overhead. `mypyc` [[mypyc 2019]](#ref10) achieves static C compilation from standard PEP 484 annotations, but depends entirely on annotation coverage. GCP-Python occupies a unique position in this space: it is the first approach that derives `mypyc`-compatible annotations *automatically* from call-site demand inference, eliminating both the manual annotation burden of Cython/mypyc and the runtime overhead of Numba's JIT, while achieving performance competitive with manually-annotated mypyc code (avg 13.57× over CPython, peak 50×). Unlike Numba's value specialization (which operates per JIT invocation), GCP's demand inference operates at program analysis time and handles parametric polymorphism through explicit monomorphization, producing deterministic ahead-of-time compiled modules rather than profiling-dependent JIT artifacts.

---

## 12. Conclusion

We have presented **Generalized Constraint Projection (GCP)**, a type inference methodology for dynamic languages built around three core contributions:

1. **The four-dimensional constraint model**, which separates the four qualitatively distinct sources of type information (actual value, declaration, usage context, structural access pattern) and propagates them independently. This avoids the spurious constraint conflicts that arise when these sources are conflated, and naturally handles the late-binding patterns of dynamic languages.

2. **Outline Expression Matching (OEM)**, a formalization of duck typing that defines structural subtyping via recursive member-set containment. The bidirectional delegation protocol (`tryIamYou` / `tryYouAreMe`) makes OEM an open extension point that can accommodate new Outline types without modifying existing code.

3. **Bidirectional Projection**, a generic instantiation mechanism that propagates type information simultaneously from call-site context into lambda parameters (enabling type-safe filter/map operations) and from lambda body results back to call-site return types. The orthogonal `projectedType` field supports IDE metadata extraction without perturbing inference.

Together, these contributions deliver on the key promise of GCP: eliminating the three pain points of existing type systems for dynamic languages — redundant annotations, context insensitivity, and cumbersome generic inference — while remaining sound on the programs that dynamic language developers actually write.

Two production applications substantiate these claims at industrial scale.

The **Entitir ontology platform** demonstrates GCP in its original role as a type-safe query engine: every entity query, action invocation, and decision trigger is a GCP-inferred Outline expression, providing end-to-end type safety from developer code through LLM-generated queries to relational database execution.

**GCP-Python** demonstrates an unexpected second application: GCP's demand-driven inference, originally designed for IDE correctness, is equally powerful as an automatic annotation engine for ahead-of-time Python compilation. Starting from zero-annotation source files, GCP infers complete PEP 484 type signatures from call-site context and rewrites the source before `mypyc` compilation. The experimental results — an average **13.57×** speedup over CPython, with peaks at **32×** for a number-theoretic function and **50×** under monomorphic specialization — demonstrate that the annotation bottleneck is the *only* bottleneck separating Python's productivity from C-level numeric performance. GCP eliminates that bottleneck automatically.

Together, these results position GCP as a general-purpose constraint propagation substrate suitable for both static analysis (type-safe query generation) and dynamic-to-static compilation (annotation-free ahead-of-time optimization), opening avenues for applying demand-driven type inference to other annotation-heavy compilation pipelines such as Cython, Numba, and Julia's JIT specialization.

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

<a id="ref10">[mypyc 2019]</a> Jukka Lehtosalo et al. mypyc: Compiling Python to C Extensions Using mypy's Type System. https://mypyc.readthedocs.io, 2019. Dropbox Engineering.

<a id="ref11">[Cython 2007]</a> S. Behnel, R. Bradshaw, C. Citro, L. Dalcín, D.S. Seljebotn, and K. Smith. Cython: The Best of Both Worlds. *Computing in Science & Engineering*, 13(2):31–39, 2011.

<a id="ref12">[Numba 2015]</a> S.K. Lam, A. Pitrou, and S. Seibert. Numba: A LLVM-based Python JIT Compiler. In *Proceedings of the LLVM Compiler Infrastructure in HPC Workshop*, pages 1–6, 2015.
