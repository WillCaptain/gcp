# GCP — 架构总览

## 定位

GCP（Generic Constraint Propagation，泛型约束传播）是 Outline 语言的核心类型推导引擎与运行时解释器。它对外提供：

- **类型推导**：基于约束传播的自动类型推断，无需显式类型注解
- **错误诊断**：结构化错误列表，含错误类别、源码位置、详细说明
- **元数据提取**：模块符号、类型字段、作用域链的可导航元数据树（供 IDE 和 LLM 使用）
- **解释执行**：树行走解释器，支持跨模块导入/导出、插件扩展
- **插件系统**：通过 `GCPBuilderPlugin` SPI 将外部 Java 对象注入 Outline 运行时

## 模块地图

```
gcp/
├── ast/
│   ├── AST.java                ← 单模块抽象语法树：推导入口、错误收集、元数据提取
│   ├── ASF.java                ← 抽象语法森林：多模块协调推导与执行
│   ├── Node.java               ← 节点接口（infer / interpret / loc）
│   └── Location.java           ← 源码位置（行列号 + 字节偏移量）
│
├── inference/
│   ├── OutlineInferencer.java  ← 推导器实现（Visitor 分发）
│   └── *Inference.java         ← 各节点类型的推导规则（共 ~40 个）
│
├── interpreter/
│   ├── OutlineInterpreter.java ← 解释器实现（树行走）
│   ├── value/                  ← 运行时值类型（Value 继承树）
│   ├── interpretation/         ← 各节点类型的执行逻辑（共 ~40 个）
│   └── Environment.java        ← 运行时词法作用域链
│
├── outline/
│   ├── Outline.java            ← 类型对象根接口
│   ├── primitive/              ← 原始类型（STRING/INTEGER/BOOL/NOTHING/ANY…）
│   ├── adt/                    ← 代数数据类型（Entity/ProductADT/Option）
│   ├── projectable/            ← 可投影类型（Genericable/Function/Reference…）
│   ├── builtin/                ← 内置类型（内置方法载体：String_/Integer_…）
│   └── decorators/             ← 类型装饰器（Lazy/This/OutlineWrapper）
│
├── meta/
│   ├── MetaExtractor.java      ← 元数据提取器（AST→ModuleMeta）
│   ├── ModuleMeta.java         ← 模块级元数据（含位置感知 API）
│   ├── FieldMeta.java          ← 字段/方法元数据
│   ├── ScopeMeta.java          ← 作用域快照（范围 + 符号列表）
│   ├── SymbolMeta.java         ← 符号元数据（名称 + 类型 + 位置）
│   ├── OutlineMeta.java        ← outline 声明元数据
│   ├── FunctionMeta.java       ← 函数声明元数据
│   └── ForestMeta.java         ← 多模块森林级元数据
│
├── exception/
│   ├── GCPError.java           ← 结构化错误（code + node + message）
│   └── GCPErrCode.java         ← 40+ 错误码枚举（含类别分类）
│
├── outlineenv/
│   ├── LocalSymbolEnvironment.java  ← 单模块符号表（推导时构建）
│   └── GlobalSymbolEnvironment.java ← 跨模块符号表（ASF 共享）
│
├── plugin/
│   ├── GCPBuilderPlugin.java   ← 外部插件 SPI（将 Java 对象注入 Outline）
│   └── PluginLoader.java       ← 从 JAR 文件自动加载插件
│
└── builder/
    └── ASTBuilder.java         ← 以编程方式构建 AST（测试 / 嵌入使用）
```

## 核心依赖关系

```
调用方（outline parser / entitir / playground …）
  │
  ├── OutlineParser.parse(src)         → AST  （单模块）
  │     │
  │     ├── ast.infer()                → 类型推导（填充符号表）
  │     ├── ast.errors()              → 错误列表
  │     ├── ast.meta()                → ModuleMeta（IDE/LLM 元数据）
  │     └── OutlineInterpreter.runAst(ast) → Value（执行结果）
  │
  └── ASF（多模块）
        ├── asf.newAST()              → 创建并注册模块
        ├── asf.infer()               → 跨模块固定点推导
        └── interpreter.run(asf)      → 多模块顺序执行
```

## 关键概念

### 约束传播推导（GCP）

GCP 不依赖 Hindley-Milner 合一，而是通过**四维约束**迭代收敛：

| 维度 | 含义 | 来源示例 |
|---|---|---|
| `extendToBe` | 上界约束（实际赋值/投影值） | `let x: Animal = dog` → x.extendToBe = Animal |
| `declaredToBe` | 显式注解约束 | `(c: Country) ->` → c.declaredToBe = Country |
| `hasToBe` | 使用约束（调用上下文） | `filter(c -> ...)` → c.hasToBe = 元素类型 |
| `definedToBe` | 结构访问约束 | `c.code` → c.definedToBe = {code: ?} |

此外有第五维 `projectedType`：由泛型实例化时记录，**不参与推导约束链**，专供元数据提取（IDE 成员补全）使用。

### Outline 类型层次

```
Outline（根接口）
  ├── Primitive      ← STRING / INTEGER / BOOL / NOTHING / ANY …
  ├── ProductADT     ← 积类型（有成员），Entity 是其子类
  │     └── Entity   ← 命名实体（可继承、可扩展）
  ├── Option         ← 和类型（A | B | C）
  ├── Function       ← 函数类型（FirstOrderFunction / HigherOrderFunction）
  ├── Genericable    ← 泛型变量（携带四维约束）
  │     └── AccessorGeneric  ← 成员访问表达式的泛型
  └── Reference      ← 类型参数（VirtualSet<a> 里的 a）
```

### 多模块推导（ASF 固定点算法）

1. **预注册**：所有模块 shell 写入 GlobalSymbolEnvironment，解决互相引用的鸡蛋问题
2. **首轮推导**：所有 AST 顺序推导，跨模块符号暂以 LazyModuleSymbol 占位
3. **固定点迭代**：反复推导直到所有节点收敛或达到最大轮数（默认 100 轮）

### 插件系统

外部 Java 对象通过 `__name__<TypeArg>` 语法注入 Outline 运行时：

```outline
let repo = __my_repo__<Employee>;  // 调用 id="my_repo" 的 GCPBuilderPlugin
```

插件 JAR 放入 `plugin_dir`，文件名须以 `ext_builder_` 开头，通过 ServiceLoader 自动发现。

## 构建方式

```java
// 单模块（最常见用法，通常由 outline-parser 模块封装）
ASF asf = new ASF();
AST ast = asf.newAST();
// ... 用 OutlineParser 或 ASTBuilder 填充 ast ...
asf.infer();
ast.errors();   // 类型错误列表
ast.meta();     // 模块元数据

// 执行
OutlineInterpreter interpreter = new OutlineInterpreter();
Value result = interpreter.run(asf);
```

## 文档索引

| 文档 | 说明 |
|---|---|
| [api/gcp_api.md](api/gcp_api.md) | 核心 API 参考（AST / ASF / 解释器 / 插件） |
| [api/meta_api.md](api/meta_api.md) | 元数据 API（MetaExtractor / ModuleMeta / FieldMeta…） |
| [type-system.md](type-system.md) | Outline 类型系统与 GCP 约束传播原理 |
| [error.md](error.md) | 错误诊断 API（GCPError / GCPErrCode） |
