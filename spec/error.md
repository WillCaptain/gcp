# GCP — 错误诊断 API

**包路径** `org.twelve.gcp.exception`

---

## 概览

GCP 的错误系统设计为**结构化诊断**，每个错误携带：
- 精确的**错误类别**（`GCPErrCode`）
- **源码节点**引用（含行列号位置）
- **详细说明**字符串
- 人可读的格式化输出

错误通过 `ast.errors()` 收集，推导结束后统一读取。

---

## 一、GCPError（结构化错误）

`org.twelve.gcp.exception.GCPError`

```java
GCPError error = ast.errors().get(0);

error.errorCode();   // GCPErrCode.FIELD_NOT_FOUND
error.node();        // 出错的 AST 节点（含位置信息）
error.message();     // "unknown field 'codee' on Country"（详细说明）

// 格式化诊断输出（适合展示给用户或传给 LLM）
error.toString();
// → "[error] field not found – 'c.codee'  (unknown field 'codee' on Country)  @line 3:12"
```

### toString() 格式

```
[<category>] <description> – '<source_snippet>'  (<detail>)  @<line>:<col>
```

| 部分 | 来源 | 示例 |
|---|---|---|
| `[category]` | `errorCode.getCategory().name()` | `[error]` / `[warning]` |
| `description` | `errorCode.description()` | `field not found in type` |
| `source_snippet` | `node.lexeme()`（≤ 80 字符） | `c.codee` |
| `detail` | `error.message()` | 附加说明（可为空） |
| `@line:col` | `node.loc().display()` | `@line 3:12` |

---

## 二、GCPErrCode（错误码枚举）

`org.twelve.gcp.exception.GCPErrCode`

### 语法 / 结构类

| 错误码 | 描述 | 类别 |
|---|---|---|
| `NODE_AST_MISMATCH` | internal: node belongs to a different AST | SYNTAX |
| `UNREACHABLE_STATEMENT` | unreachable code | SYNTAX |
| `DUPLICATED_DEFINITION` | duplicate definition | SYNTAX |

### 类型系统类

| 错误码 | 描述 | 类别 |
|---|---|---|
| `OUTLINE_MISMATCH` | type mismatch | TYPE_SYSTEM |
| `CONSTRUCT_CONSTRAINTS_FAIL` | type constraint violated | TYPE_SYSTEM |
| `UNSUPPORTED_UNARY_OPERATION` | unsupported unary operation for this type | TYPE_SYSTEM |
| `ARGUMENT_MISMATCH` | argument type does not match parameter type | TYPE_SYSTEM |
| `POLY_SUM_FAIL` | polymorphic type resolution failed | TYPE_SYSTEM |
| `INVALID_OPTION_EXPRESSION` | option ('|') can only be defined in outline declarations | TYPE_SYSTEM |

### 语义类

| 错误码 | 描述 | 类别 |
|---|---|---|
| `VARIABLE_NOT_DEFINED` | variable is not defined | SEMANTIC |
| `MODULE_NOT_DEFINED` | module is not defined | SEMANTIC |
| `FUNCTION_NOT_DEFINED` | function is not defined | SEMANTIC |
| `NOT_A_FUNCTION` | expression is not callable | SEMANTIC |
| `FIELD_NOT_FOUND` | field not found in type | SEMANTIC |
| `NOT_INITIALIZED` | variable used before initialization | SEMANTIC |
| `NOT_ASSIGNABLE` | cannot assign to an immutable binding | SEMANTIC |
| `THIS_IS_NOT_ASSIGNABLE` | 'this' is not assignable | SEMANTIC |
| `NOT_ACCESSIBLE` | member is not accessible from this scope | SEMANTIC |
| `OUTLINE_NOT_FOUND` | outline (type) not found | SEMANTIC |
| `OUTLINE_USED_AS_VALUE` | outline type cannot be used as a value expression | SEMANTIC |
| `UNAVAILABLE_THIS` | 'this' is not available in this context | SEMANTIC |

### 控制流类

| 错误码 | 描述 | 类别 |
|---|---|---|
| `CONDITION_IS_NOT_BOOL` | condition must be a Bool expression | CONTROL_FLOW |
| `POSSIBLE_ENDLESS_LOOP` | possible infinite loop | CONTROL_FLOW |

### 名称解析类

| 错误码 | 描述 | 类别 |
|---|---|---|
| `AMBIGUOUS_VARIABLE_REFERENCE` | ambiguous variable reference | NAME_RESOLUTION |
| `AMBIGUOUS_DECLARATION` | ambiguous declaration | NAME_RESOLUTION |

### 类型推导类

| 错误码 | 描述 | 类别 |
|---|---|---|
| `INFER_ERROR` | type inference failed | INFERENCE |
| `UNAVAILABLE_OUTLINE_ASSIGNMENT` | cannot determine type for assignment | INFERENCE |
| `DECLARED_CAN_NOT_BE_GENERIC` | declared type cannot be generic | INFERENCE |

### 函数 / 引用类（SYSTEM 类别）

