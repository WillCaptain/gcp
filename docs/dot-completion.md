# Outline Dot-Completion — 调用逻辑与 LLM 集成指南

## 概述

本文档说明 Outline 语言推导系统在 IDE editor 和 LLM 工具链中如何完成 **dot-completion**（`.` 触发后，根据表达式类型返回可用成员列表）。核心能力全部封装在 GCP 层的 `MetaExtractor` 中，editor 和 LLM 工具都使用同一套调用路径。

---

## 核心 API：`MetaExtractor.dotCompletionOf`

```java
// GCP: org.twelve.gcp.meta.MetaExtractor

/**
 * 给定 . 之前的表达式的 Outline 对象和当前模块的 ASF，
 * 返回可用成员列表（FieldMeta）。
 *
 * 统一处理：plain entity / VirtualSet collection / lazy / Genericable
 */
public static List<FieldMeta> dotCompletionOf(Outline outline, ASF contextAsf)

/**
 * 专门处理 VirtualSet 集合类型（如 Employees、Schools）。
 * own 导航方法 + VirtualSet 内置算子（filter/count/take/…）合并返回。
 * 通过名字在 AST 声明体里查找，绕开 GCP lazy parametric type 的限制。
 */
public static List<FieldMeta> virtualSetDotCompletion(Outline resolved, ASF contextAsf)
```

**关键规则**：
- 调用方不需要自己解包 lazy / Returnable / Genericable，`dotCompletionOf` 内部全部处理。
- 对 VirtualSet 集合，必须传入包含该 outline 声明的 `contextAsf`（outline editor 传当前 module ASF，entitir editor 传 world preamble ASF）。
- 返回空列表代表"无成员"，调用方不应 fallback 到其他路径自行猜测。

---

## 两个 Editor 的调用架构

### Outline Playground Editor

**特点**：所有 schema 和用户代码在同一个 ASF 中，没有独立 preamble。

```
用户在 editor 里输入 `expr.`
    ↓
POST /api/completions  { code, offset }
    ↓
OutlineCompilerService.completions(code, offset)
    ↓
dotCompletionFromCode(code, offset)
    ├── [Fast path] cachedMeta.membersOf(symbolName, pos)   ← 上次 typecheck 缓存
    └── [Slow path] 重新 parse+infer，得到 ASF/AST
            ├── Strategy A: 命名符号 (symbolName != null)
            │       findLastIdentifier → outline → membersFromOutline(outline, asf)
            │       ↓ fallback
            │       meta.membersOf(symbolName, offset)
            ├── Strategy B: lambda 参数推导由 meta.membersOf 处理
            └── Strategy C: 链式表达式 (symbolName == null，如 coll.nav().)
                    lastExpr.outline() → membersFromOutline(outline, asf)
                    ↓ 内部调用
                    MetaExtractor.dotCompletionOf(outline, asf)
```

`membersFromOutline(outline, asf)` 直接调用 `MetaExtractor.dotCompletionOf`，asf 就是刚解析完整代码的那个 ASF，其中包含所有 `outline X = VirtualSet<Y>{...}` 声明。

### Entitir Playground Editor

**特点**：world schema 作为 preamble 预编译缓存，用户代码通过 `world.forkedInfer(userCode)` 增量推导。

