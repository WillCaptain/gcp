# GCP — 核心 API 参考

**包路径** `org.twelve.gcp`  
**Maven 模块** `gcp`

---

## 概览

GCP 对外暴露的核心接口层：

| 类 | 职责 |
|---|---|
| `AST` | 单模块抽象语法树——推导入口、错误收集、元数据提取 |
| `ASF` | 抽象语法森林——多模块协调推导与执行 |
| `OutlineInterpreter` | 树行走解释器——执行 Outline 程序，返回运行时值 |
| `GCPConfig` | 运行时配置——插件目录、参数覆盖 |
| `GCPBuilderPlugin` | 外部插件 SPI——将 Java 对象注入 Outline 运行时 |
| `GCPError` | 结构化错误——代码 + 位置 + 说明 |
| `GCPErrCode` | 40+ 错误码枚举（含类别分类） |

---

## 一、AST（单模块语法树）

`org.twelve.gcp.ast.AST`

单个 Outline 模块的核心对象，通常由 `outline-parser` 模块中的 `OutlineParser.parse(src)` 创建并填充。GCP 通过 `AST` 完成推导、错误收集、执行和元数据提取。

### 1.1 类型推导

```java
// 触发类型推导（填充符号表）
Module module = ast.infer();

// 检查是否全部推导成功（true = 无未解析节点）
boolean ok = ast.inferred();

// 获取未能推导的节点列表（调试用）
List<Node> missed = ast.missInferred();
```

| 方法 | 返回 | 说明 |
|---|---|---|
| `infer()` | `Module` | 对整个模块触发类型推导；返回导出的 Module 接口 |
| `inferred()` | `boolean` | 检查所有节点类型是否已收敛（清空 missInferred 缓存后重新检查） |
| `missInferred()` | `List<Node>` | 上次 `inferred()` 后仍未解析的节点列表 |

### 1.2 错误收集

```java
// 推导完成后检查错误
List<GCPError> errors = ast.errors();
for (GCPError e : errors) {
    System.out.println(e);
    // "[error] field not found – 'c.aa'  @line 3:12"
}

// 手动添加错误（推导规则内部使用）
ast.addError(new GCPError(node, GCPErrCode.FIELD_NOT_FOUND, "detail"));

// 清除指定节点的所有错误
ast.clearNodeErrors(nodeId);

// 清除范围内的错误（[from, to)，用于增量重推导）
ast.clearErrors(from, to);
```

| 方法 | 说明 |
|---|---|
| `errors()` | 全部已收集错误的只读列表 |
| `addError(GCPError)` | 添加错误（内置去重：同一节点 + 同一错误码只记录一次） |
| `clearNodeErrors(Long nodeId)` | 移除指定节点的所有错误 |
| `clearErrors(int from, int to)` | 移除 `errors()` 中下标 [from, to) 的错误（保持去重集同步） |

### 1.3 元数据提取

```java
// 获取模块完整元数据（推导后调用）
ModuleMeta meta = ast.meta();
meta.outlines();                   // Outline 类型声明列表
meta.variables();                  // 变量声明列表
meta.functions();                  // 函数声明列表
meta.imports();                    // import 规格列表
meta.exports();                    // export 规格列表
meta.toMap();                      // JSON 可序列化 Map

// 位置感知（IDE/LLM dot-completion）
meta.scopeAt(offset);              // 光标位置所在的最内层作用域
meta.resolve("name", offset);      // 在指定位置解析符号
meta.visibleSymbols(offset);       // 指定位置可见的所有符号
meta.membersOf("c", offset);       // 符号 c 在 offset 处的成员列表
```

详细参见 [meta_api.md](meta_api.md)。

### 1.4 源码与名称

```java
// 模块名称（由 OutlineParser 填充）
String name = ast.name();

// 命名空间（namespace 声明）
String ns = ast.namespace().lexeme();

// 模块源码（由 OutlineParser.setSourceCode 注入，元数据提取时使用）
ast.setSourceCode(src);
String src = ast.sourceCode();
```

### 1.5 符号表访问