| 错误码 | 描述 |
|---|---|
| `AMBIGUOUS_RETURN` | ambiguous return type |
| `FUNCTION_NOT_FOUND` | function overload not found |
| `NOT_REFER_ABLE` | expression is not referable |
| `REFERENCE_MIS_MATCH` | reference type mismatch |
| `UNARY_POSITION_MISMATCH` | unary operator used in wrong position |
| `TYPE_CAST_NEVER_SUCCEED` | this type cast can never succeed |
| `NOT_INTEGER` | array index must be an integer |
| `NOT_AN_ARRAY_OR_DICT` | expression is not an array or dict |
| `UNPACK_INDEX_OVER_FLOW` | unpack index out of range |
| `INVALID_SYMBOL` | invalid symbol |
| `NOT_ENTITY_INHERITED` | type is not an entity subtype |
| `MISSING_REQUIRED_FIELD` | missing required field in entity construction |
| `NOT_BE_ASSIGNEDABLE` | value cannot be assigned to this binding |
| `INTERPRETER_NOT_IMPLEMENTED` | interpreter support for this feature is not yet implemented |
| `PROJECT_FAIL` | project compilation failed |

---

## 三、错误类别（ErrorCategory）

```java
GCPErrCode.ErrorCategory category = error.errorCode().getCategory();
// SYNTAX / TYPE_SYSTEM / SEMANTIC / CONTROL_FLOW / NAME_RESOLUTION / INFERENCE / SYSTEM
```

| 类别 | 说明 | 对应错误码 |
|---|---|---|
| `SYNTAX` | 结构语法问题 | NODE_AST_MISMATCH, DUPLICATED_DEFINITION, UNREACHABLE_STATEMENT |
| `TYPE_SYSTEM` | 类型系统约束违反 | OUTLINE_MISMATCH, ARGUMENT_MISMATCH, POLY_SUM_FAIL… |
| `SEMANTIC` | 语义问题（变量、字段、成员访问） | VARIABLE_NOT_DEFINED, FIELD_NOT_FOUND, NOT_A_FUNCTION… |
| `CONTROL_FLOW` | 控制流问题 | CONDITION_IS_NOT_BOOL, POSSIBLE_ENDLESS_LOOP |
| `NAME_RESOLUTION` | 名称模糊 | AMBIGUOUS_VARIABLE_REFERENCE, AMBIGUOUS_DECLARATION |
| `INFERENCE` | 类型推导失败 | INFER_ERROR, UNAVAILABLE_OUTLINE_ASSIGNMENT |
| `SYSTEM` | 系统 / 实现层面 | 其余所有 |

### 可恢复错误（Warning）

下列错误不阻止执行，仅作警告：

```java
boolean warn = error.errorCode().isRecoverable();
// true: POSSIBLE_ENDLESS_LOOP, UNREACHABLE_STATEMENT, TYPE_CAST_NEVER_SUCCEED
```

---

## 四、GCPError 使用场景

### 场景 A：IDE 展示诊断

```java
asf.infer();
for (GCPError e : asf.allErrors()) {
    String message  = e.toString();     // 完整诊断行
    Node   node     = e.node();
    int    line     = node.loc().line(); // 行号（1-based）
    int    col      = node.loc().col();  // 列号（0-based）
    String severity = e.errorCode().isRecoverable() ? "warning" : "error";
    ide.addDiagnostic(line, col, severity, message);
}
```

### 场景 B：LLM 代码校验反馈

```java
asf.infer();
if (asf.hasErrors()) {
    List<String> feedback = asf.allErrors().stream()
        .map(GCPError::toString)
        .toList();
    // feedback = ["[error] field not found – 'c.codee'  @line 3:12",
    //             "[error] type mismatch – 'x + \"hello\"'  @line 5:8"]
    llm.correctCode(originalCode, feedback);
}
```

### 场景 C：按类别过滤

```java
// 只关注类型系统错误
List<GCPError> typeErrors = ast.errors().stream()
    .filter(e -> e.errorCode().getCategory() == GCPErrCode.ErrorCategory.TYPE_SYSTEM)
    .toList();

// 只关注致命错误（排除 warning）
List<GCPError> fatal = ast.errors().stream()
    .filter(e -> !e.errorCode().isRecoverable())
    .toList();
```

### 场景 D：增量推导清除错误

```java
// 编辑器重新输入时，只需清除受影响节点的错误，无需重建整个 AST
ast.clearNodeErrors(editedNode.id());
// 或清除某范围的错误（如重新推导某个表达式后的旧结果）
ast.clearErrors(from, to);
```

---

## 五、GCPErrorReporter（推导内部使用）

`org.twelve.gcp.exception.GCPErrorReporter`

推导规则内部通过此工具类向 AST 报告错误，外部不需要直接使用：

```java
// 推导规则内部使用示例
GCPErrorReporter.report(ast, node, GCPErrCode.FIELD_NOT_FOUND, "field 'xx' not found on Country");
```

---

## 六、GCPRuntimeException（解释执行期异常）

`org.twelve.gcp.exception.GCPRuntimeException`

运行时（解释执行阶段）抛出的异常，区别于推导阶段收集到 `ast.errors()` 的诊断错误：

```java
try {
    Value result = interpreter.run(asf);
} catch (GCPRuntimeException e) {
    GCPErrCode code = e.errorCode();
    // 如：MODULE_NOT_DEFINED、INTERPRETER_NOT_IMPLEMENTED 等
}
```

运行时异常通常由以下情况触发：
- 调用 `asf.get("moduleName")` 但模块不存在（`MODULE_NOT_DEFINED`）
- 解释器执行到尚未实现的节点（`INTERPRETER_NOT_IMPLEMENTED`）
- 插件构造失败（`PROJECT_FAIL`）
