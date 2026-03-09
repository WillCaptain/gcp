# GCP Meta API — 元数据 / IDE / LLM 工具参考

**包路径** `org.twelve.gcp.meta`

---

## 概览

Meta API 是 GCP 对外暴露的**结构化元数据层**，面向两类主要消费者：

1. **IDE 工具**（dot-completion、悬停提示、符号跳转）
2. **LLM Agent**（理解模块结构、验证成员访问合法性、上下文感知代码生成）

所有元数据对象均为**只读不可变**，支持 `toMap()` 序列化为 JSON。推导完成后通过 `ast.meta()` 或 `asf.meta()` 一次性提取。

| 类 | 职责 |
|---|---|
| `MetaExtractor` | 静态工厂：从 AST/符号表提取元数据 |
| `ModuleMeta` | 单模块元数据根（含位置感知 API） |
| `ForestMeta` | 多模块森林元数据根 |
| `SchemaMeta` | 声明元数据基类（OutlineMeta / VariableMeta / FunctionMeta） |
| `OutlineMeta` | outline 类型声明的完整元数据（字段 + 方法） |
| `FunctionMeta` | 函数声明元数据（参数 + 返回值） |
| `VariableMeta` | 变量声明元数据（类型 + 可变性） |
| `FieldMeta` | 字段/方法条目（名称 + 类型 + 来源） |
| `ScopeMeta` | 作用域快照（源码范围 + 符号列表） |
| `SymbolMeta` | 作用域内可见符号（名称 + 类型 + 种类） |
| `ImportMeta` | import 规格 |
| `ExportMeta` | export 规格 |

---

## 一、MetaExtractor（元数据提取器）

`org.twelve.gcp.meta.MetaExtractor`

**纯静态工具类**，是元数据 API 的唯一入口。通常不需要直接调用——`ast.meta()` 和 `asf.meta()` 内部已自动调用 `MetaExtractor.extract(ast)`。直接调用的场景：

- 需要在 **dot-completion** 服务中解析任意 `Outline` 对象的成员列表
- 需要解析 `Genericable`（泛型变量，如 lambda 参数）的具体类型

### 1.1 模块元数据提取

```java
// 从 AST 提取单模块元数据（ast.meta() 等价）
ModuleMeta meta = MetaExtractor.extract(ast);
```

### 1.2 成员列表（Dot-Completion 核心 API）

```java
// 从任意 Outline 提取字段/方法列表
// ✓ 自动解析 Genericable（lambda 参数）→ 具体实体类型
// ✓ 自动解析 Returnable（链式调用结果）→ 具体返回类型
// ✓ 自动合并 Option（和类型）各 arm 的成员（取并集）
List<FieldMeta> members = MetaExtractor.fieldsOf(outline, sourceCode);

// 无源码时调用（field.description 为 null）
List<FieldMeta> members = MetaExtractor.fieldsOf(outline);
```

典型使用场景：

```java
// Outline 代码：countries.filter(c -> c.)
//   此时 c 的 Outline 是 Genericable（lambda 参数泛型变量）
//   MetaExtractor.fieldsOf 会正确解析出 Country 实体的成员

Outline cOutline = /* c 的 Outline，从符号表取得 */;
List<FieldMeta> members = MetaExtractor.fieldsOf(cOutline, src);
// 返回：[{name:"code", type:"String"}, {name:"name", type:"String"}, {name:"to_str", type:"Unit -> String"}]
```

### 1.3 类型解析（resolveOutline）

将 `Genericable` / `Returnable` 等包装类型解析为最具体的底层类型。

```java
Outline concrete = MetaExtractor.resolveOutline(outline);
```

解析优先级（高→低，取第一个"具体类型"）：

| 优先级 | 维度 | 来源 |
|---|---|---|
| 1 | `extendToBe` | 实际赋值 / 投影值（上界约束） |
| 2 | `projectedType` | 泛型实例化时记录（lambda 参数专用，不影响推导约束） |
| 3 | `declaredToBe` | 显式类型注解（`(c: Country) ->` 中的 `Country`） |
| 4 | `hasToBe` | 调用上下文约束 |
| 5 | `definedToBe` | 结构访问约束（`c.code` 导致 `c.definedToBe = {code:?}`） |

"具体类型"判定标准：非 null、非 `Genericable`、非 `UNKNOWN`、非 `ANY`。

---

## 二、ModuleMeta（单模块元数据）

`org.twelve.gcp.meta.ModuleMeta`

由 `ast.meta()` 返回，是整个模块元数据的根对象。

