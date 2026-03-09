# GCP — Outline 类型系统与约束传播原理

## 类型层次

```
Outline（根接口）
  │
  ├── Primitive（原始类型，不可变）
  │     ├── STRING          ← String 字面量类型
  │     ├── INTEGER         ← Int 整数类型
  │     ├── LONG            ← Long 长整型
  │     ├── FLOAT           ← Float 浮点
  │     ├── DOUBLE          ← Double 双精度浮点
  │     ├── NUMBER          ← Number 超类型（Integer ⊆ Number）
  │     ├── BOOL            ← Bool 布尔
  │     ├── SYMBOL          ← :symbol 字面量
  │     ├── NOTHING         ← Bottom type（无值，空集）
  │     └── ANY             ← Top type（所有类型的超类型）
  │
  ├── ProductADT（积类型，有成员）
  │     ├── Entity          ← 命名实体（struct/object）
  │     │     ├── 可继承（extends）基类 Entity
  │     │     ├── 可持有泛型参数（Reference 列表）
  │     │     └── 成员（EntityMember）= 字段 + 方法
  │     ├── Array           ← [T] 有序列表
  │     ├── Dict            ← [K:V] 字典
  │     └── Tuple           ← {field:T} 无名结构体
  │
  ├── Option（和类型）
  │     └── A | B | C       ← 多个类型的联合（按 arm 迭代成员）
  │
  ├── Function（函数类型）
  │     ├── FirstOrderFunction    ← A -> B（单参数）
  │     └── HigherOrderFunction   ← A -> B -> C（多参数 Curried）
  │
  ├── Genericable（泛型变量，携带四维约束）
  │     ├── Generic               ← 普通泛型变量
  │     └── AccessorGeneric       ← 成员访问表达式的推导变量（如 c.code 的结果）
  │
  ├── Reference（类型参数占位符）
  │     └── 如 VirtualSet<a> 中的 a
  │
  └── 内置装饰器
        ├── UNIT            ← Unit 空类型（unit value）
        ├── UNKNOWN         ← 未知类型（推导前占位）
        ├── Returnable      ← 函数调用结果（惰性解包为返回类型）
        ├── Lazy            ← 跨模块惰性引用
        └── This            ← `this` 关键字类型
```

---

## Genericable（泛型变量）详解

`Genericable` 是 GCP 类型推导的核心抽象，表示一个**尚未完全确定类型的变量**。
它的类型通过**四个约束维度**从多个方向逐步收窄。

### 四维约束链

```
  extendToBe  ← 上界（赋给它的实际值类型）
      |
      ↓  <: 关系
  declaredToBe ← 显式注解（程序员写明的类型）
      |
      ↓  <: 关系
  hasToBe     ← 使用约束（上下文要求它必须是什么）
      |
      ↓  <: 关系
  definedToBe ← 结构访问约束（通过调用 / 访问模式推断出的结构）
```

约束链的含义：每个维度的类型必须是下一个维度类型的子类型（`<:`）。推导器在每轮迭代中检查这些约束的一致性，若违反则报错。

### 第五维：projectedType（元数据专用）

`projectedType` 是独立于四维约束链之外的字段，**不参与推导**，仅供 IDE/LLM 元数据提取使用：

- 在泛型实例化（`projectGeneric`）时，若能从 `projection.max()` 或 `projection.min()` 得到具体实体类型，将其记录于 `projectedType`
- 用于解决 lambda 参数的成员补全问题：`c` 的 `definedToBe` 只包含已被访问过的字段，而 `projectedType` 记录了完整的 `Country` 实体

### 类型猜测（guess）

当推导尚未完全收敛时，`Genericable.guess()` 返回当前最佳近似：

```
guess() = extendToBe ?? declaredToBe ?? hasToBe ?? definedToBe ?? UNKNOWN
```

---

## 子类型关系（`is` 运算）

Outline 的 `outline.is(other)` 判断 `outline` 是否为 `other` 的子类型：

