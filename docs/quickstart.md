# GCP + Outline — 5-Minute Quick Start

This guide takes you from zero to a working Outline program in under 5 minutes. By the end you will have:

- Parsed Outline source code into a typed AST
- Run type inference and checked for errors
- Executed the program and read the result
- Registered a custom Java plugin callable from Outline

---

## Step 1 — Add the Dependencies

```xml
<!-- GCP: type inference engine + interpreter -->
<dependency>
    <groupId>org.twelve</groupId>
    <artifactId>gcp</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- Outline: source-code parser (converts Outline text → GCP AST) -->
<dependency>
    <groupId>org.example</groupId>
    <artifactId>outline</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

> **GCP** owns the type system, inferencer, and interpreter.  
> **Outline** is the language front-end that parses source text into GCP's AST.  
> If you build ASTs programmatically (e.g. from another language), you only need `gcp`.

---

## Step 2 — Parse Outline Source Code

```java
import org.twelve.outline.OutlineParser;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.ASF;

OutlineParser parser = new OutlineParser();

// Parse a single-module program — returns a fully populated AST
AST ast = parser.parse("""
    let x = 42;
    let greeting = "hello " + to_str(x);
    greeting
    """);
```

`OutlineParser` compiles grammar tables once per JVM. Subsequent calls to `parse()` are fast.

---

## Step 3 — Run Type Inference

Type inference resolves every node's type through constraint propagation. No explicit annotations needed.

```java
// Trigger inference on the module
ast.infer();

// Check whether all nodes converged
if (!ast.inferred()) {
    System.out.println("Some nodes could not be resolved:");
    ast.missInferred().forEach(n -> System.out.println("  " + n));
}
```

---

## Step 4 — Check for Errors

After inference, inspect structured errors with source locations:

```java
if (!ast.errors().isEmpty()) {
    ast.errors().forEach(e -> System.out.println(e));
    // "[error] field not found – 'user.emal'  @line 3:12"
    return;
}
System.out.println("No errors — program is type-safe.");
```

---

## Step 5 — Execute and Read the Result

```java
import org.twelve.gcp.interpreter.OutlineInterpreter;
import org.twelve.gcp.interpreter.value.*;

OutlineInterpreter interpreter = new OutlineInterpreter();
Value result = interpreter.runAst(ast);

// Unwrap the result
if (result instanceof StringValue sv) {
    System.out.println("Result: " + sv.value());   // "hello 42"
}
```

### Value Unwrapping Patterns

```java
// Integer / Long
if (result instanceof IntValue iv)    long n = iv.value();

// String
if (result instanceof StringValue sv) String s = sv.value();

// Boolean
if (result instanceof BoolValue bv)   boolean b = bv.isTruthy();

// Array
if (result instanceof ArrayValue av)  List<Value> elems = av.elements();

// Dict
if (result instanceof DictValue dv)   Value v = dv.get(new StringValue("key"));

// Unit (no meaningful return value)
result == UnitValue.INSTANCE
```

---

## Step 6 — Register a Custom Plugin (Optional)

Plugins let you inject external Java objects into Outline programs via the `__name__<TypeArg>` syntax.

```java
import org.twelve.gcp.interpreter.value.EntityValue;

// Register a constructor named "greet_service"
interpreter.registerConstructor("greet_service", (id, typeArgs, valueArgs) -> {
    // typeArgs: type-parameter strings, e.g. ["Employee"]
    // valueArgs: value arguments passed in Outline
    String lang = valueArgs.isEmpty() ? "en"
                : ((StringValue) valueArgs.get(0)).value();
    return new GreetServiceValue(lang);  // your custom Value implementation
});
```

In Outline source, call it as:

```outline
let svc = __greet_service__("zh");   // valueArgs = ["zh"]
let msg = svc.greet("Alice");
```

---

## Putting It All Together

```java
import org.twelve.outline.OutlineParser;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.interpreter.OutlineInterpreter;
import org.twelve.gcp.interpreter.value.*;

OutlineParser parser   = new OutlineParser();
OutlineInterpreter interp = new OutlineInterpreter();

AST ast = parser.parse("""
    let add = (a: Int, b: Int) -> a + b;
    let result = add(3, 4);
    result
    """);

ast.infer();

if (ast.errors().isEmpty()) {
    Value v = interp.runAst(ast);
    System.out.println(v);   // IntValue(7)
}
```

---

## What Happens Under the Hood

```
Outline source code (String)
  │
  ▼  OutlineParser.parse(src)
GCP AST (nodes, no types yet)
  │
  ▼  ast.infer()  ← constraint propagation, fixed-point iteration
Typed AST (every node has a resolved Outline type)
  │
  ├─ ast.errors()           → List<GCPError>  (type / name / syntax errors)
  ├─ ast.meta()             → ModuleMeta      (symbols, scopes — for IDE/LLM)
  │
  ▼  interpreter.runAst(ast)
Value                       (IntValue / StringValue / EntityValue / …)
```

For **multi-module** programs, the same pipeline runs inside an `ASF` (Abstract Syntax Forest):

```
ASF asf = new ASF();
OutlineParser parser = new OutlineParser(new GCPConverter(asf));

parser.parse("namespace geo; export let PI = 3.14;");
parser.parse("import { PI } from geo; PI * 2.0");

asf.infer();                           // fixed-point across all modules
Value result = interpreter.run(asf);  // execute all modules in order
```

---

## Next Steps

| What you want | Where to look |
|---|---|
| Full SDK API reference | [SDK Reference](./sdk-reference.md) |
| Outline language syntax | [Outline Language Guide](../../outline/docs/outline-language.md) |
| Multi-module programs | [SDK Reference → Multi-Module Programs](./sdk-reference.md#4-multi-module-programs) |
| Type inference details | [SDK Reference → Type Inference](./sdk-reference.md#3-type-inference) |
| Plugin development | [SDK Reference → Plugins](./sdk-reference.md#7-plugins) |
| Error handling | [SDK Reference → Error Handling](./sdk-reference.md#6-error-handling) |
| Metadata / IDE integration | [SDK Reference → Metadata](./sdk-reference.md#9-metadata) |