### 2.1 基本信息

```java
ModuleMeta meta = ast.meta();
meta.name();           // 模块名（如 "geo"）
meta.namespace();      // 命名空间 lexeme（如 "org.twelve.example"）
meta.description();    // 模块注释（模块第一个 // 注释）
```

### 2.2 声明列表

```java
// 所有顶层声明（outline / 变量 / 函数，按源码顺序）
List<SchemaMeta> all = meta.nodes();

// 按类型过滤
List<OutlineMeta>   outlines  = meta.outlines();    // outline 类型声明
List<VariableMeta>  variables = meta.variables();   // let/var 声明
List<FunctionMeta>  functions = meta.functions();   // 函数声明（let f = x -> ...）

// 按名称查找（跨三种类型）
SchemaMeta country = meta.find("Country");
```

### 2.3 Import / Export

```java
List<ImportMeta> imports = meta.imports();
// [{symbol:"countries", as:null, from:"geo", description:"..."}]

List<ExportMeta> exports = meta.exports();
// [{name:"countries", exportedAs:null, description:"..."}]
```

### 2.4 位置感知 API（IDE/LLM 核心）

```java
// 光标位置所在的最内层作用域（字节偏移量）
ScopeMeta scope = meta.scopeAt(42L);

// 在指定位置（光标偏移量）解析符号——从内层作用域向外查找
SymbolMeta sym = meta.resolve("c", 42L);
// {name:"c", type:"Country", kind:"parameter", mutable:false}

// 指定位置可见的全部符号（内层符号遮蔽外层同名符号）
List<SymbolMeta> visible = meta.visibleSymbols(42L);

// 指定位置符号的成员列表（dot-completion 高层入口）
List<FieldMeta> members = meta.membersOf("c", 42L);
// [{name:"code", type:"String"}, {name:"name", type:"String"}, ...]
```

> **实现说明**：`membersOf` 会先 `resolve` 符号取得其类型名，再在模块的 `OutlineMeta` 列表中查找匹配的类型，最后返回其成员。
> 对于 lambda 参数等通过 `projectedType` 记录类型的符号，类型名会正确反映具体实体（如 `"Country"`）。

### 2.5 作用域查询（ScopeMeta）

```java
ScopeMeta scope = meta.scopeAt(offset);
scope.scopeId();          // 作用域唯一 ID
scope.startOffset();      // 源码起始偏移（字节）
scope.endOffset();        // 源码终止偏移（字节）
scope.parentScopeId();    // 父作用域 ID（根作用域为 null）
scope.symbols();          // 本作用域直接定义的符号列表
scope.contains(offset);   // 是否包含某偏移量
scope.length();           // 作用域字节长度
```

### 2.6 JSON 序列化

```java
// 转为 JSON 可序列化 Map（供 LLM 使用）
Map<String, Object> json = meta.toMap();
// 结构：{name, namespace, description?, imports:[], exports:[], nodes:[], scopes:[]}
```

---

## 三、声明元数据（SchemaMeta 子类）

### 3.1 OutlineMeta（outline 类型声明）

```java
OutlineMeta country = (OutlineMeta) meta.find("Country");
country.name();         // "Country"
country.type();         // "{code:String, name:String, ...}"（结构描述）
country.description();  // 声明前的注释文本

// 字段 vs 方法
country.fields();       // 数据字段列表：[{name:"code", type:"String"}, ...]
country.methods();      // 方法字段列表：[{name:"provinces", type:"Unit -> Provinces"}, ...]
country.members();      // 全部成员（fields + methods，按声明顺序）
```

### 3.2 FunctionMeta（函数声明）

```java
FunctionMeta fn = (FunctionMeta) meta.find("addTax");
fn.name();           // "addTax"
fn.type();           // "Number -> Number -> Number"（完整函数类型）
fn.description();    // 声明前的注释文本
fn.parameters();     // 参数列表：List<FieldMeta>（名称 + 类型）
fn.returns();        // 返回值类型字符串
```

### 3.3 VariableMeta（变量声明）

```java
VariableMeta v = (VariableMeta) meta.find("pi");
v.name();            // "pi"
v.type();            // "Double"（推导得到的类型）
v.description();     // 声明前的注释文本
v.mutable();         // false（let 声明）/ true（var 声明）
```

---

## 四、FieldMeta（字段/方法条目）

`org.twelve.gcp.meta.FieldMeta`

最细粒度的元数据单元，表示一个命名成员（数据字段或方法）。

