# 并行约束：hasToBe 与 definedToBe 的正确语义

> 术语与符号约定遵循 `terminology.md`。

## 问题背景

GCP 的 Genericable 泛型变量携带四维约束链，最初设计为严格的线性顺序：

```
extendToBe <: declaredToBe <: hasToBe <: definedToBe
```

这意味着 `addHasToBe` 会检查 `hasToBe <: definedToBe`，`addDefinedToBe` 会把 `hasToBe` 当作 `definedToBe` 的上界进行验证。

**这个设计是错误的。**

## 根本原因

`hasToBe` 和 `definedToBe` 来自不同的推导源，各自独立地从下方约束变量类型：

| 维度 | 来源 | 含义 |
|------|------|------|
| `hasToBe` | 赋值上下文、match 模式、projection | "x 必须能被当作 T 使用" |
| `definedToBe` | 成员访问（`x.field`）、函数调用（`x(arg)`） | "x 必须拥有成员 M" |

两者之间**没有 `<:` 顺序关系**，不应互相检查。

## 正确语义：并行下界约束

```
declaredToBe <: (hasToBe ∪ definedToBe)
```

即：`hasToBe` 和 `definedToBe` 共同构成变量的"下界"（lower bound），变量的实际类型必须同时满足两者。

### 触发 Bug 的例子

```outline
let f = x -> {
    var y = {name="will"};
    y = x;             // x.hasToBe = {name:String}
    let age = x.age-1; // x.definedToBe = {age:Int}
    return age;
};
// x.min() 预期为 {name:String, age:Int}
```

旧模型执行过程（无论哪种顺序）：
- 添加 `hasToBe = {name:String}`，检查 `{name:String}.is({age:Int})` → `false` → **报错（错误！）**
- 或添加 `definedToBe = {age:Int}`，把 `hasToBe = {name:String}` 当上界，检查 `{name:String}.is({age:Int})` → `false` → **报错（错误！）**

## 修正后的约束模型

### `min()` 的新语义

`min()` 负责计算变量的**合并下界**：

```
min() =
  if declaredToBe ≠ Any → declaredToBe（优先级最高）
  else                  → mergeParallelConstraints(hasToBe, definedToBe)
```

`mergeParallelConstraints(hasToBe, definedToBe)` 的合并规则：

| hasToBe | definedToBe | 结果 |
|---------|-------------|------|
| Any | T | T |
| T | Any | T |
| hasToBe.is(definedToBe) | — | hasToBe（更具体） |
| — | definedToBe.is(hasToBe) | definedToBe（更具体） |
| `ProductADT && !Tuple` | `Entity` | `hasToBe.producePreservingBase(definedToBe)` |
| Primitive（非 Function/Tuple） | `Entity` | `Entity(base=hasToBe, members=definedToBe.members)` |
| Function | Entity | 不兼容（报错在 add 时） |
| Tuple | named Entity | 不兼容（位置索引 vs 命名字段） |

### 为何排除 Tuple

`Tuple` 使用整数索引（`0, 1, 2...`），而 `definedToBe` 中的 Entity 使用字符串字段名（`name, age...`）。将两者 produce 合并会产生混合索引类型，语义不清晰，排除此 case。

### `producePreservingBase` vs `produce`

原有 `Entity.produce(Entity)` 方法创建 `base=Any` 的结果，会丢失 `hasToBe` 的 base 信息。

例如：
- `hasToBe = Entity(base=String, {name:String})`
- `definedToBe = Entity{age:Int}`
- `produce()` 结果：`Entity(base=Any, {name:String, age:Int})` ← **丢失了 String base**
- `producePreservingBase()` 结果：`Entity(base=String, {name:String, age:Int})` ← **正确**

### Primitive + Entity 的合并

用户示例：`let mystring = "aaa"{age=100}` 的类型为 `Entity(base=LiteralString("aaa"), {age:Int})`。

注：`mystring` 本身**不是 Genericable**，它是一个具体类型，通过 `extendToBe` 路径设置。
而在函数体内：`let f = x -> { x = "aaa"{age=100}; ... }` 时，`x` 才是 Genericable，
此时 `x.extendToBe = Entity(base=String, {age:Int})`。

当推导时 `hasToBe = STRING`（Primitive）且 `definedToBe = Entity{age:Int}` 并存，
`min()` 会创建 `Entity.from(node, STRING, [{age:Int}])` = `Entity(base=STRING, {age:Int})`。

## 修改的代码位置

### 1. `Genericable.addHasToBe()` 
移除对 `definedToBe` 的 downConstraint 链式检查。

### 2. `Genericable.addDefinedToBe()`
修改 `upConstraint` 不再使用 `hasToBe`（两者是并行的），只用 `declaredToBe ?? extendToBe`。

### 3. `Genericable.addExtendToBe()`
`downConstraint` 改用 `min()`（组合后的下界），而不是只取 `hasToBe ?? definedToBe` 中的一个。

### 4. `Genericable.min()`
引入私有静态方法 `mergeParallelConstraints(AST, Outline, Outline)`，计算 hasToBe + definedToBe 的合并下界。

### 5. `Genericable.project(Reference, OutlineWrapper)`
移除 `hasToBe <: definedToBe` 的显式断言，改用 `mergeParallelConstraints` 计算 benchMark。

### 6. `Entity.producePreservingBase(Entity)`
新增方法，在 produce 合并字段的同时保留 `this.base()`。

## 约束链更新后的文档

```
extendToBe  ← 上界（赋给它的实际值类型）
    |
    ↓  <: 关系（extendToBe 必须满足 min()）
  ┌─────────────────────────────────────┐
  │  min() = merged lower bound         │
  │                                     │
  │  declaredToBe ← 显式注解（最高优先级）│
  │       ||（并行，无顺序）              │
  │  hasToBe  ← 使用约束（赋值上下文等） │
  │  definedToBe ← 结构访问约束         │
  └─────────────────────────────────────┘
```