```java
// 获取符号环境（仅高级用法，如插件需读取特定符号）
LocalSymbolEnvironment env = ast.symbolEnv();
```

---

## 二、ASF（抽象语法森林）

`org.twelve.gcp.ast.ASF`

多模块程序的根容器。通过共享的 `GlobalSymbolEnvironment` 支持跨模块导入/导出，采用固定点迭代算法完成全量类型推导。

### 2.1 创建与组合

```java
ASF asf = new ASF();
AST math = asf.newAST();    // 创建并注册模块 "math"
AST main = asf.newAST();    // 创建并注册模块 "main"
// 然后由 OutlineParser 分别填充 math 和 main
```

| 方法 | 说明 |
|---|---|
| `newAST()` | 创建新 AST 并注册到本 ASF |
| `asts()` | 所有已注册 AST 的只读列表（按插入顺序） |
| `get(String name)` | 按模块名查找 AST；不存在时抛出 `GCPRuntimeException` |
| `globalEnv()` | 全局符号环境（跨模块共享） |

### 2.2 多模块推导

```java
// 固定点推导（三阶段）：
//  1. 预注册所有模块 shell（解决互相引用的先鸡先蛋问题）
//  2. 所有模块首轮推导（跨模块符号暂以 LazyModuleSymbol 占位）
//  3. 反复推导直到收敛（默认最多 100 轮）
boolean allResolved = asf.infer();

// 检查是否已全部收敛
boolean ok = asf.inferred();
```

| 方法 | 说明 |
|---|---|
| `infer()` | 全量固定点推导；返回是否全部收敛 |
| `inferred()` | 检查所有 AST 是否已收敛 |
| `isLastInfer()` | 是否为最后一轮迭代（推导规则内部使用） |

### 2.3 错误汇总

```java
// 收集所有模块的错误（扁平列表，按模块插入顺序）
List<GCPError> allErrors = asf.allErrors();

// 是否有任意错误
boolean anyError = asf.hasErrors();
```

| 方法 | 说明 |
|---|---|
| `allErrors()` | 所有模块错误的扁平列表 |
| `hasErrors()` | 任意模块有错误时返回 true |

### 2.4 执行

```java
// 使用默认解释器执行（无需自定义配置时的便捷方法）
Value result = asf.interpret();

// 自定义解释器（需要注册外部插件时）
OutlineInterpreter interp = new OutlineInterpreter();
interp.registerConstructor("my_repo", (id, typeArgs, valueArgs) -> new MyRepoValue(...));
Value result = interp.run(asf);
```

### 2.5 森林级元数据

```java
// 森林级元数据（所有模块汇总）
ForestMeta meta = asf.meta();
meta.modules();                    // 所有模块的 ModuleMeta 列表
meta.find("geo");                  // 按名称查找模块 ModuleMeta
meta.find("geo").outlines();       // geo 模块的 Outline 声明
meta.toMap();                      // JSON 可序列化 Map
```

---

## 三、OutlineInterpreter（解释器）

`org.twelve.gcp.interpreter.OutlineInterpreter`

树行走解释器，将推导完成的 AST/ASF 执行为运行时值（`Value`）。采用 Visitor 模式，每种节点类型对应独立的无状态 `Interpretation` 类。

### 3.1 创建

```java
// 标准创建（自动加载 gcp.properties + 插件目录）
OutlineInterpreter interpreter = new OutlineInterpreter();

// 自定义配置（指定插件目录）
GCPConfig cfg = GCPConfig.load().with("plugin_dir", "/app/plugins");
OutlineInterpreter interpreter = new OutlineInterpreter(cfg);

// 绑定 ASF（便于 run() 无参调用）
OutlineInterpreter interpreter = new OutlineInterpreter(asf);
```

### 3.2 执行

```java
// 执行整个 ASF（多模块，按插入顺序）
Value result = interpreter.run(asf);

// 执行单个 AST
Value result = interpreter.runAst(ast);

// 执行绑定的 ASF（构造时绑定）
Value result = interpreter.run();
```

| 方法 | 返回 | 说明 |
|---|---|---|
| `run(ASF)` | `Value` | 顺序执行所有模块，返回最后一个模块最后一条语句的值 |
| `runAst(AST)` | `Value` | 执行单个模块（处理 import/export） |
| `run()` | `Value` | 执行构造时绑定的 ASF |