```java
FieldMeta f = members.get(0);
f.name();         // "code"
f.type();         // "String"
f.description();  // 字段声明前的注释文本（可为 null）
f.origin();       // "own"（自身定义）/ "base"（继承自基类）/ "builtin"（系统内置）
f.isMethod();     // type 包含 "->" 时返回 true（如 "Unit -> String"）

// JSON 序列化
Map<String, Object> map = f.toMap();
// {name:"code", type:"String", description:"...", origin:"own"}
```

| `origin` 值 | 含义 |
|---|---|
| `"own"` | 在该 outline 中直接声明 |
| `"base"` | 从基类（extends）继承 |
| `"builtin"` | 系统内置方法（如 `to_str`、`filter`、`count`） |
| `"inferred"` | 从结构访问模式推断（lambda 参数 `c` 被访问过 `c.code`） |

---

## 五、SymbolMeta（作用域内可见符号）

`org.twelve.gcp.meta.SymbolMeta`

在某个源码位置可见的单个绑定（变量、outline、函数、参数）。

```java
SymbolMeta sym = meta.resolve("c", offset);
sym.name();     // "c"
sym.type();     // "Country"（推导得到的具体类型名）
sym.kind();     // "parameter" / "variable" / "outline" / "function"
sym.mutable();  // false（let 参数）/ true（var 声明）
```

---

## 六、ImportMeta / ExportMeta

```java
// ImportMeta
imp.symbol();      // 导入的原始符号名（"countries"）
imp.as();          // 别名（null 表示无别名）
imp.from();        // 来源模块名（"geo"）
imp.description(); // 行注释

// ExportMeta
exp.name();           // 本地名称（"countries"）
exp.exportedAs();     // 导出别名（null 表示与本地名相同）
exp.description();    // 行注释
```

---

## 七、ForestMeta（多模块森林元数据）

`org.twelve.gcp.meta.ForestMeta`

由 `asf.meta()` 返回，汇总所有模块的元数据。

```java
ForestMeta forest = asf.meta();
forest.modules();         // 所有模块的 ModuleMeta 列表（按插入顺序）
forest.find("geo");       // 按模块名查找（不存在返回 null）
forest.toMap();           // JSON 序列化（包含 modules 数组）
```

---

## 八、完整工作流（LLM 代码理解示例）

```java
// 1. 推导完成后提取元数据
ModuleMeta meta = ast.meta();

// 2. 将完整模块结构序列化给 LLM（作为系统提示上下文）
Map<String, Object> ctx = meta.toMap();
llm.setContext(ctx);

// 3. LLM 需要"在光标 pos=87 处 c. 后面有哪些成员"
List<FieldMeta> members = meta.membersOf("c", 87L);
//  → [{name:"code", type:"String", origin:"own"},
//     {name:"name", type:"String", origin:"own"},
//     {name:"to_str", type:"Unit -> String", origin:"builtin"}]
llm.suggestMembers(members);

// 4. LLM 需要当前可见的全部绑定（自动补全候选）
List<SymbolMeta> visible = meta.visibleSymbols(87L);
// → [{name:"c", type:"Country", kind:"parameter"}, {name:"countries", ...}, ...]
llm.suggestSymbols(visible);

// 5. LLM 需要某个 Outline 的全部成员（直接从 Outline 对象查询）
Outline o = /* 从符号表获取的 Outline 对象 */;
List<FieldMeta> allMembers = MetaExtractor.fieldsOf(o);
```

---

## 九、Lambda 参数成员解析（关键机制）

这是 GCP 元数据 API 最重要的设计决策，专门为 `filter(c -> c.)` 这类场景设计。

**问题**：lambda 参数 `c` 的 Outline 是 `Genericable`（泛型变量），推导后其四个约束维度通常是：

```
extendToBe  = null
declaredToBe = null
hasToBe     = VirtualSet<Country> 的元素类型约束
definedToBe = {code: String}（只有被访问过的字段）
```

**解决**：GCP 推导器在泛型实例化（`projectGeneric`）时，将具体实体类型（`Country`）记录到 `projectedType` 字段。该字段**不参与推导约束链**（不影响任何 `is`/`addExtendToBe` 等操作），仅供元数据提取读取：

```java
// MetaExtractor.resolveOutline 的优先级 2
Outline proj = g.projectedType();  // → Country 实体
if (isConcrete(proj)) return resolveOutline(proj);
```

因此 `MetaExtractor.fieldsOf(cOutline)` 能正确返回 `Country` 的全部成员（包括未被访问过的 `name` 等字段），而不只是 `{code: String}` 这种结构推断的子集。