```
用户在 entitir editor 里输入 `expr.`
    ↓
POST /entitir-api/completions  { code, offset }
    ↓
EntitirPlaygroundService.completions(code, offset)
    ↓
dotCompletionFromCode(code, offset, worldPreamble, worldPreambleAsf,
                      world::forkedInfer,
                      sym -> schemaCompletions(sym, null))
    ├── Strategy 1: 命名符号
    │       findDeclaredOutline / findLastIdentifier → outline
    │       若为 VirtualSet → ctxSchemaFn.apply(symbolName)  [schema cache]
    │       否则 → membersFromOutline(outline, ctxPreambleAsf)
    │               ↓
    │               MetaExtractor.dotCompletionOf(outline, worldPreambleAsf)
    ├── Strategy 2: 链式表达式
    │       lastExpr.outline().eventual() → 检测 isVirtualSetCollection
    │       Scenario A (lambda 内部): ctxSchemaFn.apply(innerMethod)
    │       Scenario B (链尾):        membersFromOutline(outline, ctxPreambleAsf)
    │               ↓
    │               MetaExtractor.dotCompletionOf(outline, worldPreambleAsf)
    │               → virtualSetDotCompletion(outline, worldPreambleAsf)
    │               → fieldsOf("Employees", wAst)   [own]
    │               + fieldsOf("VirtualSet", wAst)  [builtins]
    ├── Strategy 3: meta.membersOf  (lambda 参数如 c in filter(c->c.))
    └── Strategy 4: schema fallback + chain base 符号
```

---

## VirtualSet 成员解析的关键路径

```
outline Employees = VirtualSet<Employee>{
    employee: Unit -> Employees   // 导航边
};
let employees = __ontology_repo__<Employees>;

// 用户输入：
employees.                  // → Employees 自己的 members + VirtualSet builtins
employees.filter(e -> e.    // → Employee entity members
badgeRequests.employee().   // → Employees members (chain expression Scenario B)
```

### `employees.` 的解析

1. `symbolName = "employees"`，`findDeclaredOutline` 找到 `employees` 的 outline = `Employees` entity
2. `MetaExtractor.dotCompletionOf(Employees, worldPreambleAsf)` 
3. `isVirtualSetCollection(Employees)` = true
4. `virtualSetDotCompletion` → `fieldsOf("Employees", ast)` = `[employee: Unit→Employees]`
5. + `fieldsOf("VirtualSet", ast)` = `[filter, count, take, map, groupBy, …]`
6. 返回合并结果 ✓

### `employees.filter(e -> e.` 的解析

1. `symbolName = "e"`（lambda 参数）
2. Strategy 1 中 `findLastIdentifier` 找到 `e` 节点，`e.outline()` = GCP 推断的 `Employee`
3. `isVirtualSetCollection(Employee)` = false（Employee 是 plain entity）
4. `membersFromOutline(Employee, ctxPreambleAsf)` → `completionMembersOf(Employee, asf)`
5. `extractEntityFields(Employee, source)` = `[id, name, email, department, …]` ✓

### `badgeRequests.employee().` 的解析（chain expression）

1. `symbolName = null`（dot 在 `)` 后面）
2. Strategy 2: `lastExpr.outline()` = GCP 推断的 `Employees` (VirtualSet)
3. `outline.eventual()` → `Employees`，`isVirtualSetCollection` = true
4. `lastExprMethod = "employee"`，`innerMethod = "employee"`，两者相等 → Scenario B
5. `membersFromOutline(Employees, ctxPreambleAsf)` → `dotCompletionOf` → `virtualSetDotCompletion`
6. 返回 Employees members ✓

---

## LLM 工具中使用 `getMembers` 进行逐步验证

LLM 工具（如 `OntologyAgentTool`、`DecisionDescribeTool`）在辅助用户写 Outline 代码时，可以用下面的模式验证每一步 `entity.member` 是否可达：

### 调用方式

```java
// 伪代码：LLM 工具验证 entity.member 的推导正确性

ASF asf = world.forkedInfer(partialCode);   // 增量推导，只解析用户片段

// 获取表达式 expr 的成员列表
Outline exprOutline = resolveExpressionOutline(asf, partialCode);
List<FieldMeta> members = MetaExtractor.dotCompletionOf(exprOutline, worldPreambleAsf);

// 验证目标 member 是否存在
boolean valid = members.stream().anyMatch(f -> f.name().equals(targetMember));
```

### 多步链式验证示例