### 3.3 注册外部插件

```java
// 注册 Java 对象构造器（用于 __name__<TypeArg> 语法）
interpreter.registerConstructor("my_repo",
    (id, typeArgs, valueArgs) -> new MyEntityRepoValue(typeArgs.get(0)));

// 从目录动态加载插件 JAR（自动发现 ext_builder_*.jar）
interpreter.loadPlugins(Path.of("/additional/plugins"));
```

| 方法 | 说明 |
|---|---|
| `registerConstructor(String name, SymbolConstructor fn)` | 注册 `__name__<T>` 构造器；返回 this（可链式调用） |
| `loadPlugins(Path dir)` | 扫描目录的 ext_builder_*.jar，通过 ServiceLoader 注册；返回 this |

### 3.4 运行时值类型（Value 继承树）

| Value 类型 | Outline 对应 | Java 原生类型 |
|---|---|---|
| `StringValue` | String | `String` |
| `IntValue` | Int / Long | `long` |
| `FloatValue` | Float / Double / Number | `double` |
| `BoolValue` | Bool | `boolean` |
| `UnitValue` | Unit | 单例 |
| `ArrayValue` | `[T]` | `List<Value>` |
| `DictValue` | `[K:V]` | `Map<Value,Value>` |
| `FunctionValue` | `A -> B` | 闭包（含 `Interpreter` 引用） |
| `TupleValue` | `{field: T}` | `Map<String,Value>` |
| `EntityValue` | 命名实体 | 字段映射 + 方法注册 |

---

## 四、GCPConfig（运行时配置）

`org.twelve.gcp.config.GCPConfig`

四层优先级查找（高→低）：

1. **程序化覆盖**（`with(key, value)` 设置）
2. **JVM 系统属性**（`-Dplugin_dir=/path`）
3. **环境变量**（`PLUGIN_DIR=/path`）
4. **类路径默认值**（`gcp.properties`）

```java
// 标准加载
GCPConfig cfg = GCPConfig.load();
String dir = cfg.getString("plugin_dir");  // "ext_builders"（默认）
Path   p   = cfg.getPath("plugin_dir");

// 覆盖单个 key（不影响其他 key，返回新实例）
GCPConfig testCfg = GCPConfig.load().with("plugin_dir", "target/test-plugins");
new OutlineInterpreter(testCfg);
```

| 方法 | 说明 |
|---|---|
| `GCPConfig.load()` | 按四层优先级加载配置 |
| `with(String key, String value)` | 在最高优先级覆盖指定 key（不可变式链式覆盖） |
| `getString(String key)` | 获取字符串值（不存在返回 null） |
| `getString(String key, String default)` | 获取字符串值（不存在返回默认值） |
| `getPath(String key)` | 获取 Path 对象 |
| `getPath(String key, Path default)` | 获取 Path 对象（不存在返回默认路径） |

---

## 五、GCPBuilderPlugin（外部插件 SPI）

`org.twelve.gcp.plugin.GCPBuilderPlugin`

将外部 Java 对象（如数据库连接、外部实体仓库）注入 Outline 运行时的插件接口。

### 5.1 Outline 调用语法

```outline
let repo = __my_repo__<Employee>;  // 调用 id="my_repo" 的插件，类型参数 "Employee"
let db   = __sqlite__<Person>("jdbc:sqlite:...");  // 带值参数
```

### 5.2 实现接口

```java
public class MyRepoPlugin implements GCPBuilderPlugin {
    @Override
    public String id() {
        return "my_repo";  // 对应 __my_repo__ 中的 "my_repo"
    }

    @Override
    public Value construct(String id, List<String> typeArgs, List<Value> valueArgs) {
        String entityType = typeArgs.isEmpty() ? "Unknown" : typeArgs.get(0);
        return new MyEntityRepoValue(entityType);
    }
}
```