| 关系 | 示例 |
|---|---|
| 原始类型相同 | `INTEGER.is(INTEGER)` → true |
| 数字类型提升 | `INTEGER.is(NUMBER)` → true |
| Entity 继承 | `Employee.is(Person)` → true（Employee extends Person） |
| 通配符超类型 | `*.is(ANY)` → true（任何类型是 ANY 的子类型） |
| 底类型 | `NOTHING.is(*)` → true（NOTHING 是所有类型的子类型） |
| Option 包含 | `Option(A|B).is(C)` → 每个 arm 都是 C 的子类型时为 true |

---

## 泛型实例化（Projection）

泛型实例化是 GCP 处理多态函数调用（如 `filter`、`map`）的核心机制。

### 过程

```
VirtualSet<Country>.filter(c -> c.code == "CN")
  → filter 的类型参数 a = Country
  → c.hasToBe = Country
  → c 被赋予 projectedType = Country（用于元数据）
```

三类投影分派：

| 情形 | 方法 | 说明 |
|---|---|---|
| 投影目标是 Entity | `projectEntity` | 检查实体兼容性，记录 projectedType |
| 投影目标是 Function | `projectFunction` | 递归投影参数和返回值类型 |
| 投影目标是 Genericable | `projectGeneric` | 将 projection 约束传播到 this，记录 projectedType |

---

## 内置原始类型速查

| Outline 名称 | Java 类 | `is` 关系 |
|---|---|---|
| `String` | `STRING` | `STRING <: ANY` |
| `Int` | `INTEGER` | `INTEGER <: NUMBER <: ANY` |
| `Long` | `LONG` | `LONG <: NUMBER <: ANY` |
| `Float` | `FLOAT` | `FLOAT <: NUMBER <: ANY` |
| `Double` | `DOUBLE` | `DOUBLE <: NUMBER <: ANY` |
| `Number` | `NUMBER` | `NUMBER <: ANY` |
| `Bool` | `BOOL` | `BOOL <: ANY` |
| `Unit` | `UNIT` | `UNIT <: ANY` |
| `Nothing` | `NOTHING` | `NOTHING <: 所有类型` |
| `Any` | `ANY` | `ANY = 所有类型的超类型` |

---

## Option（和类型）

Option 表示值可以是多个类型之一，类似 Rust 的 `enum` 或 TypeScript 的 `A | B`：

```outline
outline Shape = Circle | Rectangle | Triangle;
```

类型检查时，Option 的成员是所有 arm 成员的**并集**（`MetaExtractor` 合并后去重）。
运行时通过 `match` 表达式分派：

```outline
match shape {
    Circle  -> shape.radius * 3.14,
    Rectangle -> shape.width * shape.height
}
```

---

## Entity 继承

Entity 支持单继承（`extends`）。子类继承父类所有成员，并可添加新成员：

```outline
outline Person { name: String, age: Int }
outline Employee extends Person { department: String, salary: Number }
```

成员解析顺序：子类自有成员 → 父类成员（`base`）。`FieldMeta.origin` 反映来源：
- `"own"` — 在该 Entity 中直接声明
- `"base"` — 继承自父类

---

## 函数类型

Outline 函数类型为 Curried 形式（类似 Haskell）：

```outline
// 一阶函数
let double = x -> x * 2;        // Int -> Int

// 多参数（Curried）
let add = x -> y -> x + y;      // Int -> Int -> Int

// 高阶函数
let apply = f -> x -> f(x);     // (a -> b) -> a -> b
```

`FirstOrderFunction`（单参数）和 `HigherOrderFunction`（多参数/高阶）是 `Function` 的两个子类。

GCP 能自动推导大多数函数类型，但对于 Church Numeral 等 Rank-2 多态需要显式类型注解（HM 类型系统的固有限制）。

---

## 类型推导局限性

GCP 基于约束传播，比 HM（Hindley-Milner）更灵活，但仍有以下已知局限：

| 场景 | 说明 |
|---|---|
| Rank-2 多态（Church Numerals） | `let zero = f -> x -> x` 的类型参数需要外部约束才能收敛 |
| 递归类型 | 递归 outline 定义需要谨慎设计约束方向 |
| 交叉类型 | 目前通过 `Poly` 近似处理，不支持显式交叉类型声明 |

遇到推导无法收敛时，GCP 在最大迭代次数（默认 100）后停止，剩余未解析节点通过 `ast.missInferred()` 报告。
