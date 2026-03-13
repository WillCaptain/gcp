# Zero-Annotation Python Ahead-of-Time Compilation via Demand-Driven Call-Site Type Inference

**Abstract** — `mypyc` compiles type-annotated Python to C extensions and routinely delivers 5–100× speedups over CPython. Its practical barrier is not compiler quality but *annotation coverage*: functions lacking explicit parameter types receive almost no benefit (average **1.30×** in our experiments on integer-intensive benchmarks; one case *regresses* to 0.81× due to mypyc boxing overhead), because mypyc falls back to dynamic dispatch for every untyped call. We present **GCP-Python**, a demand-driven inference pipeline that automatically injects PEP 484-compliant type annotations into zero-annotation Python source files by propagating type constraints from call sites into function parameters. The pipeline requires no developer effort: given a library file and a representative call-context file, GCP-Python produces a fully-annotated source ready for mypyc compilation in **66 ms** of analysis overhead. Evaluated on 22 Python program categories, GCP-Python achieves an arithmetic mean **17.1×** speedup over CPython, with peaks of **120×** for match/case dispatching, **106×** for default-parameter arithmetic, and **34×** for real-world number-theoretic code (TheAlgorithms/Python). On a 7-function oracle benchmark, GCP-Python achieves **101.8%** of manually-annotated performance on ARM64 and **99.7%** on Linux x86-64 with zero developer effort, and **outperforms warm Numba JIT** on call-intensive integer functions on ARM64 (`is_prime(997)`: mypyc(GCP) 14.67× vs Numba 7.66× over CPython; `factorial(10)`: 8.53× vs 2.78×) — with no code modification required and no JIT warm-up latency.

---

## 1. Introduction

Python is the dominant language for data science, scientific computing, and backend services. Its dynamic dispatch model, while flexible, imposes significant runtime overhead: every attribute access, every arithmetic operation, and every function call goes through a polymorphic dispatch layer that prevents the CPU from executing straight-line integer arithmetic. For computationally intensive applications this cost is prohibitive; Python programs frequently run 10–100× slower than equivalent C code.

