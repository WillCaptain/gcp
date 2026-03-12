# Generalized Constraint Projection: A Progressive Type Inference Engine for Dynamic Languages

**Abstract** — Dynamic programming languages such as JavaScript and Python offer unmatched flexibility, but their lack of static type guarantees creates friction in large-scale development. Existing type systems, notably TypeScript, impose three persistent pain points: *redundant annotations* (types must be declared even when they are obvious from context), *context insensitivity* (the same variable cannot refine its type in different calling contexts), and *cumbersome generic inference* (higher-order functions require verbose type parameters). This paper presents **Generalized Constraint Projection (GCP)**, a progressive type inference methodology built around a four-dimensional constraint model, a bidirectional projection mechanism for generics, and a structural subtyping relation formalized as Outline Expression Matching (OEM). GCP achieves type convergence through iterative constraint propagation rather than Hindley–Milner unification, making it naturally suited to patterns common in dynamic languages: dynamic object extension, late-bound member access, and higher-order function composition. We describe the formal foundations of GCP, its implementation in the Outline language engine, its multi-module fixed-point inference algorithm, and its application as the type-safe query backbone of the Entitir ontology data platform.

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

**Theorem 7.1 (Monotone Convergence).** Under the assumption that all inference rules are monotone with respect to the constraint ordering (adding more constraints never removes previously derived constraints), the fixed-point iteration converges in at most O(N²) rounds, where N is the total number of Genericable variables in the program.

*Proof sketch.* Each Genericable variable's constraint chain narrows strictly in at most one dimension per round (by the monotone update semantics of §3.4). The total number of narrowings is bounded by the number of variables times the number of constraint dimensions (4). Across N variables, convergence is guaranteed within 4N rounds in the worst case. ∎

In practice, convergence typically occurs within 3–5 rounds.

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

---

## 12. Conclusion

We have presented **Generalized Constraint Projection (GCP)**, a type inference methodology for dynamic languages built around three core contributions:

1. **The four-dimensional constraint model**, which separates the four qualitatively distinct sources of type information (actual value, declaration, usage context, structural access pattern) and propagates them independently. This avoids the spurious constraint conflicts that arise when these sources are conflated, and naturally handles the late-binding patterns of dynamic languages.

2. **Outline Expression Matching (OEM)**, a formalization of duck typing that defines structural subtyping via recursive member-set containment. The bidirectional delegation protocol (`tryIamYou` / `tryYouAreMe`) makes OEM an open extension point that can accommodate new Outline types without modifying existing code.

3. **Bidirectional Projection**, a generic instantiation mechanism that propagates type information simultaneously from call-site context into lambda parameters (enabling type-safe filter/map operations) and from lambda body results back to call-site return types. The orthogonal `projectedType` field supports IDE metadata extraction without perturbing inference.

Together, these contributions deliver on the key promise of GCP: eliminating the three pain points of existing type systems for dynamic languages — redundant annotations, context insensitivity, and cumbersome generic inference — while remaining sound on the programs that dynamic language developers actually write.

The Entitir ontology platform demonstrates GCP at industrial scale: every entity query, action invocation, and decision trigger is a GCP-inferred Outline expression, providing end-to-end type safety from developer code through LLM-generated queries to relational database execution.

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