| 方法 | 说明 |
|---|---|
| `id()` | 构造器标识符（不含 `__`），对应 Outline 中 `__id__` 的 `id` 部分 |
| `construct(id, typeArgs, valueArgs)` | 构造运行时值；`typeArgs` 为类型参数字符串列表，`valueArgs` 为调用参数 |

### 5.3 自动加载（JAR 插件）

将插件打包为 `ext_builder_myrepo.jar`，在 JAR 的 `META-INF/services/org.twelve.gcp.plugin.GCPBuilderPlugin` 文件中写入实现类全名，放入 `plugin_dir` 目录，解释器启动时自动发现并注册。

### 5.4 程序化注册（嵌入用法）

```java
// 直接注册 lambda，无需 JAR 文件
interpreter.registerConstructor("my_repo",
    (id, typeArgs, valueArgs) -> new MyEntityRepoValue(typeArgs.get(0)));
```

---

## 六、典型使用场景（端到端示例）

### 场景 A：单模块推导 + 验证

```java
// 由 outline-parser 解析并填充 AST
ASF asf = OutlineParser.parseAsf("""
    let x = 42;
    let greeting = "hello " + to_str(x);
    """);

// 推导
asf.infer();

// 检查错误
if (asf.hasErrors()) {
    asf.allErrors().forEach(e -> System.out.println(e));
    // "[error] type mismatch – 'x + "str"'  @line 2:15"
    return;
}

// 执行
Value result = asf.interpret();
```

### 场景 B：多模块跨引用

```java
ASF asf = new ASF();

// 模块 1：定义类型
AST geoModule = asf.newAST();
OutlineParser.fill(geoModule, """
    namespace geo;
    outline Country { code: String, name: String }
    let countries = Countries();
    export countries;
    """);

// 模块 2：使用类型
AST mainModule = asf.newAST();
OutlineParser.fill(mainModule, """
    import { countries } from geo;
    let cn = countries.filter(c -> c.code == "CN").first();
    """);

// 固定点推导（自动解决跨模块引用）
asf.infer();
asf.allErrors().forEach(System.out::println);

// 执行
OutlineInterpreter interp = new OutlineInterpreter();
Value result = interp.run(asf);
```

### 场景 C：LLM 代码校验（GCP 作为 Outline 验证器）

```java
// LLM 生成的 Outline 代码
String llmCode = llm.generateCode(context);

// 构建 AST（通常由 outline-parser 完成）
ASF asf = OutlineParser.parseAsf(llmCode);
asf.infer();

if (asf.hasErrors()) {
    // 将结构化错误反馈给 LLM
    List<String> feedback = asf.allErrors().stream()
        .map(GCPError::toString)
        .toList();
    // "[error] field not found – 'c.codee'  @line 3:12"
    llm.retryWithFeedback(feedback);
} else {
    // 无错误，提取模块元数据供 LLM 下一步推理
    ModuleMeta meta = asf.asts().get(0).meta();
    Map<String, Object> context = meta.toMap();
    llm.continueWithContext(context);
}
```

### 场景 D：注入外部数据库插件（Entitir 模式）

```java
// 注册 SQLite 实体仓库构造器
OutlineInterpreter interpreter = new OutlineInterpreter();
interpreter.registerConstructor("sqlite_repo",
    (id, typeArgs, valueArgs) -> {
        String jdbcUrl = ((StringValue) valueArgs.get(0)).value();
        return new SQLiteEntityRepoValue(typeArgs.get(0), jdbcUrl);
    });

// Outline 代码使用 __sqlite_repo__ 构造
String code = """
    let repo = __sqlite_repo__<Employee>("jdbc:sqlite:mydb.sqlite");
    let managers = repo.filter(e -> e.is_manager == true);
    """;

ASF asf = OutlineParser.parseAsf(code);
asf.infer();
Value result = interpreter.run(asf);
```

---

## 七、线程安全说明

- `AST` / `ASF` 是**有状态对象**，不应在多线程间共享推导或执行过程
- `OutlineInterpreter` 持有运行时环境，**不可重入**，每次执行独立调用链
- `GCPConfig` 是**不可变的**，可安全共享
- `MetaExtractor` 是**纯函数**，可安全并发调用（每次调用构造独立对象）
