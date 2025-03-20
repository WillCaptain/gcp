# Generic Constraint Projection Method (GCP)
A unified methodology for type inference in dynamic programming languages, comprising three synergistic components:

### 1. Outline: Structural Type System
A runtime-informed type abstraction model that:
- Represents types as algebraic structures τ = ⟨properties|constraints⟩
- Captures object mutability through *extendable record types*
- Supports gradual typing via $unknown$ and $noghint$ type bounds

### 2. OEM (Object Equivalence Model)
Formalizes duck typing through structural compatibility checks:

$$ \text{Compat}(τ_S, τ_T) ≜ ∀(k:T_k) ∈ τ_T, ∃(k:S_k) ∈ τ_S \, | \, S_k ≺ T_k $$

- Implements nominal/structural hybrid matching
- Enforces function type contravariance:

````math
\frac{τ_{arg}^T ≺ τ_{arg}^S \quad τ_{ret}^S ≺ τ_{ret}^T}{(τ_{arg}^S→τ_{ret}^S) ≺ (τ_{arg}^T→τ_{ret}^T)}
````

### 3. GCP Constraint Resolution
A four-phase constraint chain for type synthesis:

````math
C = ⟨τ_e <: τ_d <: τ_h <: τ_f⟩
````

- **τₑ (extend_to_be)**: Contextual constraints from left-hand assignment

````javascript
let x: τ_e = e; // e's type must <: τ_e
````

- **τ_d (declared_to_be)**: Explicit type annotations

````typescript
function f(param: τ_d) {...}
````

- **τ_h (has_to_be)**: Right-value usage constraints

````javascript
e.memberAccess; // Requires e has τ_h
````

- **τ_f (defined_to_be)**: Concrete implementation signatures

**Resolution Workflow**:
1. Collect constraints through abstract interpretation
2. Verify compatibility via OEM rules
3. Project minimal viable type using:

````python
def project(τ_actual, τ_constraint):
    τ_result = glb(τ_actual, τ_constraint)
    return τ_result if τ_result ≠ never else raise Conflict
````

**Example Inference**:

````javascript
const pipe = (f, g) => x => g(f(x));

// Inference process:
1. f(x) requires x: τ_arg_f (τ_e from g(f(x)))
2. x[0] implies x: Array<τ_0> (τ_h constraint)
3. y * 2 requires y: number (τ_h constraint)
4. Final projection: pipe: <T>(T[] → T, T → number) → (T[] → number)
````

#### Method Advantages
- **Annotation-Free Inference**: Resolves types through usage context
- **Dynamic Type Evolution**: Handles prototype mutation and monkey patching
- **Conflict Detection**: Identifies type mismatches during constraint projection

This tripartite architecture enables practical static analysis for JavaScript, Python, and other dynamic languages while respecting their inherent flexibility.
