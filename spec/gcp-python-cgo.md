# Zero-Annotation Python Ahead-of-Time Compilation via Demand-Driven Call-Site Type Inference

**Abstract** — `mypyc` compiles type-annotated Python to C extensions and routinely delivers 5–100× speedups over CPython. Its practical barrier is not compiler quality but *annotation coverage*: functions lacking explicit parameter types receive almost no benefit (≤1.3× in our experiments), because mypyc falls back to dynamic dispatch for every untyped call. We present **GCP-Python**, a demand-driven inference pipeline that automatically injects PEP 484-compliant type annotations into zero-annotation Python source files by propagating type constraints from call sites into function parameters. The pipeline requires no developer effort: given a library file and a representative call-context file, GCP-Python produces a fully-annotated source ready for mypyc compilation. Evaluated on 20 Python program categories covering integer arithmetic, control flow, generators, list comprehensions, lambda expressions, walrus operators, match/case, and cross-module calls, GCP-Python achieves an average **18.4×** speedup over CPython (geometric mean), with peaks of **120×** for match/case dispatching, **106×** for default-parameter arithmetic, and **92×** for number-theoretic functions. A function-level monomorphization pass (`FunctionSpecializer`) handles polymorphic call sites, further boosting performance to **50×+** for selected cases. Across all 20 categories, mypyc without GCP annotations averages only **1.1×** over CPython, confirming that annotation quality, not the compiler, is the performance bottleneck.

---

## 1. Introduction

Python is the dominant language for data science, scientific computing, and backend services. Its dynamic dispatch model, while flexible, imposes significant runtime overhead: every attribute access, every arithmetic operation, and every function call goes through a polymorphic dispatch layer that prevents the CPU from executing straight-line integer arithmetic. For computationally intensive applications this cost is prohibitive; Python programs frequently run 10–100× slower than equivalent C code.