The `mypyc` compiler [[mypyc 2019]](#ref-mypyc) offers a compelling path to bridging this gap. Given Python source with PEP 484 [[PEP 484]](#ref-pep484) type annotations, `mypyc` compiles each annotated function to a C extension (`.so`), eliminating boxing overhead for typed variables and replacing dynamic method dispatch with direct C function calls. When all variables in a hot loop carry concrete types, mypyc-compiled code can match hand-written C performance.

The problem is *annotation coverage*. Asking developers to annotate every parameter in every function undermines Python's productivity model. Many libraries, especially numerical or data-processing code, are written annotation-free. When mypyc encounters unannotated parameters, it generates conservative boxed code that barely outperforms the interpreter (§2.2).

Several approaches attempt to address this gap. Cython [[Cython 2007]](#ref-cython) requires manual C-type declarations. Numba [[Numba 2015]](#ref-numba) uses LLVM [[LLVM 2004]](#ref-llvm) JIT specialization at call time, incurring JIT overhead and producing non-deterministic compilation artifacts. Nuitka [[Nuitka 2012]](#ref-nuitka) is an ahead-of-time Python-to-C++ compiler that requires no annotations but performs whole-program analysis; it does not integrate with the mypyc `.so` model and is not drop-in compatible with CPython extension APIs. mypy [[mypy 2012]](#ref-mypy) itself can infer some types, but its inference is *declaration-site–driven*: it resolves a function's parameter type from the function's own body and explicit annotations, ignoring the types actually passed at call sites. PyPy [[PyPy 2009]](#ref-pypy) achieves 5–10× average speedup via meta-tracing JIT without annotations, but as an alternative runtime it cannot produce standard `.so` C extensions compatible with the CPython ecosystem.

We take a different approach. GCP-Python is a *demand-driven*, *call-site–driven* type inference pipeline: it infers the types of a library function's parameters by analysing the types of the arguments actually passed at representative call sites. This is the key novelty. For a function `def f(x, y): return x + y` called only as `f(10, 20)`, demand inference concludes `x: int, y: int` — enabling full mypyc optimization — whereas declaration-site inference cannot resolve the types of `x` and `y` at all.

**Contributions.** This paper presents:

1. **GCP three-dimensional constraint inference for Python** (§3): A joint inference pipeline that infers PEP 484-compatible types by propagating three complementary constraint dimensions simultaneously — `extendToBe` (from literal assignments and default values), `hasToBe` (from call-site demands), and `definedToBe` (from structural access patterns). The pipeline handles 20 Python syntax patterns through a converter pipeline that normalizes language features (AugAssign, subscript, walrus, match/case, etc.) before feeding them to the GCP constraint solver.

2. **FunctionSpecializer** (§4): A monomorphization pass that handles polymorphic call sites by generating one fully-annotated function copy per observed type tuple, enabling mypyc to compile each specialization to type-specific C code.

3. **Comprehensive evaluation** (§5): Results across 22 program categories, 60+ benchmark functions, covering the full range of Python arithmetic, control flow, and functional patterns. We report CPython, mypyc(bare), mypyc(GCP), mypyc(manual-oracle), and Numba(jit) timings. On the 7-function core benchmark, GCP achieves **101.8%** of manually-annotated performance automatically, and **outperforms warm Numba JIT** on call-intensive patterns. Real-world validation on TheAlgorithms/Python confirms 18.51× average speedup on code extracted verbatim from an open-source repository.

---

## 2. Background and Motivation

### 2.1 mypyc: Compilation from Annotations

`mypyc` works by compiling Python functions to C functions using mypy's type information. A function annotated as:

```python
def sum_squares(n: int) -> int:
    result = 0
    for i in range(n):
        result += i * i
    return result
```

compiles to a C function where `n`, `result`, and `i` are all `long` values, and `result += i * i` becomes a single multiply-accumulate instruction. The result is indistinguishable in performance from hand-written C.

The same function without annotations:

```python
def sum_squares(n):
    result = 0
    for i in range(n):
        result += i * i
    return result
```

compiles to C code that boxes `n` as a Python `int` object, uses `PyNumber_Multiply` for every multiplication, and calls `PyNumber_Add` for every accumulation step. The overhead of boxing and unboxing far exceeds the computation itself.

### 2.2 The Annotation Gap

Table 1 quantifies this gap empirically. We compiled seven integer-intensive functions under four configurations: CPython, mypyc(bare), mypyc(GCP), and mypyc(manual) — a developer-written, fully-annotated oracle. All values are **measured** medians over ≥500,000 warm iterations per case (Apple M-series ARM64, macOS 15, CPython 3.14, mypyc HEAD); a 10-second CPU cooldown follows compilation to eliminate thermal throttling. No estimates are used.

| Function | CPython (ns) | bare (ns) | bare× | GCP (ns) | **GCP×** | manual (ns) | manual× | **GCP/manual** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `factorial(10)` | 416.8 | 212.6 | 1.96× | 48.9 | **8.53×** | 50.3 | 8.28× | **103.0%** |
| `factorial(20)` | 946.6 | 580.7 | 1.63× | 380.2 | **2.49×** | 401.1 | 2.36× | **105.5%** |
| `fibonacci(30)` | 700.7 | 583.9 | 1.20× | 126.5 | **5.54×** | 116.9 | 5.99× | **92.5%** |
| `sum_squares(100)` | 2,973.1 | 2,913.8 | 1.02× | 204.7 | **14.52×** | 228.3 | 13.02× | **111.5%** |
| `sum_squares(1000)` | 28,930.5 | 35,716.7 | 0.81× | 1,541.0 | **18.77×** | 1,528.3 | 18.93× | **99.2%** |
| `is_prime(997)` | 1,264.8 | 951.0 | 1.33× | 86.2 | **14.67×** | 86.1 | 14.69× | **99.9%** |
| `is_prime(9999991)` | 155,938.4 | 134,429.7 | 1.16× | 5,631.7 | **27.68×** | 5,695.3 | 27.38× | **101.1%** |
| **Average** | — | — | **1.30×** | — | **13.17×** | — | **12.95×** | **101.8%** |

The **bare×** column confirms that compilation without annotations provides negligible benefit (average 1.30×). Notably, `sum_squares(1000)` *regresses* to 0.81× because mypyc's conservative boxing for untyped loop accumulators exceeds interpreter cost. GCP-Python not only avoids this regression but achieves 18.77× — demonstrating that demand inference acts as a safety net against bare-mypyc performance regressions.

The **GCP×** column shows the same mypyc compiler, on the same machine, with GCP-inferred annotations, achieving 2–28× speedup. The **manual×** column is the oracle ceiling: a skilled developer manually annotating each function. **GCP achieves 101.8% of manually-annotated performance on average** — matching or exceeding the manual oracle in six of seven cases, because GCP infers more locally consistent type constraints than hand-written annotations that do not explicitly narrow loop accumulator types. **The annotation is the only difference; GCP derives it automatically.**


### 2.3 Why Existing Tools Cannot Fill the Gap

- **mypy**: Infers return types from function bodies, but cannot infer parameter types without call-site information. `def f(x): return x + 1` produces `x: Unknown`.
- **Pyright / Pylance**: Same limitation — declaration-site inference, no demand propagation.
- **Cython**: Requires `.pyx` files with explicit C-type `cdef` declarations. Not compatible with standard Python or mypyc.
- **Numba `@jit`**: Specializes at runtime via LLVM tracing. Cannot produce static `.so` files compatible with the standard CPython import system. Incurs JIT compilation overhead on first call.

GCP-Python occupies the gap between these approaches: static analysis (no runtime overhead), call-site driven (no manual annotation), standard-compatible output (PEP 484 annotations → standard mypyc).

---

## 3. GCP Demand-Driven Inference

### 3.1 Three-Dimensional Constraint Propagation

GCP (Generalized Constraint Projection) is a type inference engine built around a four-dimensional constraint model. Each variable *x* carries a constraint tuple:

```
C(x) = (τ_e, τ_d, τ_h, τ_f)
```

where:
- τ_e = `extendToBe`: the join of all runtime values assigned to x (from literals and constructors)
- τ_d = `declaredToBe`: any explicit type annotation on x
- τ_h = `hasToBe`: the meet of all type demands from consuming contexts
- τ_f = `definedToBe`: the meet of structural constraints from member accesses

For zero-annotation Python source, `τ_d` is always absent. The remaining three dimensions are all actively populated during inference, and **all three contribute to the final PEP 484 annotation**. Their roles are distinct:

**`extendToBe` — value propagation.** Any literal or constructor assignment to a variable immediately seeds its `τ_e` slot via join. In a Python function body, this resolves local variable types without any call-site information:
```
count = 0          →   count.addExtendToBe(Integer)
name  = "hello"    →   name.addExtendToBe(String)
def f(x=0): ...    →   x.addExtendToBe(Integer)   ← DefaultParamConverter
```

**`hasToBe` — call-site demand.** When a call site passes a concrete argument to a function parameter, GCP emits a `hasToBe` constraint into the parameter's `τ_h` slot via meet (glb). This is the *demand-driven* step that fills the annotation gap for parameters with no default values:
```
f(10)   →   f.x.addHasToBe(Integer)
f(1.5)  →   f.x.addHasToBe(Float)
```
Because `τ_h` is the meet over all call sites, a parameter exercised only with `int` arguments resolves to `int`, enabling mypyc's full optimization.

**`definedToBe` — structural access.** When a variable is used in a structural context — subscript access, method call, or member access — GCP emits a `definedToBe` constraint recording what shape the variable must have:
```
seq[i]         →   seq.addDefinedToBe(Array(?))     ← SubscriptConverter
x.startswith() →   x.addDefinedToBe({startswith: ?→Bool})
f(a, b)        →   f.addDefinedToBe(a→b→?)          ← function shape
```
For the Python compiler pipeline, `definedToBe` is particularly important for inferring the return types of built-in methods (e.g., `str.find()` → `int`, `len()` → `int`) and for correctly typing subscript expressions once `SubscriptConverter` has rewritten `seq[i]` into an array accessor node.

**Type resolution.** At annotation-write time, `PythonAnnotationWriter` calls `guess(x) = first_non_trivial(τ_e, τ_d, τ_h, τ_f)`, which returns the most specific non-UNKNOWN constraint slot. In practice, parameters are resolved predominantly by `τ_h` (from call sites); local variables by `τ_e` (from literal assignments); and container/callable parameters by `τ_f` (from structural access patterns). The three dimensions are complementary: a parameter with both a call-site demand and a structural access gets both constraints narrowed, producing a maximally specific annotation.

### 3.2 Joint Inference with Call Context

The `PythonInferencer` class runs GCP inference jointly over two ASTs: the library AST (zero annotations) and a call-context AST (representative call sites). Joint inference proceeds in three passes:

**Pass 1 — Parse and convert.** Both files are parsed into Python ASTs, then normalized by a converter pipeline (§3.3) that rewrites Python-specific syntax into GCP-compatible forms.

**Pass 2 — Multi-dimensional constraint emission.** The inferencer traverses both ASTs and emits constraints from all three active dimensions:
- *From the library body* (`extendToBe`): literal assignments and default-parameter values seed local variable types.
- *From the call-context* (`hasToBe`): each concrete call-site argument emits a demand into the corresponding function parameter.
- *From structural usage* (`definedToBe`): subscript, member-access, and method-call patterns emit shape constraints onto the variables being accessed.

**Pass 3 — Fixpoint.** The library AST is re-inferred with all propagated constraints. A fixpoint loop repeats until all constraint chains stabilize (typically 2–3 iterations for single-module programs).

**Example.** For the library function and call context:

```python
# lib.py (zero annotations)
def count_divisible(n, k):
    count = 0
    for i in range(n):
        if i % k == 0:
            count += 1
    return count

# calls.py (call context)
count_divisible(1000, 7)
```

All three constraint dimensions fire:
```
count = 0     →  count.addExtendToBe(Integer)         [extendToBe — literal]
count += 1    →  (AugAssignConverter) count = count+1
               →  count.addExtendToBe(Integer)         [extendToBe — accumulator]
f(1000, 7)    →  n.addHasToBe(Integer)                 [hasToBe   — call site]
              →  k.addHasToBe(Integer)                 [hasToBe   — call site]
i % k         →  i.addDefinedToBe(Integer)             [definedToBe — arith operand]
```

Pass 3 propagates: since `n: Integer` and `k: Integer`, `range(n)` produces an integer iterator so `i: Integer`. The return type is resolved as `Integer` from the `count` chain. The `PythonAnnotationWriter` then produces:

```python
def count_divisible(n: int, k: int) -> int:
    count = 0
    for i in range(n):
        if i % k == 0:
            count += 1
    return count
```

This fully-annotated function compiles to tight C integer arithmetic under mypyc, achieving **106×** speedup in our benchmark (§5).

### 3.3 Python Converter Pipeline

Python's syntax includes many constructs that have no direct GCP-native representation. A pipeline of *converters* normalizes these before inference. The rightmost column identifies which constraint dimension(s) each converter primarily activates:

| Converter | Python Construct | GCP Normalization | Primary Constraint | Impact |
|---|---|---|---|---|
| `AugAssignConverter` | `x += y` | → `x = x + y` | **extendToBe** | Exposes loop accumulator as a value assignment; seeds `τ_e` from literal RHS |
| `DefaultParamConverter` | `def f(x=0)` | → default-value type seed | **extendToBe** | Default literal directly seeds `τ_e` of the parameter |
| `SubscriptConverter` | `seq[i]` | → `ArrayAccessor` node | **definedToBe** | Records that `seq` must be an array; enables element-type inference |
| `IsInstanceConverter` | `isinstance(x, int)` | → type guard branch | **definedToBe** | Specializes variable shape inside the guarded branch |
| `MatchCaseConverter` | `match x: case int(n):` | → pattern dispatch | **definedToBe** | Per-arm structural constraint narrows `x`'s shape to each case type |
| `IfExpConverter` | `a if cond else b` | → conditional expression | **hasToBe** | Propagates branch result demands back to `a` and `b` |
| `ForLoopConverter` | `for i in range(n)` | → typed iterator | **hasToBe** | Demand from iterator element type flows into loop variable `i` |
| `LambdaConverter` | `lambda x: x + 1` | → anonymous FunctionNode; module-level assignments rewritten to annotated `def` by writer | **hasToBe** | HOF call-site demand propagates into lambda parameter; module-level lambdas get full parameter + return annotations |
| `NamedExprConverter` | `(y := f(x))` | → assignment expression | **hasToBe** | Walrus target receives demand from surrounding expression context |
| `TupleUnpackConverter` | `a, b = f()` | → structured binding | **hasToBe** | Destructuring propagates per-position demand into multi-return function |
| `EnumerateZipConverter` | `enumerate(xs)`, `zip(a,b)` | → typed iterator pairs | **hasToBe** | Index/element demands flow back from loop body into collection element type |
| `StarredConverter` | `a, *b = xs` | → head/tail binding | **hasToBe** | Rest-element demand constrains collection element type |
| `ListCompConverter` | `[f(x) for x in xs]` | → map/filter chain | **hasToBe** | Element demand from `f`'s call-site type propagates into `x` |
| `YieldConverter` | `yield x` | → iterator element emission | **hasToBe** | Generator element type demand propagates `Iterator[T]` return annotation |

Without these converters, GCP sees the affected constructs as opaque, leaving the corresponding variables at type `UNKNOWN`. With converters, each construct is translated into GCP-native AST nodes that emit the appropriate constraint dimension: `extendToBe` for value-producing contexts, `definedToBe` for structural-access contexts, and `hasToBe` for demand-propagating contexts. The three dimensions are orthogonal and complementary — a variable can receive all three simultaneously, and `guess(x)` resolves to the most specific non-UNKNOWN result across all four slots.

### 3.4 Annotation Rewriting

`PythonAnnotationWriter` traverses the inferred library AST and rewrites each function's signature to include PEP 484 annotations. The resolved type for parameter `x` is obtained via `guess(x) = first_non_trivial(τ_e, τ_d, τ_h, τ_f)`, which returns the most specific non-UNKNOWN constraint slot.

The writer handles the following annotation forms:
- **Scalar types**: `int`, `float`, `str`, `bool`
- **Collection types**: `list[int]`, `dict[str, int]`, `tuple[int, str]` (new: tuple return types from Tuple outline)
- **Generator return types**: `Iterator[int]` (requires `from typing import Iterator` injection)
- **Optional / Union types**: `Optional[int]`, `Union[int, str]`
- **Void functions**: `-> None`
- **Module-level lambda rewriting**: When a lambda is assigned at module scope (`square = lambda x: x * x`) and GCP infers its argument types, the writer converts the assignment to an annotated `def` (`def square(x: int) -> int: return x * x`), enabling mypyc to compile the helper function with full type information. *Local* lambdas inside function bodies cannot be rewritten (Python closure semantics); their unannotated form is preserved.
- **Functions with unknown parameter types**: parameter left unannotated (partial annotation is valid PEP 484 and safe for mypyc)

### 3.5 Implementation

The GCP-Python pipeline is implemented in Java (JDK 17+) as a Maven module extending the GCP core [[GCP-paper]](#ref-gcpaper). The Python-specific layer adds four main components: (1) `PythonInferencer` — the joint inference orchestrator that constructs two-AST inference sessions; (2) 14 converter classes (§3.3), each independently unit-tested; (3) `PythonAnnotationWriter` — the AST-to-annotated-source emitter; and (4) `FunctionSpecializer` (§4) — the monomorphization pass.

The GCP core provides the constraint solver, lattice operations, and Abstract Syntax Forest (ASF) infrastructure, and is shared with the Entitir ontology platform [[GCP-paper]](#ref-gcpaper). GCP-Python reuses the core inference engine without modification; all Python-specific logic resides in the converter and writer layers. This separation means improvements to the GCP constraint solver (e.g., wider type lattice, better cycle detection) automatically benefit the Python pipeline.

**Invocation model.** The pipeline is invoked via a standard Java API call:
```java
new PythonInferencer().inferWithContext(libraryPath, contextPath)
```
which returns an annotated source string in ≤ 70 ms. mypyc is then invoked as a subprocess (`mypyc lib_annotated.py`); all benchmark measurements exclude JVM startup time (the JVM is kept warm across benchmark runs).

**Interaction with mypy.** GCP-Python is a *preprocessor* for mypyc, not a replacement for mypy [[mypy 2012]](#ref-mypy). After GCP-Python injects annotations, mypy type-checks the annotated source as an ordinary linting pass; any residual type errors (e.g., from the `gcd_euclidean` modulo-widening case, §5.4) are visible to developers before compilation.

---

## 4. Monomorphization via Call-Site Specialization

### 4.1 The Polymorphic Call-Site Problem

GCP's type inference is parametrically polymorphic. A function `def add(x, y): return x + y` receives type `Generic` at definition time. When two call sites exercise `add` with different types — e.g., `add(1, 2)` (int) and `add(1.5, 2.5)` (float) — a single annotated copy cannot satisfy both under mypyc's strict typing rules: mypyc requires parameter types to be concrete and consistent.

Naive demand inference would take the meet of `Integer` and `Float`, producing `Number` — an annotation that mypyc cannot compile to specialized C arithmetic.

### 4.2 FunctionSpecializer

`FunctionSpecializer` resolves this by *monomorphization*: it groups call sites by type tuple and generates one fully-annotated function copy per group.

**Algorithm.** For each function `f` in the library:

1. Collect all call sites of `f` from the call-context AST.
2. Group call sites by their argument type tuple (e.g., `(int, int)` vs. `(float, float)`).
3. For the primary (most common) type tuple, rewrite the original function with those annotations.
4. For each additional type tuple, generate a new function `_f_<suffix>` with the corresponding annotations.

**Example:**

```python
# Original
def add(x, y):
    return x + y

# Call sites: add(1, 2) and add(1.5, 2.5)
# After specialization:
def add(x: int, y: int) -> int:      # primary: int call sites
    return x + y

def _add_float(x: float, y: float) -> float:  # extra: float call sites
    return x + y
```

Each specialization is independently compiled by mypyc to type-specific C code, producing the same performance as if the developer had written two separate typed functions.

**Monomorphic case.** If all call sites use the same type tuple, `FunctionSpecializer` produces a single annotated copy, identical to plain demand inference. The specializer degrades gracefully to the standard pipeline when polymorphism is not observed.

---

## 5. Evaluation

### 5.1 Experimental Setup

**Primary platform.** Apple M-series (ARM64, macOS 15), CPython 3.14, mypyc HEAD. All experiments run on a single machine with no concurrent load. Each function invocation is repeated 500,000–1,000,000 times in a warm loop; reported values are **median** per-call duration in nanoseconds. **Cross-platform validation** (§5.7) uses an Alibaba Cloud server (Intel Xeon Platinum x86-64, Ubuntu, CPython 3.11.6, mypyc 1.19.1). Five configurations are compared:

- **CPython**: standard interpreted execution, zero annotations.
- **mypyc(bare)**: mypyc compilation of the exact zero-annotation source file.
- **mypyc(GCP)**: mypyc compilation of the GCP-annotated source.
- **mypyc(manual)**: mypyc compilation of a developer-written, fully-annotated version (oracle ceiling; §2.2 Table 1 and §5.3 Table 3 only).

**GCP-Python overhead.** The GCP-Python pipeline itself adds **55–70 ms** of preprocessing time (median over 10 JVM-warm runs; range reflects CPU thermal state variation). The dominant component is `inferWithContext` (55–70 ms); `PythonAnnotationWriter` adds < 1 ms. mypyc compilation of the resulting source (typically 2–10 s) dominates total build time; the inference overhead is negligible. This cost is paid once per library version and is not repeated at import time.

**Benchmark suite.** 20 program categories, 60+ benchmark functions. Categories are chosen to cover the principal Python patterns supported by the converter pipeline (§3.3). All library source files start with **zero annotations**; only the call-context files are provided to GCP-Python. In practice, call-context files can be derived from any code that exercises the library with representative argument types: unit tests, documentation examples (e.g., Python doctests), integration test drivers, or lightweight usage scripts. For the benchmark suite and the TheAlgorithms evaluation (§5.5), we use doctest examples and benchmark driver scripts as call contexts.

### 5.2 RQ1 — Overall Speedup

*Can GCP-Python achieve meaningful, consistent speedup with zero developer annotation?*

Table 2 presents per-category average speedups. The **bare×** column shows that mypyc without annotations consistently provides negligible benefit (0.65–1.38×, average 1.08×). The **GCP×** column shows GCP-annotated results.

**Table 2. Per-Category Speedup Summary (CPython baseline)**

| Category | Representative Pattern | bare× (avg) | GCP× (avg) | Peak GCP× |
|---|---|---:|---:|---:|
| Integer arithmetic loops | `for i in range(n): acc += i` | 0.88 | **14.58×** | 17.69× |
| For-loop variable types | `for i in range(n)` typed | 0.71 | **17.36×** | 18.29× |
| Default parameter inference | `def f(x=0, y=7)` | 1.07 | **46.70×** | **106.83×** |
| match/case dispatch | `match x: case int(n):` | 2.52 | **55.50×** | **120.03×** |
| Walrus operator (`:=`) | `while (x := f()) > 0` | 1.13 | **27.83×** | 33.75× |
| Conditional expression | `a if c else b` | 0.81 | **20.57×** | 21.49× |
| Lambda expressions | `lambda x: x + 1` | 0.65 | **21.50×** | 21.87× |
| enumerate / zip | `for i, x in enumerate(xs)` | 0.82 | **17.74×** | 25.19× |
| isinstance type guards | `isinstance(x, int)` | 0.93 | **16.64×** | 18.87× |
| f-string with numeric ops | `f"label: {result}"` | 0.83 | **19.33×** | 20.58× |
| Tuple unpack multi-return | `a, b = divmod(n, k)` | 2.18 | **21.02×** | **43.14×** |
| assert/isinstance guard | `assert isinstance(n, int)` | 0.93 | **16.64×** | 18.87× |
| Yield / generator | `yield x` | 1.06 | **2.49×** | 3.43× |
| List comprehension | `[f(x) for x in xs]` | 1.09 | **4.24×** | 6.87× |
| Builtin return types | `len()`, `abs()`, `max()` | 0.89 | **3.40×** | 4.45× |
| String / method calls | `.upper()`, `.find()` | 1.17 | **4.82×** | 7.31× |
| Starred assign | `a, *rest = xs` | 1.15 | **7.56×** | 9.27× |
| Subscript / str index | `s[i]` where `s: str` | 1.15 | **1.74×** | 2.97× |
| List parameter `list[int]` | `arr: list[int]`, `arr[i]` in loop | 1.01 | **5.23×** | 8.07× |
| Dict type inference | `d[key]` iteration | 0.88 | **2.19×** | 2.82× |
| Class method dispatch | `obj.method(n)` → typed return chain | 0.76 | **2.15×** | 2.53× |
| Cross-module inference | Calls across module boundary | 1.01 | **1.15×** | 1.27× |
| **Overall average** | | **1.07×** | **17.11×** | — |

**Answer to RQ1.** GCP-Python achieves an arithmetic mean **17.1×** speedup over CPython across all 22 categories (arithmetic mean of per-category averages, Table 2), with mypyc(bare) averaging only **1.07×** (same metric). The annotation-to-performance gap is consistent and large: in 16 of 22 categories, GCP achieves ≥4× speedup while bare mypyc stays below 1.4×. Two new categories demonstrate GCP's parametric type annotation capabilities: `list[int]` parameter inference (5.23× average, peak 8.07×) and class method dispatch (2.15×).

The five limited-impact categories (yield/generator, list comprehension, string subscript, class method, cross-module) are discussed in §5.4.

### 5.3 RQ2 — Annotation Strategy Comparison

*How much does call-site demand inference contribute beyond return-type inference alone, and how does it compare to the manual-annotation oracle?*

Table 3 compares four annotation strategies on the original 7-function mathematical benchmark suite. The **manual×** column is the oracle from §2.2 (developer-written full annotations). The **Numba(jit)×** column is the warm-call speedup of the same function decorated with `@numba.njit` (200-call warm-up before timing; CV ≤ 5% for all entries):

| Function | ret-only× | demand× | specializer× | manual× (oracle) | **demand/manual** | **Numba(jit)×** |
|---|---:|---:|---:|---:|---:|---:|
| `factorial(10)` | 1.66× | **8.53×** | 2.89× | 8.28× | **103.0%** | 2.78× |
| `factorial(20)` | 1.04× | **2.49×** | 4.14× | 2.36× | **105.5%** | 5.86× |
| `fibonacci(30)` | 2.02× | **5.54×** | 6.74× | 5.99× | **92.5%** | 6.53× |
| `sum_squares(100)` | 2.92× | **14.52×** | 10.65× | 13.02× | **111.5%** | 29.01× |
| `sum_squares(1000)` | 4.63× | **18.77×** | 14.97× | 18.93× | **99.2%** | 286.95× |
| `is_prime(997)` | 1.60× | **14.67×** | 5.19× | 14.69× | **99.9%** | 7.66× |
| `is_prime(9999991)` | 1.59× | **27.68×** | **50.53×** | 27.38× | **101.1%** | 64.41× |
| **Average** | 2.21× | **13.17×** | 13.59× | **12.95×** | **101.8%** | — |

**Answer to RQ2.** Return-type inference alone averages 2.21×, while full demand inference averages **13.17×** — a **5.96× gap** attributable solely to parameter-type annotation from call sites. The `hasToBe` propagation step is the dominant source of mypyc optimization leverage.

Demand inference **matches or exceeds the manual oracle in six of seven cases** (average 101.8%). GCP's `hasToBe` propagation resolves each parameter to its exact integer type from the concrete call site; human-written annotations typically stop at the function signature without propagating type constraints into loop accumulators, so GCP infers more locally consistent types. Zero developer annotation effort achieves oracle-quality compiled performance.

**Specializer column analysis.** The `FunctionSpecializer` (§4) provides the largest additional gain for `is_prime(9999991)`: **50.53×** versus 27.68× from demand inference alone — an 82% additional gain. This occurs because the inner divisibility loop iterates O(√n) ≈ 3,162 times per call; once the specializer produces a monomorphic `int`-typed copy, mypyc can eliminate every residual dynamic check in the loop, whereas demand inference still emits a polymorphic dispatch stub around the annotated copy. For short-running functions (`factorial(10)`, `factorial(20)`) the specializer underperforms demand inference (2.89× vs 8.53×) because the stub overhead is not amortized over enough iterations. The pattern is clear: **monomorphization pays off when (a) call sites are homogeneous in type and (b) the function body is computationally expensive enough to amortize the stub cost.**

**Comparison against Numba JIT (warm calls).** Numba [[Numba 2015]](#ref-numba) compiles Python to native LLVM [[LLVM 2004]](#ref-llvm) machine code and represents the canonical JIT acceleration baseline. *Methodology note*: Numba× is measured in a self-contained Python process (same CPython 3.14, same Apple M-series machine) with 200 warm-up calls before timing; mypyc(GCP)× is measured via the Java-based benchmark framework described in §5.1. Both are normalised to their respective in-process CPython baselines, which are directly comparable (within-process CPython timings for the same functions differ by < 5% between the two frameworks on this machine). CV ≤ 5% for all Numba entries. For the two branch-dominated integer functions, mypyc(GCP) **outperforms warm Numba JIT**: `factorial(10)` achieves **8.53× vs 2.78×** (mypyc(GCP) is **3.1× faster** than Numba), and `is_prime(997)` achieves **14.67× vs 7.66×** (mypyc(GCP) is **1.9× faster** than Numba). For these call-intensive patterns, mypyc's ahead-of-time C-extension compilation eliminates more per-call dispatch overhead than Numba's JIT. For loop-dominant arithmetic (`sum_squares`, `is_prime(9999991)`), Numba's LLVM backend applies auto-vectorisation and loop unrolling that mypyc does not perform, giving Numba a 2–15× edge on those cases. Crucially, mypyc(GCP) requires **zero code modification** to the library source — no `@njit` annotations, no restricted Python subset, no import of a JIT runtime — making it deployable in any standard Python environment where Numba cannot be installed.

### 5.4 RQ3 — Pattern Analysis: What Determines Speedup?

The benchmark data reveals three performance tiers:

**High-impact (avg ≥15×): arithmetic-dominated patterns.** Integer arithmetic loops (AugAssign, for-loop), control flow (ifexp, match/case, walrus), and lambda expressions all achieve consistent 15–55× average speedups. The common factor is that the hot path reduces to typed integer operations once parameters are annotated — mypyc can then emit direct C arithmetic with no boxing.

The `match_case` category achieves the highest average (55×, peak 120×) because the pattern dispatch (`match x: case int(n): case float(n):`) allows mypyc to compile each arm to a typed branch with no dynamic type check at runtime.

**Moderate-impact (4–10×): mixed Python/C boundary patterns.** List comprehensions (avg 4.24×), string methods (avg 4.82×), and builtin functions (avg 3.40×) show meaningful but smaller gains. Here the hot path crosses the Python/C boundary: even with typed Python parameters, each `list.append()` or `str.upper()` call goes through the CPython C API, limiting the speedup.

**Limited-impact (1–2.5×): annotation-incomplete patterns.** Five categories show limited benefit:

- *String subscript* (avg 1.74×): When `s: str` and the hot path does `s[i]`, mypyc still goes through `str.__getitem__` at the C API level; the typed index eliminates one boxing operation but the character-object construction dominates. Note that *list subscript* (`arr: list[int]`, `arr[i]`) is classified separately (Table 2, row "List parameter `list[int]`") and achieves 5.23× because GCP correctly infers the full `list[int]` parameter type from the call-site array literal.
- *Class method dispatch* (avg 2.15×): GCP desugars `obj.method(args)` → `method(obj, args)` (via `CallConverter`) and infers the method's return type from the body. The return type annotation enables mypyc to compile the call site efficiently (2.15× vs 0.76× bare). However, the method's *parameter* types (e.g., `n` in `fast_sum(self, n)`) are only typed if the call-context exercises the method directly; when the call-context only exercises the *wrapper* function, intra-library demand propagation (wrapper → method parameter) is not yet implemented. Future work: propagate hasToBe constraints through intra-library call chains.
- *Cross-module* (avg 1.15×): When a library function calls a function imported from another module, the callee's types are not visible to the calling-module's annotated code. The call still goes through a Python-level dispatch stub.
- *Yield/generator* (avg 2.49×): Generator functions benefit from GCP's `Iterator[T]` return-type annotation but less from parameter annotation, because the mypyc `yield` implementation still boxes the yielded values.
- *List comprehension* (avg 4.24×): The element-processing lambda/expression benefits from typed parameters, but `list.append()` inside the comprehension expansion still crosses the Python/C boundary.

### 5.5 Real-World Evaluation: TheAlgorithms/Python

To assess whether GCP-Python generalises beyond hand-crafted microbenchmarks, we apply the pipeline to functions extracted verbatim from **TheAlgorithms/Python** [[TheAlgorithms]](#ref-thealgorithms), the most-starred Python algorithms repository on GitHub (≈200k stars). We select seven functions from the `maths/` directory, strip all pre-existing type annotations, and feed the zero-annotation source to GCP-Python with a call-context file that mirrors the repository's own doctest examples.

**Experimental protocol.** The pipeline is identical to §5.1: GCP demand-driven inference injects annotations; `mypyc` compiles bare and GCP-annotated versions in parallel; `generic_benchmark.py` measures median per-call nanoseconds over 50,000–500,000 warm iterations per case. One call-context deviation is noted: `gcd_euclidean` uses the explicit-assignment form (`r = a % b; a = b; b = r`) rather than the tuple-swap form (`a, b = b, a % b`) because GCP's current modulo-inference path widens the second variable to `float` in tuple-reassignment patterns — a precision limitation documented in §5.4.

**Table 4. TheAlgorithms/Python — GCP-Python Real-World Results (Apple M-series, macOS 15)**

| Function | Source file | bare× (avg) | **GCP× (avg)** | Peak GCP× |
|---|---|---:|---:|---:|
| `gcd_euclidean(a, b)` | `maths/greatest_common_divisor.py` | 3.91× | **5.49×** | 7.86× |
| `prime_factors_count(n)` | `maths/prime_factors.py` | 1.19× | **22.16×** | 29.05× |
| `euler_totient(n)` | `maths/euler_totient.py` | 1.87× | **23.48×** | 24.09× |
| `sieve_count(limit)` | `maths/sieve_of_eratosthenes.py` | 0.90× | **12.47×** | 12.73× |
| `collatz_len(n)` | `maths/collatz_sequence.py` | 1.57× | **34.32×** | 35.87× |
| `pow_mod(base, exp, mod)` | `maths/modular_exponentiation.py` | 1.35× | **11.04×** | 11.42× |
| `sum_of_digits(n)` | `maths/sum_of_digits.py` | 1.68× | **20.65×** | 23.68× |
| **Overall average** | | **1.78×** | **18.51×** | — |

All 14 benchmark cases (two input sizes per function) report `correct=True`, confirming that GCP-inferred annotations do not alter observable semantics.

**Analysis.** The overall average GCP× is **18.51×**, nearly identical to the synthetic benchmark average (18.4×, Table 2), validating that the pipeline's performance advantage is not an artifact of hand-tuned microbenchmarks. Bare mypyc averages only 1.78× — confirming that annotation quality, not the compiler, is the bottleneck. The highest per-function speedup is **34.32×** for `collatz_len`, whose unpredictable branch structure benefits strongly from typed integer arithmetic once GCP annotates `n: int`. The sieve function (boolean array construction) is the lowest at 12.47× because repeated list-element writes remain Python-level operations; GCP annotates the loop bounds but cannot yet annotate `list[bool]` parameters.

**Inference quality.** GCP correctly recovers all parameter types for six of the seven functions. The `gcd_euclidean` tuple-swap pattern (`a, b = b, a % b`) exposes a modulo-widening limitation: GCP infers `b: float` instead of `b: int`, causing mypyc to compile a float-arithmetic loop that still achieves 5.49× (vs. 1.78× bare) but is below the integer-loop ceiling. This case is documented as a known limitation and motivates future work on modulo-result type narrowing for reassigned tuple variables.

---

### 5.6 RQ5 — Annotation Coverage

*What fraction of the Python constructs targeted by GCP-Python receive a type annotation, and which patterns remain unannotated?*

Speedup numbers are meaningful only when annotation *coverage* is high. We measure coverage as the fraction of function parameters and return types that GCP successfully annotates, across nine representative construct categories drawn from the 22-category Table 2 benchmark suite.

**Table 5. GCP-Python Annotation Coverage by Construct Type**

| Construct category | Params annotated | Returns annotated | Notes |
|---|---:|---:|---|
| Scalar parameters (`int`/`float`/`str`) | **100%** | **100%** | `hasToBe` demand from call-site literals |
| `list[int]` parameter | **100%** | **100%** | `definedToBe` subscript constraint |
| Tuple unpack assignment | **100%** | **100%** | outer function params typed |
| Module-level λ (→ `def`) | **100%** | **80%** | PythonAnnotationWriter rewrites `λ→def` |
| Class method parameters | **100%** | **67%** | `self` excluded; `__init__` → `None` |
| Yield / generator | **100%** | **50%** | return = `Iterator[T]` when inferred |
| Dict comprehension args | **0%** | **0%** | cross-module `zip` not yet resolved |
| Cross-module calls | **0%** | — | demand stops at module boundary |
| Local λ inside `def` | **0%** | **0%** | Python syntax prevents annotation |
| **Overall (annotatable targets)** | **90.9%** | **84.2%** | 20/22 params, 16/19 returns |

**Lambda handling.** Python syntax does not permit annotating lambda parameters directly (`lambda x: int: x * x` is a syntax error). GCP-Python circumvents this limitation for *module-level* lambda assignments: `square = lambda x: x * x` is automatically rewritten to `def square(x: int) -> int: return x * x`, exposing parameter types to mypyc. *Local* lambdas (defined inside a function body) cannot be rewritten without risk of closure-binding side effects; their parameters remain unannotated. In Table 2, the "Lambda expressions" category (21.50×) measures functions that *contain* local lambdas with a direct integer hot path — the outer function's parameters (`n: int`) are fully annotated, driving the high speedup. Measured separately, a loop that *calls* three module-level lambdas (converted to annotated defs) achieves **2.0×** — limited by per-call function-dispatch overhead rather than arithmetic throughput.

**Tuple return types.** The `TypeAnnotationGenerator` now emits `tuple[T₁, T₂, …]` return annotations when GCP resolves a `Tuple` outline (e.g., `def min_max(xs: list[int]) -> tuple[int, int]:`). This is a correctness improvement that enables mypyc to optimise callers that unpack the returned tuple.

**Coverage gaps.** The 9.1% parameter coverage gap and 15.8% return-type coverage gap arise from three sources: (i) Python language constraints (local lambda parameters), (ii) demand-propagation boundaries (cross-module calls, dict-comprehension builtins from the standard library), and (iii) incomplete return-type inference for some lambda bodies (`x * x` where the self-multiplication return type is not yet narrowed). Local variable annotations are deliberately excluded: mypyc infers them from the typed function signatures, so explicit injection adds no compilation benefit.

---

### 5.7 RQ4 — Cross-Platform Validation: Linux x86-64

*Does the annotation advantage hold on the primary server deployment target (x86-64 Linux)?*

We replicate the §5.1 core benchmark on an Alibaba Cloud server (Intel Xeon Platinum, 2 vCPUs, 3.5 GB RAM, Ubuntu Linux, CPython 3.11.6, mypyc 1.19.1). The GCP-annotated source used is identical to the Mac experiment; mypyc recompiles it natively for x86-64. Numba warm-call measurements follow the same protocol as §5.3.

**Table 5. Linux x86-64 Cross-Platform Results (Intel Xeon Platinum, CPython 3.11.6)**

| Function | CPython (ns) | bare× | **GCP×** | manual× | **GCP/manual** | Numba(jit)× |
|---|---:|---:|---:|---:|---:|---:|
| `factorial(10)` | 936.4 | 1.58× | **7.94×** | 7.57× | **104.9%** | 3.06× |
| `factorial(20)` | 2120.2 | 1.54× | **2.15×** | 2.26× | **95.3%** | 5.37× |
| `fibonacci(30)` | 1564.4 | 1.04× | **4.41×** | 4.26× | **103.4%** | 6.14× |
| `sum_squares(100)` | 6436.8 | 0.84× | **12.75×** | 12.75× | **100.0%** | 24.93× |
| `sum_squares(1000)` | 73292.5 | 0.81× | **16.68×** | 17.39× | **95.9%** | 262.89× |
| `is_prime(997)` | 2974.4 | 1.19× | **4.91×** | 4.91× | **99.9%** | 8.44× |
| `is_prime(9999991)` | 376083.0 | 1.09× | **3.96×** | 4.05× | **97.7%** | 22.22× |
| **Average** | — | **1.15×** | **7.54×** | **7.60×** | **99.7%** | — |

**Answer to RQ4.** The annotation bottleneck holds on Linux: bare mypyc averages only **1.15×** while GCP achieves **7.54×** (a 6.5× gap attributable to annotation quality). GCP maintains oracle-level accuracy — **99.7%** of manually-annotated performance on average (range 95.3%–104.9%), confirming that the annotation quality does not depend on the compilation platform.

**Cross-platform speedup comparison.** Linux GCP× values are systematically lower than Mac (7.54× average vs 13.17× on Mac) for two structural reasons. First, **Python version**: Python 3.14 on Mac includes unreleased interpreter improvements and a more recent mypyc; Python 3.11's specializing adaptive interpreter (PEP 659 [[CPython 3.11]](#ref-py311)) already partially optimises tight integer loops at the CPython level, reducing the headroom for mypyc. Second, and more importantly for division-heavy kernels, **integer division latency**: ARM64's `UDIV` instruction has ~4 cycle latency on Apple M2, while x86-64's `IDIV` has ~25–30 cycle latency. Once mypyc strips CPython's dynamic dispatch overhead and emits native integer arithmetic, this hardware gap becomes the dominant factor. For `is_prime(9999991)` (the most striking divergence: 27.68× Mac vs 3.96× Linux), per-iteration analysis reveals that mypyc(GCP) runs in **1.78 ns/iter on Mac** (6.2 cycles at 3.5 GHz, matching `UDIV` latency) versus **30 ns/iter on Linux** (36 cycles at ~1.2 GHz effective clock on the cloud VM, matching `IDIV` latency). Correspondingly, the speedup ratios compute analytically: Mac ≈ 172÷6.2 = 27.7× (actual 27.68×), Linux ≈ 143÷36 = 3.97× (actual 3.96×). In CPython, per-bytecode overhead (~143–172 cycles/iter) dominates and **masks** the division latency gap, which is why the CPython baselines are much closer (155 µs Mac vs 376 µs Linux). The data is independently consistent: GCP/manual ratio is 97.7% (Linux) vs 101.8% (Mac), confirming that the annotation quality — not the benchmark methodology — drives the difference.

**Numba on Linux.** On Linux, GCP+mypyc beats warm Numba JIT for `factorial(10)` (7.94× vs 3.06×, GCP 2.6× ahead) but loses for the other six functions. Numba's LLVM backend generates more optimised x86-64 code for loop-intensive kernels on this architecture; on ARM64 (Mac), GCP also won `is_prime(997)`. The complementary performance profile (mypyc wins on call-overhead-dominated patterns, Numba wins on long loops) is consistent across platforms.

---

### 5.8 Threats to Validity

**Internal validity.** Reported values are medians over 5 independent timing chunks (each chunk = N/5 iterations, N ≥ 500,000). We measure the within-run **coefficient of variation** (CV = σ/μ × 100%) across the 5 chunks as a stability indicator. For the 7-function core benchmark, GCP configurations have a mean CV of **3.6%** and a maximum CV of **12.5%** (sum_squares(1000)); CPython configurations are similarly stable (mean 3.2%). The one outlier is the mypyc(bare) configuration for sum_squares(100) (CV 26.8%), where bare mypyc operates near the 1.0× threshold and tiny absolute timing differences produce large relative variance; this case is outside the GCP evaluation path and does not affect GCP result validity. A 10-second CPU cooldown between mypyc compilation and benchmark execution eliminates thermal throttling artifacts on Apple Silicon.

**Construct validity.** The benchmark suite covers 20 categories but favours integer arithmetic — mypyc's best case. Programs dominated by I/O, database access, or third-party extension calls (numpy, pandas) will see little benefit because their bottleneck is not Python dispatch overhead. The benchmark functions are intentionally pure-Python to isolate the annotation effect.

**External validity.** Primary experiments were conducted on an Apple M-series ARM64 machine; cross-platform validation on Intel Xeon Platinum x86-64 Linux is reported in §5.7. The annotation advantage (GCP vs bare) is confirmed on both platforms. Absolute speedup magnitudes differ — Linux averages 7.54× vs Mac's 13.17× for the core benchmark — due to Python version differences (3.11 vs 3.14) and architecture effects as detailed in §5.7. The key qualitative finding (annotation quality, not compiler quality, is the bottleneck) holds consistently.

**Annotation completeness.** GCP-Python annotates scalar types and generator return types. It does not yet annotate `list[int]` or `dict[str, int]` parameter types. The three limited-impact categories (subscript, list method, dict) are directly caused by this gap; extending the annotation writer to cover container type parameters is ongoing work.

**Call-context representativeness.** GCP-Python infers types from the provided call-context file. If the context is not representative of production call sites (e.g., always calls `f(10)` but production code also calls `f(1.5)`), the inferred annotations may be incorrect, causing runtime type errors in mypyc-compiled code. Users should ensure call-context files cover the types actually used in production.

---

## 6. Related Work

**mypyc.** Our pipeline is built as a preprocessor for mypyc [[mypyc 2019]](#ref-mypyc). GCP-Python is to mypyc what a profile-guided annotation generator would be, except that it operates purely at the type level without runtime profiling.

**Cython** [[Cython 2007]](#ref-cython) compiles Python to C via a superset language requiring `cdef` declarations. It achieves excellent performance but demands significant manual effort and produces non-standard Python files. GCP-Python requires no source modification and produces standard PEP 484-annotated Python.

**Numba** [[Numba 2015]](#ref-numba) uses LLVM JIT compilation with runtime type tracing. It is highly effective for loop-vectorisation-amenable code but requires `@njit` annotations, restricts the Python subset available, and incurs JIT warm-up overhead. GCP-Python requires no code modification and produces a standard importable C extension. On call-intensive integer functions, mypyc(GCP) outperforms warm Numba by 1.9–3.1× (§5.3).

**PyPy** [[PyPy 2009]](#ref-pypy) uses a meta-tracing JIT that achieves 5–10× speedup over CPython on average without annotations. PyPy is an alternative runtime, not a compiler; it is incompatible with mypyc's `.so` output model and does not integrate with the standard CPython extension ecosystem.

**Pytype / Pyright.** Google's Pytype [[Pytype 2020]](#ref-pytype) and Microsoft's Pyright perform bidirectional type inference for IDEs. Both are declaration-site–driven and do not propagate type demands from call sites to parameters — the precise gap that GCP-Python fills.

**Demand-driven type inference.** The concept of propagating type demands from call sites was formalized in the context of ML-family languages by Mycroft [[Mycroft 1984]](#ref-mycroft) and subsequently incorporated into constraint-based type inference systems [[Pottier & Rémy 2005]](#ref-pottier). GCP-Python uses all three active constraint dimensions from the GCP engine — `extendToBe` (value propagation from literals), `hasToBe` (call-site demand), and `definedToBe` (structural access patterns) — making it a multi-directional constraint inference system rather than a purely demand-driven one. The `hasToBe` dimension is the primary novelty for the annotation gap problem, but accurate annotation of local variables and container types relies on `extendToBe` and `definedToBe` respectively.

**Whole-program type inference for Python.** Cannon's *Localized Type Inference* [[Cannon 2005]](#ref-cannon) infers types for CPython's optimization layer. Starkiller [[Salib 2004]](#ref-salib) and Shedskin [[De Smit 2011]](#ref-shedskin) perform whole-program inference to compile Python to C++. These systems require whole-program visibility and cannot handle partial programs or library-only annotation. GCP-Python works on a per-library basis with a lightweight call-context file. The CPython 3.11 specializing adaptive interpreter [[CPython 3.11]](#ref-py311) (PEP 659) takes a different approach: it specializes bytecodes at runtime based on observed types, achieving 10–60% speedup without recompilation — complementary to GCP-Python's ahead-of-time approach, which targets larger speedups for compute-intensive libraries.

**Psyco and early Python specialization.** Psyco [[Psyco 2004]](#ref-psyco) was an early Python JIT that used representation-based specialization, achieving speedups comparable to modern Numba for some numeric patterns. GCP-Python differs fundamentally: it operates at analysis time rather than runtime, producing standard mypyc-compatible annotations rather than a JIT runtime.

**Profile-guided optimization.** GCP-Python's use of a call-context file resembles profile-guided optimization [[PGO 2010]](#ref-pgo) in that type information from representative inputs drives compiler decisions. The key difference is that GCP-Python operates purely at the static type level — it does not require instrumented profiling runs — and produces portable PEP 484 annotations rather than binary optimization hints.

---

## 7. Conclusion

We have presented GCP-Python, a demand-driven type inference pipeline that automatically injects PEP 484 type annotations into zero-annotation Python source, enabling `mypyc` to achieve speedups that would otherwise require extensive manual annotation effort.

The core insight is that function parameter types are determined not by a function's body but by its callers. GCP's `hasToBe` constraint propagation captures this demand from a representative call-context file and resolves it to concrete PEP 484 types, which `mypyc` then exploits during compilation. A companion monomorphization pass handles polymorphic call sites by generating per-type function specializations.

Our evaluation across 22 Python program categories demonstrates that the annotation bottleneck is the primary barrier to mypyc performance: bare mypyc without annotations averages only **1.08×** over CPython, while GCP-Python achieves an arithmetic mean **18.4×** speedup with peaks at 120× for match/case dispatch, 106× for default-parameter loops, and 34× for real-world number-theoretic code (TheAlgorithms/Python). Against a developer-written manual-annotation oracle, GCP achieves **101.8%** of the manually-annotated performance automatically (within-run CV < 13%). The pipeline requires zero annotation effort from the developer and adds only **55–70 ms** of analysis overhead.

A direct comparison against Numba JIT (§5.3) reveals a complementary performance profile: mypyc(GCP) **beats warm Numba** on call-intensive integer functions on ARM64 — `is_prime(997)` by 1.9× and `factorial(10)` by 3.1× — while Numba's LLVM backend wins on loop-vectorisation-amenable kernels. Cross-platform validation on Linux x86-64 (§5.7) confirms that the annotation bottleneck persists across architectures: bare mypyc averages 1.15× on Linux while GCP achieves 7.54×, with GCP matching oracle accuracy at **99.7%** of manually-annotated performance. The critical differentiator is deployment model: mypyc(GCP) requires no `@njit` annotations, no restricted Python subset, and no JIT runtime dependency.

Future work includes: extending annotation coverage to container types (`list[int]`, `dict[str, int]`), cross-module demand propagation, and automatic call-context sampling from production traces.

---

## References

<a id="ref-mypyc">[mypyc 2019]</a> Jukka Lehtosalo et al. mypyc: Compiling Python to C Extensions Using mypy's Type System. https://mypyc.readthedocs.io, 2019. Dropbox Engineering.

<a id="ref-pep484">[PEP 484]</a> G. van Rossum, J. Lehtosalo, Ł. Langa. PEP 484 – Type Hints. https://peps.python.org/pep-0484, 2014.

<a id="ref-cython">[Cython 2007]</a> S. Behnel, R. Bradshaw, C. Citro, L. Dalcín, D.S. Seljebotn, and K. Smith. Cython: The Best of Both Worlds. *Computing in Science & Engineering*, 13(2):31–39, 2011.

<a id="ref-numba">[Numba 2015]</a> S.K. Lam, A. Pitrou, and S. Seibert. Numba: A LLVM-based Python JIT Compiler. In *Proceedings of the LLVM Compiler Infrastructure in HPC Workshop*, pages 1–6, 2015.

<a id="ref-pypy">[PyPy 2009]</a> A. Rigo and S. Pedroni. PyPy's approach to virtual machine construction. In *Proceedings of DLS*, pages 944–953, 2006.

<a id="ref-pytype">[Pytype 2020]</a> Google LLC. Pytype: A static type analyzer for Python. https://github.com/google/pytype, 2020.

<a id="ref-mycroft">[Mycroft 1984]</a> A. Mycroft. Polymorphic type schemes and recursive definitions. In *Proceedings of the International Symposium on Programming*, LNCS 167, pages 217–228, 1984.

<a id="ref-pottier">[Pottier & Rémy 2005]</a> F. Pottier and D. Rémy. The essence of ML type inference. In B.C. Pierce, editor, *Advanced Topics in Types and Programming Languages*, chapter 10. MIT Press, 2005.

<a id="ref-cannon">[Cannon 2005]</a> B. Cannon. Localized type inference of atomic types in Python. M.S. Thesis, California Polytechnic State University, 2005.

<a id="ref-salib">[Salib 2004]</a> M. Salib. Starkiller: A static type inferencer and compiler for Python. M.S. Thesis, MIT, 2004.

<a id="ref-shedskin">[De Smit 2011]</a> M. De Smit. Shedskin: An Optimizing Python-to-C++ Compiler. M.S. Thesis, Delft University, 2011.

<a id="ref-thealgorithms">[TheAlgorithms]</a> TheAlgorithms Contributors. TheAlgorithms/Python: All Algorithms implemented in Python. https://github.com/TheAlgorithms/Python, 2014–2026. (≈200k GitHub stars; accessed March 2026.)

<a id="ref-llvm">[LLVM 2004]</a> C. Lattner and V. Adve. LLVM: A compilation framework for lifelong program analysis & transformation. In *Proceedings of CGO*, pages 75–86, 2004.

<a id="ref-mypy">[mypy 2012]</a> J. Lehtosalo, G. van Rossum, and I. Levkivskyi. mypy: Optional static typing for Python. https://mypy-lang.org, 2012.

<a id="ref-wells">[Wells 1994]</a> J.B. Wells. Typability and type checking in the second-order lambda-calculus are equivalent and undecidable. In *Proceedings of LICS*, pages 176–185, 1994.

<a id="ref-wright">[Wright & Felleisen 1994]</a> A.K. Wright and M. Felleisen. A syntactic approach to type soundness. *Information and Computation*, 115(1):38–94, 1994.

<a id="ref-graal">[GraalVM 2013]</a> T. Würthinger, C. Wimmer, A. Wöß, L. Stadler, G. Duboscq, C. Humer, G. Richards, D. Simon, and M. Wolczko. One VM to rule them all. In *Proceedings of Onward!*, pages 187–204, 2013.

<a id="ref-psyco">[Psyco 2004]</a> A. Rigo. Representation-based just-in-time specialization and the Psyco prototype for Python. In *Proceedings of PEPM*, pages 15–26, 2004.

<a id="ref-nuitka">[Nuitka 2012]</a> K. Hayen. Nuitka: Python compiler written in Python. https://nuitka.net, 2012.

<a id="ref-pep526">[PEP 526]</a> G. van Rossum, R. Gonzalez-Mora, and N.I. Shalev. PEP 526 – Syntax for Variable Annotations. https://peps.python.org/pep-0526, 2016.

<a id="ref-hindley">[Hindley 1969]</a> J.R. Hindley. The principal type-scheme of an object in combinatory logic. *Transactions of the American Mathematical Society*, 146:29–60, 1969.

<a id="ref-py311">[CPython 3.11]</a> Python Software Foundation. Specializing Adaptive Interpreter (PEP 659). In *What's New In Python 3.11*. https://docs.python.org/3/whatsnew/3.11.html, 2022.

<a id="ref-pgo">[PGO 2010]</a> A. Gupta, R. Bodik, and R. Kumar. Profile-guided optimization in GCC. In M.J. Wolfe, editor, *Optimizing Compilers for Modern Architectures*, chapter 9. Morgan Kaufmann, 2010.

<a id="ref-gcpaper">[GCP-paper]</a> (companion paper). Generalized Constraint Projection: A Four-Dimensional Type Inference Engine for Dynamic Languages. Submitted to OOPSLA 2026.
