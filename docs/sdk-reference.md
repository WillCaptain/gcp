# GCP + Outline SDK Reference

Complete API reference for the GCP type engine and Outline language front-end.

**Modules:**
- `gcp` (`org.twelve:gcp`) — Core engine: AST, type inference, interpreter, plugins, metadata
- `outline` (`org.example:outline`) — Language front-end: parses Outline source text into GCP ASTs

---

## Table of Contents

1. [OutlineParser — Parsing Source Code](#1-outlineparser--parsing-source-code)
2. [AST — Single-Module Program](#2-ast--single-module-program)
3. [Type Inference](#3-type-inference)
4. [Multi-Module Programs (ASF)](#4-multi-module-programs-asf)
5. [Executing Programs (OutlineInterpreter)](#5-executing-programs-outlineinterpreter)
6. [Value Types](#6-value-types)
7. [Error Handling](#7-error-handling)
8. [Plugins (GCPBuilderPlugin)](#8-plugins-gcpbuilderplugin)
9. [Configuration (GCPConfig)](#9-configuration-gcpconfig)
10. [Metadata](#10-metadata)
11. [Testing](#11-testing)

---

## 1. OutlineParser — Parsing Source Code

`org.twelve.outline.OutlineParser`

Converts Outline source text into a GCP `AST`. Grammar tables are compiled once per JVM and shared across all `OutlineParser` instances for performance.

### Two Modes

#### Isolated mode (single-module, most common)

Every `parse()` call creates a fresh independent `ASF`. Use for single-module programs, tests, and tooling.

```java
OutlineParser parser = new OutlineParser();

// Parse → returns a standalone AST (new ASF created internally each call)
AST ast = parser.parse("let x = 42; x + 1");
```

#### Shared-ASF mode (multi-module)

All `parse()` calls feed into the same `ASF`, enabling cross-module import/export resolution.

```java
ASF asf = new ASF();
OutlineParser parser = new OutlineParser(new GCPConverter(asf));

AST geo = parser.parse("namespace geo; export let PI = 3.14;");
AST app = parser.parse("import { PI } from geo; PI * 2.0");

// Both modules are now in `asf`, cross-references resolved during infer()
asf.infer();
```

### API

```java
// Isolated mode constructor
new OutlineParser()

// Shared-ASF mode constructor
new OutlineParser(GCPConverter converter)

// Parse source → AST (respects current mode)
AST ast = parser.parse(String code)

// Parse and append to a specific ASF (ignores mode; always creates fresh GCPConverter)
AST ast = parser.parse(ASF asf, String code)
```

---

## 2. AST — Single-Module Program

`org.twelve.gcp.ast.AST`

The core object representing one Outline module. Created by `OutlineParser.parse()`. Holds the node tree, symbol table, error list, and metadata.

### Program Structure

```java
// Add statements programmatically (when building ASTs without OutlineParser)
ast.addStatement(stmt);
ast.addImport(importNode);
ast.addExport(exportNode);

// Module identity
String name = ast.name();
String ns   = ast.namespace().lexeme();  // declared namespace, e.g. "geo"

// Source code (set by OutlineParser; needed by meta extraction)
ast.setSourceCode(src);
String src = ast.sourceCode();
```

### Built-in Type Singletons

```java
// Access primitive type singletons directly on AST
ast.String    // → STRING outline type
ast.Integer   // → INTEGER outline type
ast.Boolean   // → BOOL outline type
ast.Number    // → NUMBER outline type
ast.Unit      // → UNIT outline type
ast.Nothing   // → NOTHING (bottom type)
ast.Any       // → ANY (top type)
```

---

## 3. Type Inference

GCP uses **constraint propagation** rather than Hindley-Milner unification. Types are resolved through four constraint dimensions (`extendToBe`, `declaredToBe`, `hasToBe`, `definedToBe`) in a fixed-point iteration.

### Single-Module Inference

```java
// Trigger inference — fills the symbol table and resolves all node types
ast.infer();

// Check convergence — true if every node has a resolved type
boolean ok = ast.inferred();

// Nodes that could not be resolved (for diagnostics)
List<Node> unresolved = ast.missInferred();
```

### Multi-Module Inference

See [§4 — Multi-Module Programs](#4-multi-module-programs-asf).

### Constraint Dimensions

| Dimension | Source | Example |
|---|---|---|
| `extendToBe` | Assignment / projection target | `let x: Animal = dog` |
| `declaredToBe` | Explicit type annotation | `(c: Country) ->` |
| `hasToBe` | Usage context | `filter(c -> ...)` → c must be element type |
| `definedToBe` | Structural field access | `c.code` → c must have field `code` |

---

## 4. Multi-Module Programs (ASF)

`org.twelve.gcp.ast.ASF` — Abstract Syntax Forest

Coordinates multiple ASTs that share a `GlobalSymbolEnvironment`. Enables cross-module `import`/`export` with a fixed-point algorithm.

### Creating and Populating

```java
ASF asf = new ASF();

// Option A: create empty ASTs, fill programmatically
AST mod1 = asf.newAST();
AST mod2 = asf.newAST();

// Option B: parse directly into the ASF (recommended with OutlineParser)
OutlineParser parser = new OutlineParser(new GCPConverter(asf));
AST mod1 = parser.parse("namespace geo; export let radius = 6371;");
AST mod2 = parser.parse("import { radius } from geo; radius * 2");
```

### Inference and Execution

```java
// Fixed-point type inference across all modules
// Algorithm: (1) pre-register all module shells → (2) first-pass inference →
//            (3) iterate until convergence (default max 100 rounds)
boolean allConverged = asf.infer();
boolean done = asf.inferred();

// Collect all errors across all modules
List<GCPError> errors = asf.allErrors();
boolean hasErrors = asf.hasErrors();

// Execute all modules in insertion order
Value result = asf.interpret();                          // uses default interpreter
Value result = new OutlineInterpreter().run(asf);        // with custom interpreter
```

### Module Lookup

```java
AST geo = asf.get("geo");       // by module name; throws if not found
List<AST> all = asf.asts();    // all registered modules (insertion order)
GlobalSymbolEnvironment env = asf.globalEnv();  // shared cross-module symbol table
```

### Forest-Level Metadata

```java
ForestMeta meta = asf.meta();
meta.modules()              // List<ModuleMeta> — all modules
meta.find("geo")            // ModuleMeta for module "geo"
meta.find("geo").outlines() // Outline type declarations in geo
meta.toMap()                // JSON-serializable Map
```

---

## 5. Executing Programs (OutlineInterpreter)

`org.twelve.gcp.interpreter.OutlineInterpreter`

Tree-walking interpreter. Each node type has a corresponding stateless `Interpretation` class. The interpreter is **not thread-safe** — use one instance per execution.

### Creating

```java
// Standard: loads gcp.properties + auto-discovers plugins in plugin_dir
OutlineInterpreter interp = new OutlineInterpreter();

// With custom config
GCPConfig cfg = GCPConfig.load().with("plugin_dir", "/app/plugins");
OutlineInterpreter interp = new OutlineInterpreter(cfg);

// Pre-bound to an ASF (enables no-argument run())
OutlineInterpreter interp = new OutlineInterpreter(asf);
```

### Executing

```java
// Execute all modules in an ASF (in insertion order)
Value result = interp.run(asf);

// Execute a single module
Value result = interp.runAst(ast);

// Execute the pre-bound ASF (requires the ASF constructor)
Value result = interp.run();
```

| Method | Returns | Description |
|---|---|---|
| `run(ASF)` | `Value` | Execute all modules; returns last statement's value in last module |
| `runAst(AST)` | `Value` | Execute one module; handles import/export wiring |
| `run()` | `Value` | Execute the ASF bound at construction |

### Node-Level Evaluation

```java
// Evaluate a single AST node (used internally and in tests)
Value val = interp.eval(Node node);

// Apply a function value to an argument (curried application)
Value result = interp.apply(Value fn, Value arg);
```

### Environment Access

```java
Environment env = interp.env();
interp.setEnv(env);
Environment current = interp.currentEnv();  // during evaluation (test/debug)
```

---

## 6. Value Types

All runtime values implement `org.twelve.gcp.interpreter.value.Value`.

### Value Hierarchy

| Class | Outline type | Java representation |
|---|---|---|
| `IntValue` | `Int` / `Long` | `long` (pool-cached for −256 to 65535) |
| `FloatValue` | `Float` / `Double` / `Number` | `double` |
| `StringValue` | `String` | `java.lang.String` |
| `BoolValue` | `Bool` | singletons `BoolValue.TRUE` / `BoolValue.FALSE` |
| `UnitValue` | `Unit` | singleton `UnitValue.INSTANCE` |
| `ArrayValue` | `[T]` | mutable `List<Value>` |
| `DictValue` | `[K:V]` | `Map<Value,Value>` |
| `TupleValue` | `{field: T}` | immutable `Map<String,Value>` |
| `EntityValue` | named entity | field map + method registry |
| `FunctionValue` | `A -> B` | closure (with `Interpreter` reference) |
| `PolyValue` | `A \| B` | variant list |
| `PromiseValue` | async | future-like |

### Common Operations

```java
// Root interface
boolean truthy = value.isTruthy();
String  display = value.display();
Object  raw    = value.unwrap();         // unwrap to Java primitive

// IntValue
long n = ((IntValue) v).value();
IntValue iv = IntValue.of(42L);          // use factory (pool cache)

// StringValue
String s = ((StringValue) v).value();

// ArrayValue
ArrayValue av = (ArrayValue) v;
Value elem   = av.get(0);
int   size   = av.size();
List<Value> elems = av.elements();       // read-only snapshot

// EntityValue
EntityValue ev = (EntityValue) v;
Value field  = ev.get("name");
ev.setField("name", new StringValue("Alice"));
Map<String, Value> all = ev.allFields();
String tag = ev.symbolTag();             // outline type name, e.g. "Employee"

// FunctionValue — apply via interpreter, not directly
Value result = interp.apply(fn, arg);
```

---

## 7. Error Handling

`org.twelve.gcp.exception.GCPError`

### GCPError

```java
GCPError error = errors.get(0);

error.errorCode()                   // GCPErrCode enum value
error.node()                        // AST node where the error occurred
error.message()                     // human-readable detail string
error.toString()
// "[error] field not found – 'user.emal'  @line 3:12"

// Error code metadata
error.errorCode().getCategory()     // ErrorCategory (SYNTAX / TYPE_SYSTEM / SEMANTIC / …)
error.errorCode().isRecoverable()   // true = warning-level (can continue)
error.errorCode().description()     // one-line description of the error kind
```

### Error Categories

| Category | Examples |
|---|---|
| `SYNTAX` | Unexpected token, missing delimiter |
| `TYPE_SYSTEM` | Type mismatch, incompatible assignment |
| `SEMANTIC` | Unreachable code, duplicate declaration |
| `NAME_RESOLUTION` | Undefined variable, missing import |
| `INFERENCE` | Cannot converge — circular or under-constrained type |
| `CONTROL_FLOW` | Non-exhaustive match, missing return |
| `SYSTEM` | Internal / configuration errors |

### LLM Feedback Pattern

```java
// Parse and infer LLM-generated code, feed errors back for retry
AST ast = parser.parse(llmCode);
ast.infer();

if (!ast.errors().isEmpty()) {
    List<String> feedback = ast.errors().stream()
        .map(GCPError::toString)
        .toList();
    llm.retryWithFeedback(feedback);
}
```

### Error Management on AST

```java
ast.addError(new GCPError(node, GCPErrCode.FIELD_NOT_FOUND, "detail"));
ast.clearNodeErrors(nodeId);      // clear all errors for a node
ast.clearErrors(from, to);        // clear errors by index range (incremental re-inference)
```

---

## 8. Plugins (GCPBuilderPlugin)

Plugins inject external Java objects into Outline programs via the `__name__<TypeArg>` syntax.

### Outline Call Syntax

```outline
let repo  = __my_repo__<Employee>;            // type arg only
let db    = __sqlite__<Person>("jdbc:...");   // type arg + value arg
let cache = __redis__<String>("localhost");
```

### Implementing the SPI

```java
import org.twelve.gcp.plugin.GCPBuilderPlugin;

public class MyRepoPlugin implements GCPBuilderPlugin {

    @Override
    public String id() {
        return "my_repo";   // maps to __my_repo__
    }

    @Override
    public Value construct(String id, List<String> typeArgs, List<Value> valueArgs) {
        String entityType = typeArgs.isEmpty() ? "Unknown" : typeArgs.get(0);
        return new MyEntityRepoValue(entityType);
    }
}
```

### Programmatic Registration (embedded use)

```java
// Lambda registration — no JAR required
interpreter.registerConstructor("my_repo",
    (id, typeArgs, valueArgs) -> new MyEntityRepoValue(typeArgs.get(0)));

// Chain multiple registrations
interpreter
    .registerConstructor("sqlite_repo", sqliteFn)
    .registerConstructor("redis_cache", redisFn);
```

### JAR Plugin Auto-loading

1. Implement `GCPBuilderPlugin`
2. Add `META-INF/services/org.twelve.gcp.plugin.GCPBuilderPlugin` with the fully-qualified class name
3. Package as `ext_builder_myrepo.jar`
4. Drop into the `plugin_dir` directory (default: `ext_builders/`)
5. Interpreter discovers and registers it automatically at startup

```java
// Or load from a custom directory at runtime
interpreter.loadPlugins(Path.of("/extra/plugins"));
```

---

## 9. Configuration (GCPConfig)

`org.twelve.gcp.config.GCPConfig`

Four-level priority lookup (highest first):

1. **Programmatic overrides** — `with(key, value)`
2. **JVM system properties** — `-Dplugin_dir=/path`
3. **Environment variables** — `PLUGIN_DIR=/path`
4. **Classpath defaults** — `gcp.properties`

```java
// Load with default priority chain
GCPConfig cfg = GCPConfig.load();

// Read values
String dir = cfg.getString("plugin_dir");           // "ext_builders" (default)
Path   p   = cfg.getPath("plugin_dir");

// Override a single key — returns a new immutable instance
GCPConfig testCfg = GCPConfig.load().with("plugin_dir", "target/test-plugins");
OutlineInterpreter interp = new OutlineInterpreter(testCfg);
```

| Method | Description |
|---|---|
| `GCPConfig.load()` | Load with four-level priority chain |
| `with(key, value)` | Override one key at the highest priority level (immutable chaining) |
| `getString(key)` | Get string value (null if absent) |
| `getString(key, default)` | Get string value with fallback |
| `getPath(key)` | Get as `Path` |
| `getPath(key, default)` | Get as `Path` with fallback |

---

## 10. Metadata

After `infer()`, both `AST` and `ASF` expose rich metadata for IDE integrations, LLM agents, and developer tooling.

### ModuleMeta (single module)

```java
ModuleMeta meta = ast.meta();

meta.outlines()               // List<OutlineMeta>  — outline type declarations
meta.variables()              // List<SymbolMeta>   — let/var declarations
meta.functions()              // List<FunctionMeta> — function declarations
meta.imports()                // List<ImportSpec>   — import statements
meta.exports()                // List<ExportSpec>   — export statements
meta.toMap()                  // JSON-serializable Map<String,Object>
```

### ForestMeta (multi-module)

```java
ForestMeta meta = asf.meta();
meta.modules()                // List<ModuleMeta>
meta.find("geo")              // ModuleMeta for module "geo"
meta.toMap()
```

### Position-Aware API (IDE / dot-completion)

```java
ModuleMeta meta = ast.meta();

// Scope at a character offset (for cursor-position-aware tooling)
ScopeMeta scope = meta.scopeAt(offset);

// Resolve a symbol name at a position
SymbolMeta sym = meta.resolve("employees", offset);

// All symbols visible at a position
List<SymbolMeta> visible = meta.visibleSymbols(offset);

// Members of a symbol at a position (dot-completion)
List<FieldMeta> members = meta.membersOf("employee", offset);
```

### SymbolMeta / FieldMeta

```java
SymbolMeta sym = meta.resolve("x", 42);
sym.name()         // "x"
sym.typeName()     // "Int"
sym.location()     // Location (line, column, offset)

FieldMeta field = meta.membersOf("emp", offset).get(0);
field.name()       // "name"
field.typeName()   // "String"
field.isMethod()   // true if it's a callable method
```

---

## 11. Testing

### Inline Parse + Infer + Run

```java
@Test
void should_add_two_numbers() {
    OutlineParser parser = new OutlineParser();
    OutlineInterpreter interp = new OutlineInterpreter();

    AST ast = parser.parse("let add = (a: Int, b: Int) -> a + b; add(3, 4)");
    ast.infer();

    assertThat(ast.errors()).isEmpty();

    Value result = interp.runAst(ast);
    assertThat(((IntValue) result).value()).isEqualTo(7L);
}
```

### Asserting Type Errors

```java
@Test
void should_report_field_not_found() {
    OutlineParser parser = new OutlineParser();

    AST ast = parser.parse("""
        outline Point = { x: Int, y: Int };
        let p = Point { x: 1, y: 2 };
        p.z   // ← should fail
        """);
    ast.infer();

    assertThat(ast.errors()).isNotEmpty();
    assertThat(ast.errors().get(0).errorCode()).isEqualTo(GCPErrCode.FIELD_NOT_FOUND);
}
```

### Multi-Module Test

```java
@Test
void should_resolve_cross_module_import() {
    ASF asf = new ASF();
    OutlineParser parser = new OutlineParser(new GCPConverter(asf));
    OutlineInterpreter interp = new OutlineInterpreter();

    parser.parse("namespace math; export let PI = 3.14;");
    parser.parse("import { PI } from math; PI * 2.0");

    asf.infer();
    assertThat(asf.hasErrors()).isFalse();

    Value result = interp.run(asf);
    assertThat(((FloatValue) result).value()).isCloseTo(6.28, within(0.001));
}
```

### Testing a Custom Plugin

```java
@Test
void should_call_registered_plugin() {
    OutlineParser parser = new OutlineParser();
    OutlineInterpreter interp = new OutlineInterpreter();

    interp.registerConstructor("const_42",
        (id, typeArgs, valueArgs) -> IntValue.of(42L));

    AST ast = parser.parse("let x = __const_42__; x + 1");
    ast.infer();
    Value result = interp.runAst(ast);

    assertThat(((IntValue) result).value()).isEqualTo(43L);
}
```

---

## Quick Lookup: Error Scenarios

| Symptom | Likely cause | Fix |
|---|---|---|
| `ast.errors()` returns `FIELD_NOT_FOUND` | Accessing a field that doesn't exist on the type | Check field name via `ast.meta().membersOf(...)` |
| `ast.inferred()` returns `false` | Underconstrained expression — type cannot be determined | Add an explicit type annotation, e.g. `(x: Int) ->` |
| `GCPRuntimeException: module not found` | `asf.get("name")` called with wrong module name | Check `asf.asts()` to list all registered module names |
| Plugin not called | Constructor ID mismatch between `registerConstructor` and `__id__` in source | IDs are matched without `__` delimiters: `"my_repo"` ↔ `__my_repo__` |
| `ClassCastException` on Value unwrap | Wrong Value subtype assumed | Check with `instanceof` before casting |
| Multi-module: import not resolved | Module registered after the importing module | Register all modules before calling `asf.infer()` |

---

## Thread Safety

| Object | Thread-safe? | Notes |
|---|---|---|
| `AST` / `ASF` | No | Stateful; do not share across threads during inference/execution |
| `OutlineInterpreter` | No | Holds a mutable `Environment`; not reentrant |
| `GCPConfig` | Yes | Immutable; safe to share |
| `OutlineParser` | No | Shares static grammar tables (read-only) but instance state is not thread-safe |
| `MetaExtractor` | Yes | Pure function; constructs independent objects per call |