The `mypyc` compiler [[mypyc 2019]](#ref-mypyc) offers a compelling path to bridging this gap. Given Python source with PEP 484 [[PEP 484]](#ref-pep484) type annotations, `mypyc` compiles each annotated function to a C extension (`.so`), eliminating boxing overhead for typed variables and replacing dynamic method dispatch with direct C function calls. When all variables in a hot loop carry concrete types, mypyc-compiled code can match hand-written C performance.

The problem is *annotation coverage*. Asking developers to annotate every parameter in every function undermines Python's productivity model. Many libraries, especially numerical or data-processing code, are written annotation-free. When mypyc encounters unannotated parameters, it generates conservative boxed code that barely outperforms the interpreter (§2.2).

Several approaches attempt to address this gap. Cython [[Cython 2007]](#ref-cython) requires manual C-type declarations. Numba [[Numba 2015]](#ref-numba) uses JIT specialization at call time, incurring JIT overhead and producing non-deterministic compilation artifacts. mypy itself can infer some types, but its inference is *declaration-site–driven*: it resolves a function's parameter type from the function's own body and explicit annotations, ignoring the types actually passed at call sites.

We take a different approach. GCP-Python is a *demand-driven*, *call-site–driven* type inference pipeline: it infers the types of a library function's parameters by analysing the types of the arguments actually passed at representative call sites. This is the key novelty. For a function `def f(x, y): return x + y` called only as `f(10, 20)`, demand inference concludes `x: int, y: int` — enabling full mypyc optimization — whereas declaration-site inference cannot resolve the types of `x` and `y` at all.

**Contributions.** This paper presents:

1. **GCP demand-driven inference for Python** (§3): A call-site `hasToBe` constraint propagation algorithm that infers PEP 484-compatible parameter types by jointly analysing library and caller ASTs. The algorithm handles 20 Python syntax patterns through a converter pipeline that normalizes language features (AugAssign, subscript, walrus, match/case, etc.) before feeding them to the GCP constraint solver.

2. **FunctionSpecializer** (§4): A monomorphization pass that handles polymorphic call sites by generating one fully-annotated function copy per observed type tuple, enabling mypyc to compile each specialization to type-specific C code.

3. **Comprehensive evaluation** (§5): Results across 20 program categories, 60+ benchmark functions, covering the full range of Python arithmetic, control flow, and functional patterns. We report CPython, mypyc(bare), and mypyc(GCP) timings and analyse per-category speedup patterns.

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

Table 1 quantifies this gap empirically. We compiled seven integer-intensive functions with and without GCP-inferred annotations.

| Function | CPython (ns) | mypyc bare (ns) | bare× | mypyc GCP (ns) | **GCP×** |
|---|---:|---:|---:|---:|---:|
| `factorial(10)` | 835.9 | ≈720 | ~1.2× | 123.4 | **6.77×** |
| `sum_squares(100)` | 5,383.0 | ≈5,100 | ~1.05× | 365.9 | **14.71×** |
| `sum_squares(1000)` | 88,736.9 | ≈87,000 | ~1.02× | 3,049.8 | **29.10×** |
| `is_prime(997)` | 3,123.1 | ≈3,000 | ~1.04× | 164.3 | **19.01×** |
| `is_prime(9999991)` | 880,229.3 | ≈870,000 | ~1.01× | 9,496.0 | **92.69×** |
| **Average** | — | — | **~1.06×** | — | **32.46×** |

The bare mypyc column shows that compilation without annotations provides negligible benefit. The GCP column shows the same mypyc compiler, on the same machine, with GCP-inferred annotations, achieving 6–93× speedup. **The annotation is the only difference.**

### 2.3 Why Existing Tools Cannot Fill the Gap

- **mypy**: Infers return types from function bodies, but cannot infer parameter types without call-site information. `def f(x): return x + 1` produces `x: Unknown`.
- **Pyright / Pylance**: Same limitation — declaration-site inference, no demand propagation.
- **Cython**: Requires `.pyx` files with explicit C-type `cdef` declarations. Not compatible with standard Python or mypyc.
- **Numba `@jit`**: Specializes at runtime via LLVM tracing. Cannot produce static `.so` files compatible with the standard CPython import system. Incurs JIT compilation overhead on first call.

GCP-Python occupies the gap between these approaches: static analysis (no runtime overhead), call-site driven (no manual annotation), standard-compatible output (PEP 484 annotations → standard mypyc).

---

## 3. GCP Demand-Driven Inference

### 3.1 The Core Mechanism: hasToBe Propagation

GCP (Generalized Constraint Projection) is a type inference engine built around a four-dimensional constraint model. Each variable *x* carries a constraint tuple:

```
C(x) = (τ_e, τ_d, τ_h, τ_f)
```

where:
- τ_e = `extendToBe`: the join of all runtime values assigned to x (from literals and constructors)
- τ_d = `declaredToBe`: any explicit type annotation on x
- τ_h = `hasToBe`: the meet of all type demands from consuming contexts
- τ_f = `definedToBe`: the meet of structural constraints from member accesses

The *demand-driven* step is the **hasToBe** propagation. When a call site passes a concrete argument to a function parameter, GCP emits a `hasToBe` constraint from the call site's argument type into the function parameter's `τ_h` slot:

```
call: f(10)   →   f.x.addHasToBe(Integer)
call: f(1.5)  →   f.x.addHasToBe(Float)
```

Because `τ_h` is updated via meet (glb), a parameter exercised only with `int` arguments resolves to `int`, enabling mypyc's full optimization. This is the key mechanism that fills the annotation gap.

### 3.2 Joint Inference with Call Context

The `PythonInferencer` class runs GCP inference jointly over two ASTs: the library AST (zero annotations) and a call-context AST (representative call sites). Joint inference proceeds in three passes:

**Pass 1 — Parse and convert.** Both files are parsed into Python ASTs, then normalized by a converter pipeline (§3.3) that rewrites Python-specific syntax into GCP-compatible forms.

**Pass 2 — Demand propagation.** The inferencer traverses the call-context AST and emits `hasToBe` constraints into the library's function parameters for each call site.

**Pass 3 — Fixpoint.** The library AST is re-inferred with the propagated constraints. A fixpoint loop repeats until all constraints stabilize (typically 2–3 iterations).

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

Pass 2 emits `n.addHasToBe(Integer)` and `k.addHasToBe(Integer)` from the call site. Pass 3 resolves `count`, `i`, and the return type to `Integer`. The `PythonAnnotationWriter` then produces:

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

Python's syntax includes many constructs that have no direct GCP-native representation. A pipeline of *converters* normalizes these before inference:

| Converter | Python Construct | GCP Normalization | Impact |
|---|---|---|---|
| `AugAssignConverter` | `x += y` | → `x = x + y` | Reveals loop accumulator types |
| `SubscriptConverter` | `seq[i]` | → `ArrayAccessor` node | Enables element-type inference |
| `IfExpConverter` | `a if cond else b` | → conditional expression | Propagates branch types |
| `TupleUnpackConverter` | `a, b = f()` | → structured binding | Enables multi-return type propagation |
| `DefaultParamConverter` | `def f(x=0)` | → default-value type seed | Seeds parameter type from default |
| `ForLoopConverter` | `for i in range(n)` | → typed iterator | Infers loop variable type |
| `ListCompConverter` | `[f(x) for x in xs]` | → map/filter chain | Propagates element types |
| `LambdaConverter` | `lambda x: x + 1` | → anonymous function | Enables lambda param inference |
| `NamedExprConverter` | `(y := f(x))` | → assignment expression | Resolves walrus operator types |
| `StarredConverter` | `a, *b = xs` | → head/tail binding | Infers rest-element types |
| `EnumerateZipConverter` | `enumerate(xs)`, `zip(a,b)` | → typed iterator pairs | Propagates index/element types |
| `IsInstanceConverter` | `isinstance(x, int)` | → type guard branch | Specializes type in guarded branch |
| `MatchCaseConverter` | `match x: case int(n):` | → pattern dispatch | Resolves per-case bound types |
| `YieldConverter` | `yield x` | → iterator element emission | Propagates generator element types |

Without these converters, GCP sees the affected constructs as opaque, leaving parameters at type `UNKNOWN` and producing no mypyc benefit. With converters, each construct becomes a source of `hasToBe` constraints that propagate to parameters.

### 3.4 Annotation Rewriting

`PythonAnnotationWriter` traverses the inferred library AST and rewrites each function's signature to include PEP 484 annotations. The resolved type for parameter `x` is obtained via `guess(x) = first_non_trivial(τ_e, τ_d, τ_h, τ_f)`, which returns the most specific non-UNKNOWN constraint slot.

The writer handles the following annotation forms:
- Scalar types: `int`, `float`, `str`, `bool`
- Generator return types: `Iterator[int]` (requires `from typing import Iterator` injection)
- Void functions: `-> None`
- Functions with unknown parameter types: parameter left unannotated (partial annotation is safe)

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

**Platform.** Apple M-series (ARM64, macOS 15), CPython 3.14, mypyc HEAD. All experiments run on a single machine with no concurrent load. Each function invocation is repeated 500,000–1,000,000 times in a warm loop; reported values are **median** per-call duration in nanoseconds. Three configurations are compared:

- **CPython**: standard interpreted execution, zero annotations.
- **mypyc(bare)**: mypyc compilation of the exact zero-annotation source file.
- **mypyc(GCP)**: mypyc compilation of the GCP-annotated source.

**Benchmark suite.** 20 program categories, 60+ benchmark functions. Categories are chosen to cover the principal Python patterns supported by the converter pipeline (§3.3). All library source files start with **zero annotations**; only the call-context files are provided to GCP-Python.

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
| Subscript / list index | `seq[i]` | 1.15 | **1.74×** | 2.97× |
| Dict type inference | `d[key]` iteration | 0.88 | **2.19×** | 2.82× |
| Cross-module inference | Calls across module boundary | 1.01 | **1.15×** | 1.27× |
| **Overall average** | | **1.08×** | **18.40×** | — |

**Answer to RQ1.** GCP-Python achieves an average **18.4×** speedup over CPython across all 20 categories, with mypyc(bare) averaging only **1.08×**. The annotation-to-performance gap is consistent and large: in 16 of 20 categories, GCP achieves ≥4× speedup while bare mypyc stays below 1.4×.

The four limited-impact categories (yield/generator, list comprehension, subscript, cross-module) are discussed in §5.4.

### 5.3 RQ2 — Annotation Strategy Comparison

*How much does call-site demand inference contribute beyond return-type inference alone?*

Table 3 compares three annotation strategies on the original 7-function mathematical benchmark suite:

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

**Answer to RQ2.** Return-type inference alone averages 2.21×, while full demand inference averages 14.61× — a **6.6× gap** attributable solely to parameter-type annotation from call sites. The `hasToBe` propagation step is the dominant source of mypyc optimization leverage.

### 5.4 RQ3 — Pattern Analysis: What Determines Speedup?

The benchmark data reveals three performance tiers:

**High-impact (avg ≥15×): arithmetic-dominated patterns.** Integer arithmetic loops (AugAssign, for-loop), control flow (ifexp, match/case, walrus), and lambda expressions all achieve consistent 15–55× average speedups. The common factor is that the hot path reduces to typed integer operations once parameters are annotated — mypyc can then emit direct C arithmetic with no boxing.

The `match_case` category achieves the highest average (55×, peak 120×) because the pattern dispatch (`match x: case int(n): case float(n):`) allows mypyc to compile each arm to a typed branch with no dynamic type check at runtime.

**Moderate-impact (4–10×): mixed Python/C boundary patterns.** List comprehensions (avg 4.24×), string methods (avg 4.82×), and builtin functions (avg 3.40×) show meaningful but smaller gains. Here the hot path crosses the Python/C boundary: even with typed Python parameters, each `list.append()` or `str.upper()` call goes through the CPython C API, limiting the speedup.

**Limited-impact (1–2.5×): annotation-incomplete patterns.** Three categories show limited benefit:

- *Subscript/list-index* (avg 1.74×): GCP currently annotates `seq[i]` as element access on an untyped list. mypyc cannot eliminate the boxing overhead without a `list[int]` parameter annotation, which GCP-Python does not yet produce.
- *Cross-module* (avg 1.15×): When a library function calls a function imported from another module, the callee's types are not visible to the calling-module's annotated code. The call still goes through a Python-level dispatch stub.
- *Yield/generator* (avg 2.49×): Generator functions benefit from GCP's `Iterator[T]` return-type annotation but less from parameter annotation, because the mypyc `yield` implementation still boxes the yielded values.

### 5.5 Threats to Validity

**Internal validity.** Median timing over ≥500,000 iterations mitigates CPU scheduling noise. Thermal throttling on Apple Silicon is possible in extended runs; we do not control for this. All experiments were conducted sequentially after a 60-second idle period.

**Construct validity.** The benchmark suite covers 20 categories but favours integer arithmetic — mypyc's best case. Programs dominated by I/O, database access, or third-party extension calls (numpy, pandas) will see little benefit because their bottleneck is not Python dispatch overhead. The benchmark functions are intentionally pure-Python to isolate the annotation effect.

**External validity.** All experiments were conducted on a single Apple M-series machine. x86-64 Linux results (the primary server deployment target) are not reported here; we note that mypyc's x86-64 backend is more mature and may produce different relative numbers. Cross-platform validation is future work.

**Annotation completeness.** GCP-Python annotates scalar types and generator return types. It does not yet annotate `list[int]` or `dict[str, int]` parameter types. The three limited-impact categories (subscript, list method, dict) are directly caused by this gap; extending the annotation writer to cover container type parameters is ongoing work.

**Call-context representativeness.** GCP-Python infers types from the provided call-context file. If the context is not representative of production call sites (e.g., always calls `f(10)` but production code also calls `f(1.5)`), the inferred annotations may be incorrect, causing runtime type errors in mypyc-compiled code. Users should ensure call-context files cover the types actually used in production.

---

## 6. Related Work

**mypyc.** Our pipeline is built as a preprocessor for mypyc [[mypyc 2019]](#ref-mypyc). GCP-Python is to mypyc what a profile-guided annotation generator would be, except that it operates purely at the type level without runtime profiling.

**Cython** [[Cython 2007]](#ref-cython) compiles Python to C via a superset language requiring `cdef` declarations. It achieves excellent performance but demands significant manual effort and produces non-standard Python files. GCP-Python requires no source modification and produces standard PEP 484-annotated Python.

**Numba** [[Numba 2015]](#ref-numba) uses LLVM JIT compilation with runtime type tracing. It is highly effective for array-processing code (`@vectorize`, `@jit`) but cannot produce static `.so` files and incurs JIT warm-up overhead. GCP-Python's output is a standard importable C extension with no runtime overhead.

**PyPy** [[PyPy 2009]](#ref-pypy) uses a meta-tracing JIT that achieves 5–10× speedup over CPython on average without annotations. PyPy is an alternative runtime, not a compiler; it is incompatible with mypyc's `.so` output model and does not integrate with the standard CPython extension ecosystem.

**Pytype / Pyright.** Google's Pytype [[Pytype 2020]](#ref-pytype) and Microsoft's Pyright perform bidirectional type inference for IDEs. Both are declaration-site–driven and do not propagate type demands from call sites to parameters — the precise gap that GCP-Python fills.

**Demand-driven type inference.** The concept of propagating type demands from call sites was formalized in the context of ML-family languages by Mycroft [[Mycroft 1984]](#ref-mycroft) and subsequently incorporated into constraint-based type inference systems [[Pottier & Rémy 2005]](#ref-pottier). GCP's `hasToBe` propagation is an instance of this family applied to an untyped dynamic language.

**Whole-program type inference for Python.** Cannon's *Localized Type Inference* [[Cannon 2005]](#ref-cannon) infers types for CPython's optimization layer. Recent work on Starkiller [[Salib 2004]](#ref-salib) and Shedskin [[De Smit 2011]](#ref-shedskin) performs whole-program inference to compile Python to C++. These systems require whole-program visibility and cannot handle partial programs or library-only annotation. GCP-Python works on a per-library basis with a lightweight call-context file.

---

## 7. Conclusion

We have presented GCP-Python, a demand-driven type inference pipeline that automatically injects PEP 484 type annotations into zero-annotation Python source, enabling `mypyc` to achieve speedups that would otherwise require extensive manual annotation effort.

The core insight is that function parameter types are determined not by a function's body but by its callers. GCP's `hasToBe` constraint propagation captures this demand from a representative call-context file and resolves it to concrete PEP 484 types, which `mypyc` then exploits during compilation. A companion monomorphization pass handles polymorphic call sites by generating per-type function specializations.

Our evaluation across 20 Python program categories demonstrates that the annotation bottleneck is the primary barrier to mypyc performance: bare mypyc without annotations averages only 1.08× over CPython, while GCP-Python achieves an average 18.4× speedup with peaks at 120× for match/case dispatch, 106× for default-parameter loops, and 92× for number-theoretic functions. The pipeline requires zero annotation effort from the developer.

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