```
目标代码: badgeRequests.employee().filter(e -> e.status == "active")

Step 1: badgeRequests.
  → dotCompletionOf(BadgeRequests, asf)
  → [employee: Unit→Employees, filter, count, ...]
  ✓ "employee" 在列表中

Step 2: badgeRequests.employee().
  → 推断 badgeRequests.employee() 的 outline = Employees (VirtualSet<Employee>)
  → dotCompletionOf(Employees, asf)
  → [employee: Unit→Employees, filter, count, ...]
  ✓ "filter" 在列表中

Step 3: badgeRequests.employee().filter(e -> e.
  → 推断 filter callback 参数 e 的 outline = Employee
  → dotCompletionOf(Employee, asf)
  → [id:Int, name:String, email:String, status:String, ...]
  ✓ "status" 在列表中，类型 String，可与 "active" 比较
```

### `FieldMeta` 字段说明

| 字段 | 类型 | 含义 |
|------|------|------|
| `name()` | String | 成员名，如 `"filter"`, `"employee"`, `"status"` |
| `type()` | String | 类型字符串，如 `"Unit → Employees"`, `"String"` |
| `isMethod()` | boolean | true = 可调用（需要加 `()`），false = 属性访问 |
| `origin()` | String | `"own"` / `"base"` / `"builtin"` |
| `description()` | String | 从 outline 代码前注释提取的文档，可能为 null |

### 在 AI 工具中验证"下一步写什么"

```java
// 工具：给定当前表达式路径，返回可用的下一级 member 列表
public List<String> getNextMembers(String partialExpression, OntologyWorld world) {
    // 1. 增量推导
    ASF asf = world.forkedInfer(partialExpression);

    // 2. 找最后一个表达式的 outline
    Outline outline = findLastExpressionOutline(asf);

    // 3. 用统一 API 获取 members
    List<FieldMeta> members = MetaExtractor.dotCompletionOf(outline, world.preambleAsf());

    // 4. 返回成员名列表供 LLM 选择
    return members.stream()
        .map(f -> f.name() + (f.isMethod() ? "()" : "") + " : " + f.type())
        .toList();
}
```

LLM 收到这个列表后，只能从列表中选择，不能凭空猜测 member 名，从而保证生成的 Outline 代码在类型系统层面是正确的。

---

## 三层架构总结

```
┌─────────────────────────────────────────────────────────────────┐
│                     GCP (MetaExtractor)                         │
│                                                                 │
│  dotCompletionOf(outline, contextAsf)                           │
│    ├── resolveOutline + eventual()  [解包 lazy/Genericable]     │
│    ├── isVirtualSetCollection?                                  │
│    │     └── virtualSetDotCompletion                            │
│    │           ├── fieldsOf(outlineName, ast)  [own nav edges]  │
│    │           └── fieldsOf("VirtualSet", ast) [builtins]       │
│    └── completionMembersOf  [plain entity + AST fallback]       │
└─────────────────────────────────────────────────────────────────┘
              ↑                          ↑
┌─────────────────────┐    ┌────────────────────────────┐
│  OutlineCompilerSvc │    │  EntitirPlaygroundService  │
│                     │    │                            │
│  contextAsf =       │    │  contextAsf =              │
│  current module ASF │    │  worldPreambleAsf          │
│  (含所有 outline 声明)│    │  (world schema 预编译)     │
└─────────────────────┘    └────────────────────────────┘
              ↑                          ↑
┌─────────────────────┐    ┌────────────────────────────┐
│  Outline Playground │    │  Entitir Playground        │
│  /api/completions   │    │  /entitir-api/completions  │
└─────────────────────┘    └────────────────────────────┘
                                         ↑
                           ┌────────────────────────────┐
                           │  LLM Tools (getMembers,    │
                           │  OntologyAgentTool, etc.)  │
                           └────────────────────────────┘
```

**原则**：
1. 所有 dot-completion / getMembers 逻辑只能经过 `MetaExtractor.dotCompletionOf`，不能绕开。
2. 调用方只负责提供正确的 `contextAsf`（editor 用当前 ASF，LLM 工具用 `world.preambleAsf()`）。
3. `VirtualSet` 类型必须走 by-name AST 查找路径，不能用 `outline.members()` 直接遍历。
