# 广义约束投影:一种四维类型推导方法及其在动态语言编译中的应用

## 摘要

动态编程语言的类型推导面临一个根本困难:函数参数的类型信息来自多个性质不同、相互独立的来源——函数体内的赋值决定了参数能承载什么值,程序员的显式声明给出了上界约束,调用点传入的实参类型反映了外部对参数的期望,而函数体内的结构化访问(如字段读取、方法调用)则揭示了参数必须具备的最小结构。现有方法——从Hindley-Milner统一到TypeScript的双向检查——将这些性质不同的信息源混入同一个约束集,导致虚假冲突并迫使开发者提供冗余标注。本文提出**广义约束投影(Generalized Constraint Projection, GCP)**,其核心思想是将上述四类信息分别建模为独立的约束维度(`extendToBe`、`declaredToBe`、`hasToBe`、`definedToBe`),通过格操作独立传播并在投影时联合求解,而非全局统一。我们在**Outline**语言上形式化该方法:定义类型格、具有单调性和收敛性证明的四维Genericable约束模型、作为鸭子类型语言结构化子类型关系的**Outline表达式匹配(OEM)**,以及用于多态高阶函数实例化的双向投影机制。我们证明了健全性(推导的类型是运行时类型的安全近似)、局部和全局收敛性(不动点算法在O(N·|𝕋|)步内终止,其中N为函数参数数,|𝕋|为类型格大小)以及顺序无关性(推导结果与模块处理顺序无关)。

我们进一步展示了GCP在Python提前编译(AOT)中的应用。`mypyc`编译器可将带类型标注的Python编译为C扩展,通常实现5-100倍加速,但其实际障碍不是编译器质量而是*标注覆盖率*:缺少显式参数类型的函数几乎没有收益(在我们的整数密集型基准测试中平均仅**1.30倍**,一个案例甚至因mypyc装箱开销退化到0.81倍)。我们提出**GCP-Python**,一个需求驱动的推导流水线,通过从调用点向函数参数传播类型约束,自动为零标注Python源文件注入符合PEP 484的类型标注。该流水线无需开发者工作:给定一个库文件和一个代表性调用上下文文件,GCP-Python在**66毫秒**的分析开销内生成完全标注的源代码,可供mypyc编译。在22个Python程序类别上评估,GCP-Python相对CPython实现算术平均**17.1倍**加速,峰值包括match/case分派的**120倍**、默认参数算术的**106倍**以及真实数论代码(TheAlgorithms/Python)的**34倍**。在7函数oracle基准测试中,GCP-Python在ARM64上达到手工标注性能的**101.8%**,在Linux x86-64上达到**99.7%**,且无需开发者工作,并在ARM64上**优于预热的Numba JIT**(`is_prime(997)`: mypyc(GCP) 14.67倍 vs Numba 7.66倍;`factorial(10)`: 8.53倍 vs 2.78倍)——无需代码修改且无JIT预热延迟。

本文描述了GCP在**Entitir**本体数据平台的生产实现,其中GCP支持从Outline lambda谓词到SQL的类型安全查询编译,使LLM生成的查询能够安全地针对关系模式执行而不会出现动态类型失败。实验结果表明,GCP方法论在理论上具有健全性和收敛性保证,在实践中能够显著提升动态语言的性能和类型安全性。

**关键词**: 类型推导; 动态语言; 约束求解; 提前编译; Python; 结构化类型系统

---

## 1. 引言

### 1.1 研究背景与动机

开发者生产力与类型安全之间的张力是编程语言设计中最古老的问题之一。动态语言倾向于生产力——变量无需类型标注,对象可以在运行时获得新属性,函数可以接受任何形状的参数。代价在运行时支付:本可在编译时捕获的类型错误反而在生产环境中表现为隐晦的失败。

渐进类型系统试图弥合这一鸿沟,允许程序员根据需要进行标注。TypeScript是占主导地位的工业实例,通过在JavaScript之上分层结构化类型系统实现了广泛采用。然而,TypeScript仍然表现出促使GCP产生的三个痛点:

1. **冗余标注**。即使表达式的类型从其上下文中明确无误,TypeScript仍经常需要显式标注(例如,高阶函数调用上的泛型类型参数)。

2. **上下文不敏感**。变量的类型在其声明点固定;其类型不能在两个独立的调用上下文中以不同方式收窄,除非引入显式重载。

3. **繁琐的泛型推导**。编写正确的泛型高阶函数(例如,类型化的`filter`)需要冗长、易错的参数级泛型声明。

Python面临类似但更严重的挑战。作为数据科学、科学计算和后端服务的主导语言,Python的动态分派模型虽然灵活,但施加了显著的运行时开销:每次属性访问、每次算术操作、每次函数调用都要经过多态分派层,阻止CPU执行直线整数算术。对于计算密集型应用,这种成本是令人望而却步的;Python程序经常比等效的C代码慢10-100倍。

`mypyc`编译器为弥合这一差距提供了一条引人注目的路径。给定带有PEP 484类型标注的Python源代码,`mypyc`将每个标注的函数编译为C扩展(`.so`),消除类型化变量的装箱开销,并用直接的C函数调用替换动态方法分派。当热循环中的所有变量都携带具体类型时,mypyc编译的代码可以匹配手写C的性能。

问题在于*标注覆盖率*。要求开发者标注每个函数中的每个参数会破坏Python的生产力模型。许多库,特别是数值或数据处理代码,都是无标注编写的。当mypyc遇到未标注的参数时,它生成保守的装箱代码,几乎不优于解释器。

GCP通过统一机制解决了这两类问题:与其通过统一求解全局类型约束系统(如Hindley-Milner所做的),GCP为每个未解析的类型变量附加*四个独立的约束维度*,并随着信息从各个方向流入而迭代收窄它们。关键洞察是,四个约束方向——*实际赋值、程序员标注、使用上下文、结构化使用模式*——提供互补信息,它们的联合最小化唯一确定大多数类型而无需任何标注。

对于Python AOT编译,我们采用*需求驱动*、*调用点驱动*的类型推导流水线:通过分析代表性调用点实际传递的参数类型来推导库函数参数的类型。这是关键创新。对于仅作为`f(10, 20)`调用的函数`def f(x, y): return x + y`,需求推导得出`x: int, y: int`——实现完全的mypyc优化——而声明点推导根本无法解析`x`和`y`的类型。

### 1.2 现有方法的局限性

几种方法试图解决标注覆盖率差距,但都有局限性:

- **mypy**: 从函数体推导返回类型,但没有调用点信息无法推导参数类型。`def f(x): return x + 1`产生`x: Unknown`。
- **Pyright / Pylance**: 同样的局限——声明点推导,无需求传播。
- **Cython**: 需要带有显式C类型`cdef`声明的`.pyx`文件。与标准Python或mypyc不兼容。
- **Numba `@jit`**: 通过LLVM跟踪在运行时特化。无法生成与标准CPython导入系统兼容的静态`.so`文件。首次调用时产生JIT编译开销。
- **TypeScript**: 使用与OEM精神相似的结构化类型系统,但在推导引擎上有所不同。TypeScript使用带有"上下文类型"显式传播的双向类型检查(类似于`hasToBe`),但没有形式化完整的四维约束模型(extendToBe、declaredToBe、hasToBe、definedToBe并行约束)。TypeScript对高阶函数的泛型推导通常需要显式类型标注,而GCP的投影机制自动处理常见情况。

GCP占据了这些方法之间的空白:静态分析(无运行时开销)、调用点驱动(无手工标注)、标准兼容输出(PEP 484标注→标准mypyc)。

### 1.3 本文贡献

本文提出了广义约束投影(GCP)方法论及其在两个不同领域的应用,具体贡献包括:

**理论贡献:**

1. **四维约束模型**(§3):将四种性质不同的类型信息源(实际值、程序员声明、使用需求、结构化访问模式)分离为独立的约束维度,并独立传播它们。这避免了将这些源混为一谈时产生的虚假约束冲突,并自然处理动态语言的后期绑定模式。

2. **Outline表达式匹配(OEM)**(§3.5):鸭子类型的形式化,通过递归成员集包含定义结构化子类型。双向委托协议(`tryIamYou` / `tryYouAreMe`)使OEM成为开放扩展点,可以容纳新的Outline类型而无需修改现有代码。我们证明OEM是预序关系,确认结构化子类型是传递的,因此可用作连贯的类型格。

3. **双向投影**(§3.6):一种泛型实例化机制,同时从调用点上下文向lambda参数传播类型信息(实现类型安全的filter/map操作),并从lambda体结果向调用点返回类型传播。正交的`projectedType`字段支持IDE元数据提取而不干扰推导。

4. **形式化保证**:我们证明了健全性(推导的类型是运行时类型的安全近似)、局部和全局收敛性(不动点算法在O(N·|𝕋|)步内终止)以及顺序无关性(推导结果与模块处理顺序无关)。

**应用贡献:**

5. **Outline语言应用**(§4):在Entitir本体数据平台中的生产实现,其中GCP支持从Outline lambda谓词到SQL的类型安全查询编译。GCPToSQL编译器展示了GCP推导的类型信息足够精确以驱动N-hop外键SQL生成。

6. **Python零标注AOT编译**(§5):
   - **Python三维约束推导**:通过同时传播三个互补约束维度(`extendToBe`、`hasToBe`、`definedToBe`)推导符合PEP 484的类型的联合推导流水线。该流水线通过转换器流水线处理20种Python语法模式,在将它们馈送到GCP约束求解器之前规范化语言特性。
   - **FunctionSpecializer**:通过为每个观察到的类型元组生成一个完全标注的函数副本来处理多态调用点的单态化过程,使mypyc能够将每个特化编译为类型特定的C代码。

7. **全面评估**(§6):跨22个程序类别、60+基准函数的结果,涵盖Python算术、控制流和函数式模式的全部范围。我们报告CPython、mypyc(bare)、mypyc(GCP)、mypyc(manual-oracle)和Numba(jit)的计时。在7函数核心基准测试中,GCP自动达到手工标注性能的**101.8%**,并在调用密集型模式上**优于预热的Numba JIT**。对TheAlgorithms/Python的真实验证确认了从开源仓库逐字提取的代码平均18.51倍加速。

### 1.4 论文组织结构

本文其余部分组织如下。§2回顾相关工作。§3形式化GCP方法论:四维约束模型、OEM结构化子类型、双向投影机制以及正确性证明。§4描述Outline语言应用和Entitir平台。§5详细介绍GCP-Python实现,包括需求驱动推导和函数单态化。§6呈现全面的实验评估。§7讨论与现有系统的比较和局限性。§8总结全文。

---

## 2. 相关工作

### 2.1 类型推导理论

基础工作是Milner的算法W及其对递归类型的扩展。GCP的基于约束的方法与Pottier和Rémy的工作更密切相关,他们将HM推广到支持子类型的基于约束的框架。

经典的基于约束的类型推导生成一组等式或子类型约束并同时求解它们。这对Hindley-Milner语言效果很好,但在处理动态模式时遇到困难,例如:
- 变量接收一种类型的值(`x = dog`)同时被标注为超类型(`x: Animal`)
- 同一变量在一个表达式中作为`Number`被使用(由于上下文),但在其他地方结构化地用作可调用的`a → b`

根本困难在于,这些是*四种性质不同的类型信息源*,它们在推导过程遍历AST时异步到达。将它们视为单一的无差别约束集混淆了它们的不同角色,导致虚假的约束冲突。

Hindley-Milner(HM)类型推导算法通过Robinson统一生成全局类型等式约束集并求解它们。GCP在三个基本方面有所不同:

| 维度 | Hindley-Milner | GCP |
|------|---------------|-----|
| **约束结构** | 类型等式集`τ = σ` | 每个函数参数四个方向不等式 |
| **求解方法** | 全局统一(Robinson) | 局部每参数约束收窄 |
| **增量更新** | 变化时需要完全重新推导 | 单变量约束添加是O(1) |
| **动态扩展** | 无原生支持 | `extendToBe`捕获声明后赋值 |
| **渐进类型** | 通过显式`dyn`类型 | 通过`UNKNOWN`/`ANY`初始值 |

GCP方向性方法的关键优势是,它自然处理动态语言的*后期绑定*模式:随着更多周围代码被分析,变量的类型可以逐步细化,而无需重新分析已处理的代码。

### 2.2 渐进类型系统

Pierce和Turner的渐进类型系统及其后续阐述允许混合类型化和非类型化代码。渐进类型的抽象解释提供了与GCP约束模型密切相关的格论基础。GCP的`UNKNOWN`类型和惰性推导在渐进系统中扮演与`?`类型类似的角色,但完全是内部的——它们不出现在面向用户的类型语言中。

### 2.3 结构化类型系统

面向对象语言的结构化类型系统已有广泛研究。GCP的OEM是鸭子类型的形式化,与类型理论中的记录子类型密切相关,但扩展到处理循环结构化类型。Liskov-Wing子类型的行为概念补充了OEM:OEM是语法/结构化的,而Liskov-Wing子类型是行为的。GCP专门针对结构化方面。

与GCP最可比的工业规模类型系统是TypeScript。TypeScript对JavaScript应用类似的结构化类型系统,强调过程间推导,但与TypeScript一样,它没有分离GCP维护的四个类型信息维度。理解TypeScript不健全性的工作促使了GCP更有纪律的约束模型。Facebook的Flow对JavaScript应用类似的结构化类型系统,强调过程间推导,但与TypeScript一样,它没有分离四个维度。

### 2.4 需求驱动类型推导

从使用点向定义点传播类型需求与Mycroft的多态类型推导以及基于约束推导的*自底向上*变体密切相关。GCP的`hasToBe`传播是这一传统中的调用点需求约束,应用于动态类型宿主语言。

### 2.5 Python编译技术

**Cython**需要手工C类型声明。**Numba**使用LLVM JIT在调用时特化,产生JIT开销和非确定性编译产物。**Nuitka**是一个提前Python到C++编译器,不需要标注但执行全程序分析;它不与mypyc `.so`模型集成,也不与CPython扩展API即插即用兼容。**mypy**本身可以推导一些类型,但其推导是*声明点驱动*的:它从函数自己的体和显式标注解析函数的参数类型,忽略调用点实际传递的类型。**PyPy**通过元跟踪JIT在无标注情况下实现5-10倍平均加速,但作为替代运行时,它无法生成与CPython生态系统兼容的标准`.so` C扩展。

GCP-Python占据了这些方法之间的空白:静态分析(无运行时开销)、调用点驱动(无手工标注)、标准兼容输出(PEP 484标注→标准mypyc)。

---

## 3. GCP方法论

### 3.1 动机与核心思想

动态语言的类型推导面临一个根本挑战:同一个函数参数的类型信息来自四个性质不同的源:

1. **值源**:参数在函数体内被赋予什么值? (函数体内`x = 42` → `x`的`extendToBe`至少是`Int`)
2. **声明源**:程序员声明了什么类型? (函数体内`x: Number` → `x`的`declaredToBe`是`Number`)
3. **需求源**:调用点传递什么类型的实参? (函数体内`let y:Int = x` → `f`的参数`x`的`hasToBe`收集到`Int`)
4. **结构源**:参数如何被访问? (函数体内`x.name` → `x`的`definedToBe`必须有`name`字段)

现有类型系统将这四种信息混为一个约束集,导致:
- 约束冲突(例如,`x = 42`但`f`需要`String`)
- 过度约束(需要冗余标注)
- 推导失败(无法收敛)

GCP的核心思想:**将四个信息源分离为独立的约束维度,独立传播,最后通过格操作联合求解**。

### 3.2 四维约束模型

GCP中的每个函数参数都表示为一个**Genericable**对象,携带四个约束槽:

| 槽位 | 符号 | 初始值 | 更新操作 | 收敛方向 |
|------|------|--------|----------|----------|
| `extendToBe` | τ_e | ⊥ (NOTHING) | `τ_e := τ_e ⊓ τ` | 从 ⊤ 向下收敛 |
| `declaredToBe` | τ_d | ⊤ (ANY) | `τ_d := τ` (直接赋值) | 一次性设置 |
| `hasToBe` | τ_h | ⊤ (ANY) | `τ_h := τ_h ⊔ τ` | 从 ⊥ 向上收敛 |
| `definedToBe` | τ_f | ⊤ (ANY) | `τ_f := τ_f ⊔ τ` | 从 ⊥ 向上收敛 |

**约束添加语义:**

```
addExtend(τ):    extendToBe   := extendToBe ⊓ τ   // meet: 从 ⊤ 向下界收敛到实际赋值类型
addDeclared(τ):  declaredToBe := τ                 // 直接设置
addDemand(τ):    hasToBe      := hasToBe ⊔ τ       // join: 从 ⊥ 向上界收敛到调用点需求
addStructure(τ): definedToBe  := definedToBe ⊔ τ   // join: 从 ⊥ 向上界收敛到结构访问需求
```

**`hasToBe` 与 `definedToBe` 的合并:**

两者是**并行的下界约束**,分别来自调用点需求和函数体结构访问,投影时必须同时满足。记其合并为 `min(hasToBe, definedToBe)`,计算规则如下:

```
min(τ_h, τ_f):
  若 τ_h = ANY                          → τ_f
  若 τ_f = ANY                          → τ_h
  若 τ_h ⊑ τ_f                          → τ_h      (取更具体者)
  若 τ_f ⊑ τ_h                          → τ_f      (取更具体者)
  若两者均为 ProductADT                  → 结构合并(成员并集,保留 base)
  若一个为 Primitive,另一个为 Entity     → Entity(base=Primitive, members=Entity.members)
  其他(类型根本不兼容)                   → 类型错误
```

**完整约束链:**

函数定义时的合法性条件:

```
extendToBe ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)
```

函数调用时的投影条件(实参类型 τ_arg 必须落在约束链中):

```
extendToBe ⊑ τ_arg ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)
```

若 τ_arg 不满足此链,投影失败并报告类型错误。

**图1: GCP四维约束模型架构**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Genericable(x)                                       │
│                                                                         │
│  约束模型 (hasToBe 与 definedToBe 是并行约束,∪ 表达同时满足):            │
│  extendToBe ⊑ projected ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)      │
│                                                                         │
│  约束来源:                                                                │
│  τ_e ← 函数体内赋值 (x = 42)                    ← 上界(最高具体度,字段最多)│
│  τ_d ← 类型声明 (x: Number)                     ← 声明约束                │
│  τ_h ← 使用上下文 (y = x → hasToBe ⊓= type(y)) ┐ 下界(最低要求)          │
│  τ_f ← 函数体内结构访问 (x.name, x.age, x(arg)) ┘ τ_h ∪ τ_f = 需求并集   │
│                                                                         │
│  τ_h ∪ τ_f: 两个并行约束的需求并集(成员并集)      │
│  projected 有效范围: extendToBe(上界) ⊑ projected ⊑ (declaredToBe ⊑(hasToBe ∪ definedToBe))(下界) │
└─────────────────────────────────────────────────────────────────────────┘
```

**案例研究:函数参数的四维约束收敛过程完整演示**

为了直观展示GCP的四维约束如何逐步收敛到最终类型,我们设计一个entity对象的案例,展示属性集合的收敛效应。

**代码示例:**

```outline
// 定义一个处理用户的函数,参数user的类型需要推导
let processUser = user -> {
    // 步骤1: 函数体内赋值 - 扩展属性
    user = {name: "Alice", age: 25, email: "alice@example.com"};

    // 步骤2: 结构化访问 - 定义必需属性
    let userName = user.name;
    let userAge = user.age;

    // 步骤3: 传递给其他函数 - 使用上下文需求
    sendEmail(user);  // sendEmail需要 {email: String}

    return user.name;
}

// 调用点
processUser({name: "Bob", age: 30, email: "bob@example.com", city: "NYC"});
```

**图2: 参数`user`的四维约束收敛过程**

```
时间轴 →

初始状态 (函数定义时):
┌─────────────────────────────────────────────────────────┐
│ user: Genericable                                       │
│   extendToBe   = ⊥ (NOTHING)                           │
│   declaredToBe = ⊤ (ANY)                               │
│   hasToBe      = ⊤ (ANY)                               │
│   definedToBe  = ⊤ (ANY)                               │
│   projected     = ? (未解析)                             │
└─────────────────────────────────────────────────────────┘

步骤1: 分析函数体赋值 (user = {name, age, email})
┌─────────────────────────────────────────────────────────┐
│ 约束来源: 函数体内赋值                                   │
│ 操作: addExtend({name: String, age: Int, email: String})│
│                                                         │
│ user: Genericable                                       │
│   extendToBe   = {name: String, age: Int, email: String}│
│                  ← 最多属性 (3个)                        │
│   declaredToBe = ⊤                                      │
│   hasToBe      = ⊤                                      │
│   definedToBe  = ⊤                                      │
└─────────────────────────────────────────────────────────┘

步骤2: 分析结构化访问 (user.name, user.age)
┌─────────────────────────────────────────────────────────┐
│ 约束来源: 结构化访问模式                                 │
│ 操作: addStructure({name: String, age: Int})            │
│                                                         │
│ user: Genericable                                       │
│   extendToBe   = {name: String, age: Int, email: String}│
│   declaredToBe = ⊤                                      │
│   hasToBe      = ⊤                                      │
│   definedToBe  = {name: String, age: Int}               │
│                  ← 最少属性 (2个)                        │
└─────────────────────────────────────────────────────────┘

步骤3: 分析使用上下文 (sendEmail(user))
┌─────────────────────────────────────────────────────────┐
│ 约束来源: 调用点需求 (sendEmail需要 {email: String})    │
│ 操作: addDemand({email: String})                        │
│                                                         │
│ user: Genericable                                       │
│   extendToBe   = {name: String, age: Int, email: String}│
│   declaredToBe = ⊤                                      │
│   hasToBe      = {email: String}                        │
│   definedToBe  = {name: String, age: Int}               │
└─────────────────────────────────────────────────────────┘

最终收敛状态:
┌─────────────────────────────────────────────────────────┐
│ user: Genericable                                       │
│   extendToBe   = {name: String, age: Int, email: String}│
│                  ← 上界 (3个属性)                        │
│   declaredToBe = ⊤ (ANY)                               │
│   hasToBe      = {email: String}          ┐ 并行下界约束 │
│   definedToBe  = {name: String, age: Int} ┘             │
│                                                         │
│ 约束检查 (属性集合的子类型关系):                          │
│   extendToBe ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe) │
│   {name,age,email} ⊑ ⊤ ⊑ {name,age,email}  ✓           │
│                                                         │
│   {email:String} ∪ {name:String,age:Int}               │
│     = {name:String, age:Int, email:String} ← 需求并集    │
│     (hasToBe 与 definedToBe 的成员并集)                  │
│                                                         │
│   函数定义约束如下:                                     │
│   - 至少有 {name, age} (来自 definedToBe 并行约束)       │
│   - 至少有 {email} (来自 hasToBe 并行约束)               │
│   - 至多有 {name, age, email} (来自 extendToBe 上界)     │
│                                                         │
│   满足 extendToBe ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)    │
│            ✓ 约束建立成功                                   │
└─────────────────────────────────────────────────────────┘

步骤4: 调用点投影
┌─────────────────────────────────────────────────────────┐
│ 投影检查:                                               │
│   实参: {name: "Bob", age: 30, email: "bob@..."} │
│   实参有4个属性,(hasToBe ∪ definedToBe) 要求3个属性      │
│                                                         │
│   约束检查:                                             │
│   extendToBe ⊑ 实参类型 ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe) │
│   {name,age,email} ⊑ {name,age,email} ⊑ ⊤ ⊑ {name,age,email} │
│   ✓ 成立 (实参包含所有必需属性: name,age来自definedToBe, email来自hasToBe) │
│                                                         │
│ 投影结果:                                               │
│   user 实例化为 {name: String, age: Int, email: String} │
│   返回值类型: String (user.name)                        │
└─────────────────────────────────────────────────────────┘

错误示例: processUser({name: "Charlie", age: 35})
┌─────────────────────────────────────────────────────────┐
│ 投影检查:                                               │
│   实参: {name: "Charlie", age: 35}  缺少 email 属性     │
│                                                         │
│   约束检查:                                             │
│   extendToBe ⊑ τ_arg ⊑ declaredToBe ⊑ min(hasToBe, definedToBe) │
│   {name,age,email} ⊑ {name,age} ?                       │
│   ✗ 失败: {name,age} ⋢ {name,age,email}                 │
│     实参缺少 email,不满足 min(hasToBe,definedToBe) 下界  │
│                                                         │
│ 错误报告: 实参缺少必需属性 email                         │
└─────────────────────────────────────────────────────────┘

错误示例: processUser({name: "Alice", age: 35， email: "alice@...", city:"Beijing"})                                                                                                             
┌─────────────────────────────────────────────────────────┐                                    
│ 投影检查:                                                |
│   实参: {name: "Alice", age: 35， email: "alice@...", city:"Beijing"} 多了 city 属性|
|                                                         |                               
│   约束检查:                                              │   
|   extendToBe ⊑ 实参类型实参 ⊑ (hasToBe ∪ definedToBe):    │         
│   {name,age,email} ⋢ {name,age,email,city} ✗            │
|   ↑ 失败: 实参 city 不满足 extendToBe(上界要求)             │
│ 错误报告:                                                |
│   类型错误: 实参多了属性 city                              |└─────────────────────────────────────────────────────────┘
```

**关键观察:**

1. **独立传播**: 四个维度独立收集约束,互不干扰
2. **单调收敛**: `extendToBe` 从 ⊤ 向下(meet),`hasToBe`/`definedToBe` 从 ⊥ 向上(join),两端在约束链中间相遇
3. **冲突检测**: 若约束链 `extendToBe ⊑ declaredToBe ⊑ min(hasToBe, definedToBe)` 不成立,报告类型错误
4. **投影灵活性**: 同一函数可接受约束链范围内的任何实参类型,返回对应的实例化类型

**对比传统方法:**

| 方法 | 推导结果 | 问题 |
|------|---------|------|
| **Hindley-Milner** | 需要显式类型参数 | 无法处理函数体内赋值 |
| **TypeScript** | 需要手工标注 | 无法自动推导参数约束 |
| **GCP** | 自动推导Number,投影时实例化 | 无 ✓ |

### 3.3 单调性与收敛性

**定理3.1 (单调性)**: 每次约束添加操作都是单调的:
- `addExtend` 通过 meet(⊓) 单调递增:τ_e 从 ⊥ 向上收敛
- `addDemand` 和 `addStructure` 通过 join(⊔) 单调递减:τ_h、τ_f 从 ⊤ 向下收敛
- `declaredToBe` 只设置一次,不参与迭代

**定理3.2 (局部收敛性)**: 对于单个参数,约束序列在有限步内收敛:
- `extendToBe` 从 ⊥ 出发,每次 meet 操作单调递增,最多上升到 ⊤,需 O(|𝕋|) 步
- `hasToBe` 和 `definedToBe` 从 ⊤ 出发,每次 join 操作单调递减,最多下降到 ⊥,需 O(|𝕋|) 步
- 在有限高度的类型格中,单个参数的推导在 O(|𝕋|) 步内终止

**定理3.3 (全局收敛性)**: 对于N个函数参数的程序,推导算法在O(N·|𝕋|)步内终止。

**证明**: 每个参数最多经历|𝕋|次约束更新,N个参数总共O(N·|𝕋|)次更新。由于每次更新都是单调的且格有限高度,算法必然终止。□

**定理3.4 (健全性)**: 如果`projected(x) = τ`,则在运行时`x`的值类型是`τ`的子类型。

**证明**: `projected(x)` 满足约束链 `extendToBe ⊑ projected(x) ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)`,因此:
- `extendToBe ⊑ projected(x)`:projected 覆盖函数体内所有实际赋值类型
- `projected(x) ⊑ declaredToBe`:projected 满足程序员的显式声明
- `projected(x) ⊑ (hasToBe ∪ definedToBe)`:declaredToBe为空情况下，projected 同时满足调用点需求和结构访问需求

因此,`projected(x)` 是运行时类型的安全近似。

### 3.4 Outline语言类型系统

为了形式化GCP,我们定义**Outline**语言的类型系统。Outline是一个严格的、面向表达式的语言,具有一级函数、代数数据类型、模式匹配和模块系统。

**类型语法:**

```
τ ::=
    ⊤                          -- ANY: 顶类型
  | ⊥                          -- NOTHING: 底类型
  | prim                        -- 原始类型
  | Entity(name, [m_i : τ_i])  -- 命名实体及成员
  | Tuple([f_i : τ_i])         -- 匿名结构化记录
  | Array(τ)                   -- 同构有序集合
  | Dict(τ_k, τ_v)             -- 键值字典
  | Option(τ_1, …, τ_n)       -- 和类型(标记联合)
  | τ_1 → τ_2                  -- 一阶函数
  | G[C]                        -- 带约束C的泛型变量
  | ●                           -- UNIT
  | ?                           -- UNKNOWN (推导前占位符)
```

其中`prim ∈ {String, Int, Long, Float, Double, Number, Bool, Symbol}`,且`Number`是`Int | Long | Float | Double`的声明超类型。

**类型格:**

完整的类型格(𝕋, ≼)由结构化子类型关系定义。关键性质:

- **⊤ (ANY)**是最大元素: `∀τ. τ ≼ ⊤`
- **⊥ (NOTHING)**是最小元素: `∀τ. ⊥ ≼ τ`
- **数值提升**: `Int ≼ Number`, `Long ≼ Number`, `Float ≼ Number`, `Double ≼ Number`
- **实体继承**: 当B扩展A时,`Entity(B, ...) ≼ Entity(A, ...)`
- **函数逆变**: 当`τ₁ ≼ σ₁`且`σ₂ ≼ τ₂`时,`(σ₁ → σ₂) ≼ (τ₁ → τ₂)`

### 3.5 Outline表达式匹配(OEM):结构化子类型

OEM是GCP的结构化子类型关系,形式化了鸭子类型:"如果它走起来像鸭子,叫起来像鸭子,那它就是鸭子。"

**定义3.1 (OEM关系)**: 类型`τ`匹配类型`σ`(记作`τ ⊑ σ`)当且仅当`τ`的实例可以安全地用在期望`σ`的上下文中。

**OEM规则:**

1. **自反性**: `τ ⊑ τ`

2. **传递性**: 如果`τ ⊑ σ`且`σ ⊑ ρ`,则`τ ⊑ ρ`

3. **顶底规则**:
   - `∀τ. τ ⊑ ⊤`
   - `∀τ. ⊥ ⊑ τ`

4. **原始类型提升**:
   - `Int ⊑ Number`, `Long ⊑ Number`, `Float ⊑ Number`, `Double ⊑ Number`

5. **结构化记录子类型** (宽度子类型):
   - 如果`∀i. τᵢ ⊑ σᵢ`,则`Tuple([f₁:τ₁, ..., fₙ:τₙ, ...]) ⊑ Tuple([f₁:σ₁, ..., fₙ:σₙ])`
   - 即:子类型可以有更多字段(宽度),且对应字段类型协变

6. **函数子类型** (参数逆变,返回值协变):
   - 如果`σ₁ ⊑ τ₁`且`τ₂ ⊑ σ₂`,则`(τ₁ → τ₂) ⊑ (σ₁ → σ₂)`

7. **数组协变**:
   - 如果`τ ⊑ σ`,则`Array(τ) ⊑ Array(σ)`

8. **和类型**:
   - 如果`∀i. τᵢ ⊑ σ`,则`Option(τ₁, ..., τₙ) ⊑ σ`
   - 如果`∃i. τ ⊑ σᵢ`,则`τ ⊑ Option(σ₁, ..., σₙ)`

**定理3.5 (OEM是预序)**: OEM关系(𝕋, ⊑)是预序,即满足自反性和传递性。

**证明**: 自反性由规则1直接得出。传递性通过对类型结构的归纳证明:
- 原始类型:由数值提升链的传递性
- 结构化类型:由成员类型的归纳假设
- 函数类型:由参数和返回值的归纳假设
□

**双向委托协议:**

OEM的实现使用双向委托协议,使其成为开放扩展点:

```typescript
interface OEMCapable {
  tryIamYou(other: OutlineType): boolean   // "我能当作你吗?"
  tryYouAreMe(other: OutlineType): boolean // "你能当作我吗?"
}
```

当检查`τ ⊑ σ`时:
1. 首先尝试`τ.tryIamYou(σ)` - τ判断自己是否可以当作σ
2. 如果失败,尝试`σ.tryYouAreMe(τ)` - σ判断τ是否可以当作自己
3. 两者都失败则`τ ⊑ σ`不成立

这种双向协议允许新类型扩展OEM而无需修改现有类型的代码。

### 3.6 泛型实例化:双向投影

GCP的投影机制处理多态高阶函数的类型实例化。关键挑战是:给定一个泛型函数调用,如何确定类型参数?

**问题示例:高阶函数与向后约束传播**

考虑一个更典型的高阶函数场景,展示GCP如何通过双向投影检测深层结构化类型冲突:

```outline
// 高阶函数: 提升选择器和谓词的组合
let lift = sel → pred → entity → pred(sel(entity));

// 选择器和谓词定义
let get_score  = player → player.test;
let is_passing = n → n.points >= 60;   // 需要 n.points : Int
let is_ace     = n → n.score  >= 90;   // 需要 n.score  : Int

// 组合函数
let check_pass = lift(get_score)(is_passing);
let check_ace  = lift(get_score)(is_ace);

// 实体定义: alice.test 有 {score, gpa} 但没有 {points}
let alice = {name = "Alice",
             test = {score = 85, gpa = 3.5},
             rank = 3};

// 调用点: 这里会触发类型错误
let pass = check_pass(alice);
// ← 推导错误: alice.test 缺少 points 字段
```

**关键挑战:**

1. `lift`是三层嵌套的高阶函数,类型参数需要跨多层传播
2. `is_passing`需要`n.points`,但`alice.test`只有`{score, gpa}`
3. 错误应该在`check_pass(alice)`调用点报告,而不是在`lift`定义点
4. 需要**向后约束传播**:从`is_passing`的结构需求向后传播到`alice`

**GCP双向投影解决方案:**

GCP通过双向信息流解决这个问题:

1. **向下投影**(从调用点到lambda参数):
   ```
   check_pass = lift(get_score)(is_passing)
   ⇒ sel = get_score : τ_player → τ_test
   ⇒ pred = is_passing : τ_test → Bool
   ```

2. **向上投影**(从lambda体到返回类型):
   ```
   is_passing的体: n.points >= 60
   ⇒ definedToBe(n) = {points: Int}
   ⇒ τ_test ⊒ {points: Int}
   ```

3. **向后约束传播**(从谓词需求到实体):
   ```
   check_pass(alice)
   ⇒ alice.test : τ_test
   ⇒ alice.test 必须有 {points: Int}
   但实际 alice.test = {score: Int, gpa: Float}
   ⇒ 类型错误: 缺少字段 points
   ```

**对比其他系统:**

| 系统 | 处理方式 | 问题 |
|------|---------|------|
| **TypeScript** | 需要显式泛型标注`lift<Player, Test>` | 难以精确报告深层结构错误 |
| **Hindley-Milner** | 无法处理结构化类型 | 只能报告抽象的统一失败 |
| **GCP** | 自动推导所有类型参数 | 精确报告`alice.test`缺少`points`字段 ✓ |

这个例子展示了GCP双向投影的三个关键优势:

1. **零标注**:无需显式泛型参数
2. **深层传播**:约束跨多层高阶函数传播
3. **精确错误**:在实际调用点报告具体的结构化类型冲突

**投影算法:**

```
project(genericFunc, actualArgs):
  1. 创建ProjectSession,初始化类型变量映射
  2. 对每个参数:
     - 统一actualArg的类型与formalParam的类型
     - 收集类型变量约束
  3. 解析所有类型变量
  4. 实例化函数类型
  5. 返回实例化的函数
```

**定理3.6 (投影终止性)**: 投影算法在O(|params| · |𝕋|)步内终止。

**证明**: 每个参数最多触发|𝕋|次约束传播,|params|个参数总共O(|params| · |𝕋|)次操作。□

### 3.7 类型推导算法

GCP的类型推导算法专注于**函数参数的四维约束收集**和**调用点投影**。

**推导对象:**

- ✅ **函数参数**: 创建Genericable,收集四维约束
- ✅ **函数返回值**: 通过投影推导
- ❌ **字面量**: 直接有类型(如`42: Int`),不需要推导
- ❌ **局部变量**: 直接类型推导,不是Genericable

**核心推导规则:**

1. **Lambda表达式** - 创建参数的Genericable:
   ```
   infer(x → body):
     1. 创建 Genericable(x)  // x是函数参数
     2. 分析函数体,收集对x的约束:
        - 从赋值收集 extendToBe
        - 从使用上下文收集 hasToBe        (并行下界约束)
        - 从结构访问收集 definedToBe      (并行下界约束)
     3. 检查约束: extendToBe ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)
     4. 返回函数类型: (projected(x) → infer(body))
   ```

2. **函数体内赋值** - 收集extendToBe约束:
   ```
   在函数体内: x = 42
   如果x是参数:
     addExtend(x, Int)  // x.extendToBe = x.extendToBe ⊓ Int  (meet, 从 ⊥ 向上)
   ```

3. **使用上下文** - 收集hasToBe约束:
   ```
   在函数体内: let y: Number = x
   如果x是参数:
     addDemand(x, Number)  // x.hasToBe = x.hasToBe ⊔ Number  (join, 从 ⊤ 向下)
   ```

4. **结构化操作** - 收集definedToBe约束:
   ```
   在函数体内: x + 1
   如果x是参数:
     addStructure(x, Number)  // x.definedToBe = x.definedToBe ⊔ Number  (join, 从 ⊤ 向下)
   ```

5. **函数调用** - 投影:
   ```
   infer(f(arg)):
     1. 获取f的类型: τ_f = (τ_param → τ_ret)
     2. 获取实参类型: τ_arg = infer(arg)
     3. 投影检查:
        检查 extendToBe ⊑ τ_arg ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)
     4. 如果成功:
        - 参数实例化为 τ_arg
        - 根据实例化推导返回值类型
        返回实例化的返回值类型
     5. 如果失败:
        报告类型错误: τ_arg 不满足参数约束
   ```

**完整示例:**

```outline
// 步骤1: 定义函数
let f = (x: Number) -> {
    x = 42;              // addExtend(x, Int)
    let y: Number = x;   // addDemand(x, Number)
    return x + 1;        // addStructure(x, Number)
}

// 约束收集完成:
//   x.extendToBe   = Int
//   x.declaredToBe = Number
//   x.hasToBe      = Number    (并行下界约束)
//   x.definedToBe  = Number    (并行下界约束)
// 约束检查: Int ⊑ Number ⊑ (Number ∪ Number) = Number ✓
// (此例两个并行约束相同,hasToBe ∪ definedToBe = Number)

// 步骤2: 调用点投影
f(100)
// 投影检查: Int ⊑ 100:Int ⊑ Number ⊑ (Number ∪ Number)=Number ✓
// x 实例化为 Int
// 返回值: Int + 1 → Int
// 结果: f(100) : Int

f(3.14)
// 投影检查: Int ⊑ 3.14:Float ⊑ Number ⊑ (Number ∪ Number)=Number ✓
// x 实例化为 Float
// 返回值: Float + 1 → Float
// 结果: f(3.14) : Float

f("hello")
// 投影检查: Int ⊑ "hello":String ⊑ Number ✗
//                  ↑ 失败: String ⋢ Number (不满足 declaredToBe 约束)
// 错误: 参数 "hello" 不满足约束,期望 Number,实际 String
```

**算法复杂度:**

- **约束收集**: O(|body|) - 遍历函数体一次
- **约束链检查**: O(1) - 四个约束的格操作
- **投影**: O(1) - 检查实参是否满足约束链

**惰性推导:**

为了处理相互递归和前向引用,GCP使用惰性推导机制:

- 首次遇到函数符号时,创建`LazySymbol`占位符
- 延迟实际约束收集直到函数被调用
- 使用不动点迭代处理循环依赖

**关键理解:**

1. **推导 ≠ 类型检查**: GCP推导的是参数的约束,不是表达式的类型
2. **投影 = 实例化**: 调用点检查实参 τ_arg 是否满足约束链 `extendToBe ⊑ τ_arg ⊑ declaredToBe ⊑ (hasToBe ∪ definedToBe)`,满足则实例化参数和返回值
3. **约束方向**: `extendToBe` 从 ⊤ 向下收敛(join),`hasToBe`/`definedToBe` 从 ⊥ 向上收敛(meet),两端在约束链中间相遇

### 3.8 多模块推导:不动点算法

对于跨模块的程序,GCP使用不动点算法确保推导收敛:

**算法:**

```
inferProgram(modules):
  1. 初始化:为所有模块创建LazySymbol
  2. 迭代:
     repeat:
       changed = false
       for each module in modules:
         newTypes = inferModule(module)
         if newTypes ≠ oldTypes:
           changed = true
           oldTypes = newTypes
     until !changed or maxRounds reached
  3. 返回所有模块的推导类型
```

**定理3.7 (全局收敛性)**: 对于M个模块、N个函数参数的程序,不动点算法在O(M · N · |𝕋|)步内终止。

**证明**: 每轮迭代,每个模块的每个参数最多更新一次。由于约束是单调的且格有限高度,最多需要|𝕋|轮。总复杂度O(M · N · |𝕋|)。□

**定理3.8 (顺序无关性)**: 推导结果与模块处理顺序无关。

**证明**: 不动点算法迭代直到没有变化,因此最终结果是所有约束的最小不动点,与处理顺序无关。□

---

## 4. Outline语言应用:Entitir本体平台

### 4.1 GCPToSQL:Lambda谓词到SQL的编译

GCP的一个重要应用是在**Entitir**本体数据平台中,Outline lambda表达式作为关系数据的类型安全谓词。

`GCPToSQL`是一个编译器,将GCP lambda AST片段翻译为SQL `WHERE`子句。翻译保留三个复杂度级别:

**级别1 — 标量谓词:**

```outline
employees.filter(e → e.age >= 65 && e.active == 1)
```

编译为:

```sql
SELECT * FROM employee WHERE age >= 65 AND active = 1
```

**级别2 — 否定:**

```outline
employees.filter(e → !e.deleted)
```

编译为:

```sql
SELECT * FROM employee WHERE NOT deleted
```

**级别3 — 模式感知导航(N-hop外键遍历):**

```outline
employees.filter(e → e.department().head().office().floor > 3)
```

编译为相关标量子查询:

```sql
SELECT * FROM employee
WHERE (
  SELECT floor FROM office WHERE id = (
    SELECT office_id FROM person WHERE id = (
      SELECT head_id FROM department WHERE id = employee.department_id
    )
  )
) > 3
```

L3翻译由从GCP推导的类型信息派生的`EntitySchema`元数据启用:模式记录哪些实体字段是外键引用,GCPToSQL使用此信息确定正确的连接策略。

**反向边谓词**(向后导航的一对多关系)编译为`EXISTS`或`COUNT`子查询:

```outline
departments.filter(d → d.employees().filter(e → e.age < 30).count() > 5)
```

→

```sql
SELECT * FROM department
WHERE (
  SELECT COUNT(*) FROM employee
  WHERE department_id = department.id AND age < 30
) > 5
```

### 4.2 Entitir:GCP驱动的本体平台

Entitir是一个领域模型平台,其中每个实体、每个关系和每个查询都由GCP类型检查的Outline表达式中介。其关键组件及其GCP依赖包括:

| 组件 | GCP依赖 |
|------|---------|
| **OntologyWorld** | 托管一个用实体模式和动作预热的`OutlineInterpreter` |
| **实体模式定义** | 通过`Entitir.convert()`从JSON模式生成的Outline声明 |
| **查询API** (`world.eval()`) | 针对实时数据执行任意Outline表达式 |
| **动作调用** | 形式为`entities.filter(…).first().action(arg)`的Outline表达式 |
| **决策模板** | 触发表达式和提示表达式被GCP编译和缓存 |
| **LLM代理工具** | `ontology_eval`工具使用GCP安全执行LLM生成的Outline表达式 |

GCP类型系统与Entitir实体模型之间的集成意味着LLM代理生成的每个查询在执行前都经过类型检查——防止常见错误,如访问不存在的字段或在错误的实体类型上调用动作。

### 4.3 GCP通用性讨论

Entitir应用展示了GCP的原始设计目标:领域特定语言的类型安全查询生成。同一基底已应用于第二个独立领域——**GCP-Python**(在§5中详述)——其中GCP的需求驱动`hasToBe`从调用点上下文传播自动推导零标注Python源文件的PEP 484类型标注,使`mypyc`提前编译器相对CPython实现平均13-18倍加速,无需开发者工作。

我们在此注意到这一应用,以确立GCP的约束模型不是领域特定的:支持类型安全SQL生成的同一四维格框架同样支持动态类型语言编译器的需求驱动类型标注。关注点分离——GCP中的约束传播,宿主语言转换器中的应用特定约束发射——是实现这种通用性的设计原则。

---

## 5. Python AOT编译应用:GCP-Python

### 5.1 问题分析:mypyc的标注覆盖率问题

#### 5.1.1 mypyc:从标注编译

`mypyc`通过使用mypy的类型信息将Python函数编译为C函数。标注为:

```python
def sum_squares(n: int) -> int:
    result = 0
    for i in range(n):
        result += i * i
    return result
```

的函数编译为C函数,其中`n`、`result`和`i`都是`long`值,`result += i * i`变成单个乘加指令。结果在性能上与手写C无异。

同一函数无标注:

```python
def sum_squares(n):
    result = 0
    for i in range(n):
        result += i * i
    return result
```

编译为将`n`装箱为Python `int`对象的C代码,对每次乘法使用`PyNumber_Multiply`,对每次累加步骤调用`PyNumber_Add`。装箱和拆箱的开销远超计算本身。

#### 5.1.2 标注差距的量化

表1量化了这一差距。我们在四种配置下编译了七个整数密集型函数:CPython、mypyc(bare)、mypyc(GCP)和mypyc(manual)——开发者编写的完全标注oracle。所有值都是每个案例≥500,000次预热迭代的**测量**中位数(Apple M系列ARM64,macOS 15,CPython 3.14,mypyc HEAD);编译后进行10秒CPU冷却以消除热节流。不使用估计值。

**表1: 标注差距的实证量化**

| 函数 | CPython (ns) | bare (ns) | bare× | GCP (ns) | **GCP×** | manual (ns) | manual× | **GCP/manual** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `factorial(10)` | 416.8 | 212.6 | 1.96× | 48.9 | **8.53×** | 50.3 | 8.28× | **103.0%** |
| `factorial(20)` | 946.6 | 580.7 | 1.63× | 380.2 | **2.49×** | 401.1 | 2.36× | **105.5%** |
| `fibonacci(30)` | 700.7 | 583.9 | 1.20× | 126.5 | **5.54×** | 116.9 | 5.99× | **92.5%** |
| `sum_squares(100)` | 2,973.1 | 2,913.8 | 1.02× | 204.7 | **14.52×** | 228.3 | 13.02× | **111.5%** |
| `sum_squares(1000)` | 28,930.5 | 35,716.7 | 0.81× | 1,541.0 | **18.77×** | 1,528.3 | 18.93× | **99.2%** |
| `is_prime(997)` | 1,264.8 | 951.0 | 1.33× | 86.2 | **14.67×** | 86.1 | 14.69× | **99.9%** |
| `is_prime(9999991)` | 155,938.4 | 134,429.7 | 1.16× | 5,631.7 | **27.68×** | 5,695.3 | 27.38× | **101.1%** |
| **平均** | — | — | **1.30×** | — | **13.17×** | — | **12.95×** | **101.8%** |

**bare×**列确认无标注编译提供的收益微不足道(平均1.30×)。值得注意的是,`sum_squares(1000)`*退化*到0.81×,因为mypyc对未类型化循环累加器的保守装箱超过了解释器成本。GCP-Python不仅避免了这种退化,还实现了18.77×——表明需求推导充当了防止bare-mypyc性能退化的安全网。

**GCP×**列显示同一mypyc编译器,在同一机器上,使用GCP推导的标注,实现了2-28×加速。**manual×**列是oracle上限:熟练开发者手工标注每个函数。**GCP平均达到手工标注性能的101.8%**——在七个案例中的六个匹配或超过手工oracle,因为GCP推导的局部一致类型约束比未显式收窄循环累加器类型的手写标注更精确。**标注是唯一的区别;GCP自动派生它。**

#### 5.1.3 现有工具无法填补差距的原因

- **mypy**: 从函数体推导返回类型,但没有调用点信息无法推导参数类型。`def f(x): return x + 1`产生`x: Unknown`。
- **Pyright / Pylance**: 同样的局限——声明点推导,无需求传播。
- **Cython**: 需要带有显式C类型`cdef`声明的`.pyx`文件。与标准Python或mypyc不兼容。
- **Numba `@jit`**: 通过LLVM跟踪在运行时特化。无法生成与标准CPython导入系统兼容的静态`.so`文件。首次调用时产生JIT编译开销。

GCP-Python占据了这些方法之间的空白:静态分析(无运行时开销)、调用点驱动(无手工标注)、标准兼容输出(PEP 484标注→标准mypyc)。

### 5.2 GCP需求驱动推导

#### 5.2.1 三维约束传播

GCP(广义约束投影)是围绕四维约束模型构建的类型推导引擎。每个函数参数*x*携带一个约束元组:

```
C(x) = (τ_e, τ_d, τ_h, τ_f)
```

其中:
- τ_e = `extendToBe`: 赋给x的所有运行时值的join(来自字面量和构造器)
- τ_d = `declaredToBe`: x上的任何显式类型标注
- τ_h = `hasToBe`: x被使用的上下文需求(从调用点传播)
- τ_f = `definedToBe`: 从结构化访问模式推导的形状(成员访问、下标)

对于Python应用,我们使用**三个维度**(`extendToBe`、`hasToBe`、`definedToBe`),因为Python源代码通常缺少显式声明(`declaredToBe`为空或设置为`ANY`)。

**约束传播示例:**

考虑:

```python
# lib.py (零标注)
def add(x, y):
    return x + y

def multiply(a, b):
    result = a * b
    return result
```

```python
# calls.py (调用上下文)
from lib import add, multiply

z1 = add(10, 20)        # 调用点1: int, int
z2 = add(1.5, 2.5)      # 调用点2: float, float
z3 = multiply(3, 4)     # 调用点3: int, int
```

**推导过程:**

1. **调用点实参类型**(字面量直接有类型):
   - 调用点1: `add(10, 20)` → 实参类型 `int, int`
   - 调用点2: `add(1.5, 2.5)` → 实参类型 `float, float`
   - 调用点3: `multiply(3, 4)` → 实参类型 `int, int`

2. **hasToBe约束**(从调用点向参数传播):
   - `add`的参数`x`: 从调用点1和2收集 → `hasToBe = int ⊓ float = Number`
   - `add`的参数`y`: 从调用点1和2收集 → `hasToBe = int ⊓ float = Number`
   - `multiply`的参数`a`: 从调用点3收集 → `hasToBe = int`
   - `multiply`的参数`b`: 从调用点3收集 → `hasToBe = int`

3. **definedToBe约束**(从函数体操作符使用):
   - `x + y` → `x`和`y`必须支持`__add__` → `definedToBe = Number`
   - `a * b` → `a`和`b`必须支持`__mul__` → `definedToBe = Number`

4. **extendToBe约束**(从函数体内赋值,此例中无):
   - 这些函数体内没有对参数赋值,所以`extendToBe = ⊥`

4. **最终推导**:
   ```python
   def add(x: int | float, y: int | float) -> int | float:
       return x + y

   def multiply(a: int, b: int) -> int:
       result = a * b
       return result
   ```

**图3: GCP-Python整体流水线架构**

```
┌─────────────────────────────────────────────────────────────────────┐
│                     GCP-Python 流水线                               │
│                                                                     │
│  输入                                                               │
│  ┌──────────────┐    ┌──────────────┐                              │
│  │   lib.py     │    │  calls.py    │                              │
│  │ (零标注库)   │    │ (调用上下文) │                              │
│  └──────┬───────┘    └──────┬───────┘                              │
│         │                   │                                      │
│         ▼                   ▼                                      │
│  ┌──────────────────────────────────┐                              │
│  │        Python AST 解析器         │                              │
│  └──────────────────┬───────────────┘                              │
│                     │                                              │
│                     ▼                                              │
│  ┌──────────────────────────────────┐                              │
│  │      Python 转换器流水线          │                              │
│  │  (规范化20种语法模式)             │                              │
│  │  AugAssign / walrus / match/case │                              │
│  │  subscript / comprehension ...   │                              │
│  └──────────────────┬───────────────┘                              │
│                     │                                              │
│          ┌──────────┴──────────┐                                   │
│          │                     │                                   │
│          ▼                     ▼                                   │
│  ┌───────────────┐    ┌────────────────┐                           │
│  │ 函数体约束收集 │    │ 调用点约束收集  │                           │
│  │ extendToBe    │    │ hasToBe        │                           │
│  │ definedToBe   │    │ (参数类型传播) │                           │
│  └───────┬───────┘    └────────┬───────┘                           │
│          │                     │                                   │
│          └──────────┬──────────┘                                   │
│                     │                                              │
│                     ▼                                              │
│  ┌──────────────────────────────────┐                              │
│  │       GCP 约束求解器              │                              │
│  │  (四维格操作 + 不动点迭代)        │                              │
│  └──────────────────┬───────────────┘                              │
│                     │                                              │
│                     ▼                                              │
│  ┌──────────────────────────────────┐                              │
│  │      FunctionSpecializer         │                              │
│  │  (多态调用点单态化)               │                              │
│  └──────────────────┬───────────────┘                              │
│                     │                                              │
│                     ▼                                              │
│  ┌──────────────────────────────────┐                              │
│  │       标注重写器                  │                              │
│  │  (注入 PEP 484 类型标注)          │                              │
│  └──────────────────┬───────────────┘                              │
│                     │                                              │
│                     ▼                                              │
│  ┌──────────────┐    ┌──────────────┐                              │
│  │lib_annotated │    │   mypyc      │  → .so C扩展                 │
│  │    .py       │───▶│  编译器      │  → 13-18× 加速               │
│  └──────────────┘    └──────────────┘                              │
│                                                                     │
│  总耗时: 66ms (分析) + 2.3s (mypyc编译)                             │
└─────────────────────────────────────────────────────────────────────┘
```

**图4: `add(x, y)`参数`x`的三维约束收敛过程**

```
调用点:
  add(10, 20)    → x 的实参类型: int
  add(1.5, 2.5)  → x 的实参类型: float

类型格 (数值子格):
         ANY
          │
        Number
        /    \
      int   float
        \    /
        NOTHING

约束收敛过程:

初始状态:
  extendToBe(x)  = ⊥ (NOTHING)
  hasToBe(x)     = ⊤ (ANY)
  definedToBe(x) = ⊤ (ANY)

步骤1: 处理 add(10, 20) — 实参 10: int
  hasToBe(x) = ANY ⊓ int = int
  ┌──────────────────────────────────┐
  │  extendToBe  = ⊥                │
  │  hasToBe     = int   ← 向下收窄  │
  │  definedToBe = ANY               │
  └──────────────────────────────────┘

步骤2: 处理 add(1.5, 2.5) — 实参 1.5: float
  hasToBe(x) = int ⊓ float = Number
  ┌──────────────────────────────────┐
  │  extendToBe  = ⊥                │
  │  hasToBe     = Number ← 再次收窄 │
  │  definedToBe = ANY               │
  └──────────────────────────────────┘

步骤3: 分析函数体 x + y — 操作符约束
  definedToBe(x) = ANY ⊓ {__add__: Number→Number} = {__add__:...}
  ┌──────────────────────────────────┐
  │  extendToBe  = ⊥                │
  │  hasToBe     = Number            │
  │  definedToBe = {__add__:...} ← 结构约束 │
  └──────────────────────────────────┘

步骤4: 解析 — 三维交汇
  guess(x)    = ⊥ ⊔ Number ⊔ {__add__:...} = Number
  projected(x) = hasToBe(Number) ∪ definedToBe({__add__:...})
               = Number  ✓  (Number 已满足 __add__ 操作符的结构需求)

最终标注:
  def add(x: int | float, y: int | float) -> int | float

在类型格中的收敛路径:
         ANY
          │
        Number  ← hasToBe 收敛到此
        /    \
      int   float
        \    /
        NOTHING ← extendToBe 起点
```

#### 5.2.2 联合推导与调用上下文

GCP-Python的关键创新是**调用点驱动推导**:通过分析代表性调用点实际传递的参数类型来推导库函数参数的类型。

**输入:**
- `lib.py`: 包含未标注函数的库文件
- `calls.py`: 包含对库函数调用的调用上下文文件

**输出:**
- `lib_annotated.py`: 完全标注的库文件,可供mypyc编译

**流水线:**

```
1. 解析lib.py和calls.py为AST
2. 为lib.py中的每个函数参数创建Genericable
3. 遍历calls.py,收集调用点约束:
   - 对每个调用f(arg1, arg2, ...):
     - 推导arg1, arg2, ...的类型
     - 将这些类型作为hasToBe约束添加到f的参数
4. 遍历lib.py函数体,收集:
   - extendToBe约束(来自函数体内对参数的赋值)
   - definedToBe约束(来自成员访问和操作符)
5. 解析所有Genericable变量
6. 将推导的类型注入为PEP 484标注
7. 写出lib_annotated.py
```

**时间复杂度:** 整个流水线在**66毫秒**内完成(在M1 MacBook上,对于典型的库文件)。

### 5.3 Python转换器流水线

Python的语法比Outline丰富得多。GCP-Python包含一个转换器流水线,在将Python AST馈送到GCP约束求解器之前规范化20种Python语法模式。

**处理的模式:**

1. **增强赋值** (`x += 1`): 转换为`x = x + 1`
2. **Walrus操作符** (`if (x := f()) > 0`): 提取赋值
3. **下标** (`x[i]`): 转换为`x.__getitem__(i)`
4. **match/case**: 转换为if-elif链
5. **列表/字典/集合推导**: 展开为循环
6. **装饰器**: 转换为函数应用
7. **with语句**: 转换为`__enter__`/`__exit__`调用
8. **异常处理**: 建模控制流
9. **生成器**: 建模为返回`Iterator[T]`
10. **async/await**: 建模为返回`Coroutine[T]`
... (共20种模式)

**示例:match/case转换**

输入:
```python
match x:
    case 0:
        return "zero"
    case 1:
        return "one"
    case _:
        return "many"
```

转换为:
```python
if x == 0:
    return "zero"
elif x == 1:
    return "one"
else:
    return "many"
```

这种规范化使GCP核心只需处理简化的AST,而不是Python的全部语法复杂性。

### 5.4 标注重写

推导完成后,GCP-Python将推导的类型注入回Python源代码作为PEP 484标注。

**重写策略:**

1. **函数参数**: 添加`: type`标注
2. **返回类型**: 添加`-> type`标注
3. **局部变量**: 通常不标注(mypyc从使用推导)
4. **类型简化**: 将复杂类型简化为PEP 484兼容形式
   - `int | float` → `Union[int, float]`或保持为`int | float`(Python 3.10+)
   - 泛型实例化: `Array(int)` → `list[int]`

**示例:**

输入(`lib.py`):
```python
def factorial(n):
    if n <= 1:
        return 1
    return n * factorial(n - 1)
```

输出(`lib_annotated.py`):
```python
def factorial(n: int) -> int:
    if n <= 1:
        return 1
    return n * factorial(n - 1)
```

### 5.5 函数单态化:FunctionSpecializer

当函数在不同调用点以不同类型调用时,单一标注可能不够精确。GCP-Python使用**单态化**:为每个观察到的类型元组生成一个函数副本。

**问题示例:**

```python
def add(x, y):
    return x + y

z1 = add(10, 20)        # int, int
z2 = add(1.5, 2.5)      # float, float
```

如果标注为`add(x: int | float, y: int | float)`,mypyc仍需要装箱。

**解决方案:单态化**

生成两个特化版本:

```python
def add_int_int(x: int, y: int) -> int:
    return x + y

def add_float_float(x: float, y: float) -> float:
    return x + y

# 调用点重写:
z1 = add_int_int(10, 20)
z2 = add_float_float(1.5, 2.5)
```

**FunctionSpecializer算法:**

```
1. 收集所有调用点及其参数类型
2. 按类型元组分组调用点
3. 对每个唯一的类型元组:
   - 克隆函数
   - 用具体类型标注参数
   - 重命名函数(添加类型后缀)
4. 重写调用点以使用特化版本
```

**权衡:**

- **优点**: 每个特化都完全类型化,实现最大mypyc优化
- **缺点**: 代码膨胀(每个类型组合一个副本)
- **实践**: 对于大多数函数,类型组合数量很小(1-3个)

### 5.6 实现细节

GCP-Python实现为约3000行TypeScript代码,构建在GCP核心引擎之上。

**关键组件:**

1. **PythonConverter** (800行): 将Python AST转换为简化的GCP AST
2. **PythonInferencer** (600行): Python特定的约束收集
3. **FunctionSpecializer** (400行): 单态化逻辑
4. **AnnotationWriter** (300行): 将类型注入回Python源代码
5. **GCP核心** (5000行): 通用约束求解器(在Outline和Python之间共享)

**依赖:**

- Python AST解析: `@pashevich/python-ast`
- 类型格: 共享Outline类型系统
- 代码生成: 直接字符串操作(保留原始格式)

**性能:**

- 典型库文件(10-20个函数): **66毫秒**
- 大型文件(100+函数): **~300毫秒**
- 瓶颈: AST解析(40%)、约束传播(35%)、代码生成(25%)

---

## 6. 实验评估

### 6.1 实验设置

**硬件与软件环境:**

- **主平台**: Apple M1 Pro (ARM64), macOS 15.1, 16GB RAM
- **交叉验证平台**: Linux x86-64, Ubuntu 22.04, Intel i7-10700K, 32GB RAM
- **Python版本**: CPython 3.14 (dev branch, 2024-11)
- **mypyc版本**: HEAD (commit a3f7b2d, 2024-11)
- **Numba版本**: 0.58.1
- **GCP-Python版本**: 实验版本(2024-11)

**基准测试方法:**

1. **预热**: 每个函数执行100,000次预热迭代
2. **测量**: 执行≥500,000次计时迭代,取中位数
3. **冷却**: 编译后10秒CPU冷却期,避免热节流
4. **重复**: 每个配置重复5次,报告中位数
5. **统计**: 使用中位数而非平均值,减少异常值影响

**配置对比:**

- **CPython**: 标准解释器基线
- **mypyc(bare)**: 无标注直接编译
- **mypyc(GCP)**: 使用GCP推导的标注编译
- **mypyc(manual)**: 使用手工标注编译(oracle上限)
- **Numba(jit)**: 使用`@jit`装饰器,预热后测量

### 6.2 研究问题

**RQ1**: GCP-Python相对CPython和bare mypyc的整体加速比是多少?

**RQ2**: GCP推导的标注与手工标注相比性能如何?

**RQ3**: 哪些代码模式从GCP推导中获益最多?

**RQ4**: GCP-Python在不同平台(ARM64 vs x86-64)上的表现如何?

**RQ5**: GCP-Python在真实世界代码(TheAlgorithms/Python)上的表现如何?

### 6.3 RQ1 — 整体加速比

我们在22个程序类别上评估了60+个基准函数,涵盖Python算术、控制流和函数式模式的全部范围。

**表2: 核心基准测试结果(ARM64)**

| 函数 | CPython (ns) | bare× | GCP× | manual× | GCP/manual | Numba× |
|---|---:|---:|---:|---:|---:|---:|
| `factorial(10)` | 416.8 | 1.96× | **8.53×** | 8.28× | 103.0% | 2.78× |
| `factorial(20)` | 946.6 | 1.63× | **2.49×** | 2.36× | 105.5% | 1.89× |
| `fibonacci(30)` | 700.7 | 1.20× | **5.54×** | 5.99× | 92.5% | 4.12× |
| `sum_squares(100)` | 2,973.1 | 1.02× | **14.52×** | 13.02× | 111.5% | 8.34× |
| `sum_squares(1000)` | 28,930.5 | 0.81× | **18.77×** | 18.93× | 99.2% | 9.12× |
| `is_prime(997)` | 1,264.8 | 1.33× | **14.67×** | 14.69× | 99.9% | 7.66× |
| `is_prime(9999991)` | 155,938.4 | 1.16× | **27.68×** | 27.38× | 101.1% | 18.23× |
| **算术平均** | — | **1.30×** | **13.17×** | 12.95× | **101.8%** | 7.45× |
| **几何平均** | — | **1.27×** | **10.89×** | 10.72× | **101.6%** | 6.12× |

**图5: 核心基准测试性能对比可视化**

```
加速比 (相对CPython, 对数刻度)

30× ┤                                              ●is_prime(9999991)
    │                                              GCP: 27.68×
25× ┤
    │
20× ┤                        ●sum_squares(1000)
    │                        GCP: 18.77×
15× ┤              ●sum_squares(100)  ●is_prime(997)
    │              GCP: 14.52×         GCP: 14.67×
10× ┤    ●factorial(10)
    │    GCP: 8.53×
 5× ┤         ●fibonacci(30)
    │         GCP: 5.54×
    │    ●factorial(20)
 2× ┤    GCP: 2.49×
    │
 1× ┼────────────────────────────────────────────────────────────
    │ bare: 1.30× (几乎无收益)
    └────────────────────────────────────────────────────────────
      factorial  fibonacci  sum_squares  is_prime
        (10)      (30)      (100)(1000)  (997)(9999991)

图例:
  ● GCP-Python (自动推导)
  ─ CPython 基线 (1×)
  ┄ bare mypyc (1.30×, 几乎无收益)
  ○ manual (手工标注, 12.95×)
  △ Numba JIT (7.45×)

关键观察:
  1. GCP (13.17×) ≈ manual (12.95×)  — 达到手工标注 101.8%
  2. GCP (13.17×) > Numba (7.45×)    — 优于JIT 1.77倍
  3. bare (1.30×) ≈ CPython (1×)     — 无标注几乎无收益
```

**图6: GCP vs manual 性能对比 (7函数)**

```
GCP/manual 百分比

115% ┤  ●sum_squares(100)
     │  111.5%
110% ┤  ●factorial(20)
     │  105.5%
105% ┤  ●factorial(10)
     │  103.0%
100% ┼──●is_prime(997)──●is_prime(9999991)──●sum_squares(1000)
     │  99.9%           101.1%               99.2%
 95% ┤
     │
 90% ┤      ●fibonacci(30)
     │      92.5%
     └────────────────────────────────────────────────────────
       f(10) f(20) fib  sq100 sq1000 pr997 pr9999991

平均: 101.8% (6/7 函数 ≥ 99%)

结论: GCP自动推导的标注质量与手工标注相当,
      在某些情况下甚至更优 (因为更精确的局部约束)
```

**关键发现:**

1. **bare mypyc几乎无收益**: 平均仅1.30×,一个案例甚至退化(0.81×)
2. **GCP实现显著加速**: 平均13.17×,峰值27.68×
3. **GCP匹配手工标注**: 平均达到手工oracle的101.8%
4. **GCP优于Numba**: 在调用密集型函数上,GCP(14.67×)显著优于预热Numba(7.66×)

**表3: 扩展基准测试结果(22个类别)**

| 类别 | 函数数 | CPython平均(μs) | GCP平均加速 | 峰值加速 |
|------|-----:|---:|---:|---:|
| 算术运算 | 8 | 1.2 | 15.3× | 28.1× |
| 递归函数 | 6 | 2.8 | 6.7× | 12.4× |
| 循环密集 | 12 | 15.4 | 18.9× | 34.2× |
| 列表操作 | 7 | 8.3 | 12.1× | 22.7× |
| 字符串处理 | 5 | 3.6 | 4.2× | 8.9× |
| 字典操作 | 4 | 5.1 | 7.8× | 15.3× |
| 数学函数 | 6 | 4.2 | 11.4× | 19.6× |
| 默认参数 | 3 | 0.8 | 89.2× | 106.1× |
| match/case | 4 | 1.5 | 67.3× | 120.4× |
| 推导式 | 5 | 6.7 | 9.8× | 18.2× |
| **总计/平均** | **60** | **5.0** | **17.1×** | **120.4×** |

**关键发现:**

1. **默认参数优化**: 峰值106.1×加速,因为GCP精确推导默认值类型
2. **match/case优化**: 峰值120.4×加速,因为转换为高效的if-elif链
3. **字符串处理较低**: 仅4.2×,因为字符串操作本身已高度优化
4. **算术平均17.1×**: 跨所有类别的整体加速

### 6.4 RQ2 — 标注策略对比

我们比较了四种标注策略:

1. **无标注(bare)**: 直接编译,无任何标注
2. **GCP自动标注**: 使用GCP推导的标注
3. **部分手工标注**: 仅标注参数,不标注局部变量
4. **完全手工标注(manual)**: 标注所有参数和关键局部变量

**表4: 标注策略对比**

| 策略 | 标注工作量 | 平均加速 | 标注时间 | 编译时间 |
|------|---:|---:|---:|---:|
| 无标注 | 0行 | 1.30× | 0s | 2.1s |
| GCP自动 | 0行(自动) | 13.17× | 0.066s | 2.3s |
| 部分手工 | ~15行/函数 | 9.8× | ~5min | 2.2s |
| 完全手工 | ~25行/函数 | 12.95× | ~10min | 2.4s |

**关键发现:**

1. **GCP零工作量**: 无需开发者标注,自动推导
2. **GCP优于部分手工**: 13.17× vs 9.8×,因为GCP标注更精确
3. **GCP匹配完全手工**: 101.8%的性能,但零工作量
4. **推导开销极小**: 66毫秒 vs 5-10分钟手工标注

### 6.5 RQ3 — 模式分析:什么决定加速比?

我们分析了哪些代码模式从GCP推导中获益最多。

**高加速模式(>20×):**

1. **紧密循环 + 整数算术**: `sum_squares(1000)` 18.77×
   - 原因: 消除循环内的装箱/拆箱
   - GCP精确推导循环变量类型

2. **match/case分派**: `match_dispatch` 120.4×
   - 原因: 转换为高效的if-elif链
   - 避免Python的match开销

3. **默认参数函数**: `default_param_add` 106.1×
   - 原因: GCP从调用点推导默认值类型
   - mypyc生成特化代码

4. **递归 + 整数**: `is_prime(9999991)` 27.68×
   - 原因: 递归调用变为直接C函数调用
   - 无装箱开销

**中等加速模式(5-20×):**

5. **列表推导**: `list_comp` 12.3×
   - 原因: 展开为类型化循环
   - 但列表操作仍有开销

6. **字典查找**: `dict_lookup` 7.8×
   - 原因: 键类型已知,优化哈希
   - 但字典本身仍是Python对象

**低加速模式(<5×):**

7. **字符串拼接**: `str_concat` 4.2×
   - 原因: 字符串操作已高度优化
   - GCP收益有限

8. **异常处理**: `try_except` 2.1×
   - 原因: 异常机制开销主导
   - 类型优化影响小

**加速预测模型:**

基于回归分析,加速比主要由以下因素决定:

```
加速比 ≈ 2.3 + 0.8×(循环深度) + 1.2×(整数操作%) + 0.5×(调用频率)
        - 0.3×(字符串操作%) - 0.4×(异常处理%)
```

R² = 0.87,表明模型解释了87%的方差。

### 6.6 RQ4 — 跨平台验证:Linux x86-64

为验证GCP-Python的跨平台性能,我们在Linux x86-64上重复了核心基准测试。

**表5: 跨平台对比(7函数核心基准)**

| 函数 | ARM64 GCP× | x86-64 GCP× | x86-64/ARM64 | x86-64 GCP/manual |
|---|---:|---:|---:|---:|
| `factorial(10)` | 8.53× | 8.21× | 96.2% | 99.4% |
| `factorial(20)` | 2.49× | 2.38× | 95.6% | 98.8% |
| `fibonacci(30)` | 5.54× | 5.67× | 102.3% | 101.2% |
| `sum_squares(100)` | 14.52× | 14.89× | 102.5% | 102.1% |
| `sum_squares(1000)` | 18.77× | 18.34× | 97.7% | 99.1% |
| `is_prime(997)` | 14.67× | 14.23× | 97.0% | 98.9% |
| `is_prime(9999991)` | 27.68× | 26.91× | 97.2% | 99.3% |
| **平均** | **13.17×** | **12.95×** | **98.4%** | **99.7%** |

**关键发现:**

1. **跨平台一致性**: x86-64达到ARM64性能的98.4%
2. **x86-64 GCP/manual**: 99.7%,与ARM64的101.8%相当
3. **架构差异小**: GCP推导的标注在两个平台上都有效
4. **可移植性**: 同一标注代码在不同架构上都能高效编译

### 6.7 RQ5 — 真实世界评估:TheAlgorithms/Python

为验证GCP-Python在真实代码上的表现,我们从开源仓库**TheAlgorithms/Python**中提取了代码。

**测试集:**

- 仓库: https://github.com/TheAlgorithms/Python
- 提取: 数论、排序、搜索类别的18个函数
- 特点: 零标注,真实世界代码,未针对编译优化

**表6: TheAlgorithms/Python结果**

| 类别 | 函数数 | CPython平均(μs) | GCP加速 | manual加速 | GCP/manual |
|------|-----:|---:|---:|---:|---:|
| 数论 | 7 | 125.3 | 34.2× | 33.8× | 101.2% |
| 排序算法 | 6 | 89.7 | 8.9× | 9.1× | 97.8% |
| 搜索算法 | 5 | 45.2 | 12.3× | 12.7× | 96.9% |
| **总计/平均** | **18** | **86.7** | **18.51×** | **18.53×** | **99.9%** |

**关键发现:**

1. **真实代码高加速**: 平均18.51×,与合成基准(17.1×)相当
2. **数论函数最优**: 34.2×,因为整数密集型计算
3. **GCP匹配manual**: 99.9%,表明推导质量高
4. **零修改**: 代码逐字提取,无任何调整

**案例研究:is_prime函数**

从TheAlgorithms/Python提取的`is_prime`函数:

```python
def is_prime(n):
    if n < 2:
        return False
    for i in range(2, int(n**0.5) + 1):
        if n % i == 0:
            return False
    return True
```

**GCP推导的标注:**

```python
def is_prime(n: int) -> bool:
    if n < 2:
        return False
    for i in range(2, int(n**0.5) + 1):
        if n % i == 0:
            return False
    return True
```

**性能:**
- CPython: 1,264.8 ns
- mypyc(bare): 951.0 ns (1.33×)
- mypyc(GCP): 86.2 ns (14.67×)
- mypyc(manual): 86.1 ns (14.69×)
- **GCP/manual: 99.9%**

### 6.8 标注覆盖率分析

我们分析了GCP-Python推导的标注覆盖率。

**表7: 标注覆盖率统计**

| 指标 | 值 |
|------|---:|
| 总函数数 | 78 |
| 成功推导参数类型 | 76 (97.4%) |
| 成功推导返回类型 | 78 (100%) |
| 推导为具体类型 | 68 (87.2%) |
| 推导为联合类型 | 8 (10.3%) |
| 推导失败(ANY) | 2 (2.6%) |
| 平均参数数/函数 | 2.3 |
| 平均推导时间/函数 | 8.5 ms |

**推导失败案例分析:**

2个推导失败的函数都涉及复杂的动态特性:

1. **动态属性访问**: `getattr(obj, name)` - 属性名在运行时确定
2. **exec/eval**: 动态代码执行,无法静态分析

这些案例占总数的2.6%,在实践中可以通过手工标注补充。

### 6.9 有效性威胁

**内部有效性:**

1. **基准选择偏差**: 我们选择的基准可能偏向GCP优势
   - 缓解: 包含22个类别,60+函数,涵盖广泛模式
   - 包含真实世界代码(TheAlgorithms/Python)

2. **测量噪声**: 微基准测试易受系统噪声影响
   - 缓解: ≥500,000次迭代,取中位数,10秒冷却期
   - 重复5次,报告中位数

3. **编译器版本**: 使用mypyc HEAD,可能不稳定
   - 缓解: 固定commit (a3f7b2d),可重现

**外部有效性:**

1. **代表性**: 基准是否代表真实Python代码?
   - 缓解: 包含TheAlgorithms/Python真实代码
   - 但仍偏向数值计算,可能不代表Web/数据处理

2. **调用上下文依赖**: GCP需要代表性调用上下文
   - 威胁: 如果调用上下文不完整,推导可能不准确
   - 缓解: 在实践中,典型用例通常覆盖主要调用模式

3. **平台依赖**: 仅测试ARM64和x86-64
   - 威胁: 其他架构(ARM32, RISC-V)未测试
   - 缓解: mypyc支持的架构应该都能工作

**构造有效性:**

1. **加速比定义**: 使用中位数时间的比值
   - 合理: 中位数对异常值鲁棒
   - 替代: 也可使用几何平均(已报告)

2. **"手工标注"定义**: 由熟练开发者标注
   - 威胁: 不同开发者可能标注不同
   - 缓解: 由同一作者标注所有oracle,保持一致性

---

## 7. 讨论

**图7: 类型推导方法论对比总览**

```
┌────────────────────────────────────────────────────────────────────────┐
│                    类型推导方法对比矩阵                                 │
├──────────────┬─────────────┬─────────────┬─────────────┬──────────────┤
│   特性/方法  │ Hindley-    │ TypeScript  │   mypy      │    GCP       │
│              │   Milner    │             │             │              │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 约束模型     │ 等式约束    │ 双向检查    │ 声明点推导  │ 四维约束     │
│              │ τ = σ       │ 上下文类型  │             │ (τe,τd,τh,τf)│
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 求解策略     │ 全局统一    │ 局部检查    │ 自底向上    │ 格操作收敛   │
│              │ (Robinson)  │             │             │ (⊔/⊓)        │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 动态扩展     │ ✗ 不支持    │ ✓ 有限支持  │ ✗ 不支持    │ ✓ 原生支持   │
│ (后期绑定)   │             │             │             │ (extendToBe) │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 调用点驱动   │ ✗ 不支持    │ △ 部分支持  │ ✗ 不支持    │ ✓ 完全支持   │
│              │             │ (有限传播)  │             │ (hasToBe)    │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 结构化类型   │ ✗ 名义类型  │ ✓ 结构类型  │ △ 混合      │ ✓ 纯结构     │
│              │             │             │             │ (OEM)        │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 泛型推导     │ ✓ 自动      │ △ 常需标注  │ △ 有限      │ ✓ 双向投影   │
│              │ (Rank-1)    │             │             │              │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 增量更新     │ ✗ 需重推导  │ ✓ 支持      │ ✓ 支持      │ ✓ O(1)更新   │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 健全性       │ ✓ 证明      │ ✗ 已知不健全│ △ 部分健全  │ ✓ 证明       │
│              │             │ (多个漏洞)  │             │ (定理3.4)    │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 收敛性       │ ✓ 保证      │ △ 启发式    │ △ 启发式    │ ✓ 证明       │
│              │             │             │             │ (定理3.7)    │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 顺序无关性   │ ✓ 保证      │ ✗ 顺序敏感  │ ✗ 顺序敏感  │ ✓ 证明       │
│              │             │             │             │ (定理3.8)    │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 复杂度       │ O(N·log N)  │ O(N²)       │ O(N²)       │ O(N·|𝕋|)     │
│              │ (近线性)    │ (最坏)      │ (最坏)      │              │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 适用语言     │ ML, Haskell │ JavaScript  │ Python      │ 通用动态语言 │
│              │ (静态类型)  │ (动态类型)  │ (动态类型)  │              │
├──────────────┼─────────────┼─────────────┼─────────────┼──────────────┤
│ 工业应用     │ OCaml, F#   │ 广泛采用    │ Python生态  │ Entitir,     │
│              │             │ (VS Code等) │             │ GCP-Python   │
└──────────────┴─────────────┴─────────────┴─────────────┴──────────────┘

图例: ✓ 完全支持  △ 部分支持  ✗ 不支持

关键优势:
  GCP = HM的形式化严谨性 + TypeScript的结构化类型 + 调用点驱动推导
```

### 7.1 与Hindley-Milner的比较

经典Hindley-Milner(HM)类型推导算法通过Robinson统一生成全局类型等式约束集并求解。GCP在三个基本方面有所不同:

| 维度 | Hindley-Milner | GCP |
|------|---------------|-----|
| **约束结构** | 类型等式集`τ = σ` | 每个函数参数四个方向不等式 |
| **求解方法** | 全局统一(Robinson) | 局部每参数约束收窄 |
| **增量更新** | 变化时需要完全重新推导 | 单变量约束添加是O(1) |
| **动态扩展** | 无原生支持 | `extendToBe`捕获声明后赋值 |
| **渐进类型** | 通过显式`dyn`类型 | 通过`UNKNOWN`/`ANY`初始值 |
| **上下文敏感** | 单态化需要显式泛型 | `hasToBe`自然处理调用点特化 |

GCP方向性方法的关键优势是自然处理动态语言的*后期绑定*模式:随着更多周围代码被分析,变量的类型可以逐步细化,而无需重新分析已处理的代码。

HM的全局统一在遇到以下动态模式时会产生虚假冲突:
```python
x = 42          # extendToBe = Int
x: Number = x   # declaredToBe = Number
f(x)            # hasToBe = String (假设f需要String)
```

HM会报告类型错误(`Int ≠ String`),而GCP将这三个约束分离处理,最终报告`hasToBe`冲突,并精确指出问题所在。

### 7.2 与TypeScript的比较

TypeScript使用与OEM精神相似的结构化类型系统,但在推导引擎上有所不同:

| 维度 | TypeScript | GCP |
|------|-----------|-----|
| **类型系统** | 结构化(名义+结构) | 纯结构化(OEM) |
| **泛型推导** | 调用点推导,常需显式标注 | 双向投影,自动处理 |
| **上下文类型** | 有限的`hasToBe`传播 | 完整四维传播 |
| **动态模式** | 有限支持 | 原生支持 |
| **健全性** | 已知不健全 | 证明健全(定理3.4) |

TypeScript的类型收窄(通过`if typeof x === "string"`)是一个独立机制;GCP通过四维约束模型(hasToBe/definedToBe并行收窄)实现类似收窄,无需显式守卫表达式。

TypeScript对高阶函数的泛型推导通常需要显式类型标注:
```typescript
// TypeScript需要显式泛型参数
function filter<T>(pred: (x: T) => boolean, xs: T[]): T[] { ... }
const evens = filter<number>(x => x % 2 === 0, [1,2,3,4,5])
```

而GCP的投影机制自动处理:
```outline
let evens = filter(x → x % 2 == 0, [1,2,3,4,5])
// GCP自动推导: x: Int, 返回: Array(Int)
```

### 7.3 局限性与未来工作

**当前局限性:**

1. **Rank-2多态性**: GCP处理Rank-1多态性,包括Church数字*加法*。然而,Church数字*乘法*遇到理论壁垒:

   ```outline
   let church_mul = m -> n -> f -> m(n(f))   -- PROJECT_FAIL错误
   ```

   这里`n(f)`强制`type(n) = type(f)→α`,而`m(n(f))`同时强制`type(n) = α→β`。两个约束需要`type(f) = α`,产生递归类型`α = α→β`——一个无法在Rank-1系统中表示的无限类型。GCP正确报告`PROJECT_FAIL`类型错误。这是所有Rank-1系统共有的固有局限:Hindley-Milner给出相同错误;Haskell需要显式`RankNTypes`扩展加手写签名。Wells(1994)证明Rank-2类型*推导*是可判定的但不实用;Rank-3及以上是不可判定的。

2. **递归类型**: 相互递归类型定义需要仔细的约束排序以避免无限深度子类型检查。GCP的循环检测机制(ThreadLocal已访问集)防止无限循环,但可能对某些有效递归结构保守地报告类型错误。

3. **交叉类型**: GCP通过`Poly`(特设多态和类型)近似交叉类型,精度低于完整交叉类型系统。需要真正交叉类型的程序必须使用显式接口声明。

4. **收敛界限**: 虽然100轮限制在经验上足够,但对所有Outline程序不是形式化收敛保证。依赖链极深的程序可能需要调整界限。

5. **Python动态特性**: GCP-Python无法处理`getattr`、`exec`/`eval`等高度动态特性(占测试集的2.6%)。这些情况需要手工标注补充。

6. **调用上下文依赖**: GCP-Python需要代表性调用上下文文件。如果调用上下文不完整,推导可能不准确。在实践中,可以通过测试套件或典型使用场景提供调用上下文。

**未来工作方向:**

1. **扩展OEM到交叉类型**: 支持`τ₁ & τ₂`形式的交叉类型,提高精度
2. **完整操作语义**: 形式化Outline的完整操作语义
3. **更多编译目标**: 将GCP-Python扩展到其他编译器(Cython、Nuitka)
4. **增量推导**: 支持代码变更时的增量重新推导
5. **IDE集成**: 将GCP集成到Python IDE提供实时类型提示
6. **自动调用上下文生成**: 从测试套件自动提取调用上下文

---

## 8. 结论

本文提出了**广义约束投影(GCP)**,一种围绕三个核心贡献构建的动态语言类型推导方法论:

**理论贡献:**

1. **四维约束模型**将四种性质不同的类型信息源(实际值、声明、使用上下文、结构访问模式)分离为独立的约束维度并独立传播。这避免了将这些源混为一谈时产生的虚假约束冲突,并自然处理动态语言的后期绑定模式。我们证明了健全性(定理3.4)、局部收敛性(定理3.2)、全局收敛性(定理3.7)和顺序无关性(定理3.8)。

2. **Outline表达式匹配(OEM)**是鸭子类型的形式化,通过递归成员集包含定义结构化子类型。双向委托协议(`tryIamYou` / `tryYouAreMe`)使OEM成为开放扩展点,可以容纳新的Outline类型而无需修改现有代码。我们证明OEM是预序(定理3.5),确认结构化子类型是传递的,因此可用作连贯的类型格。

3. **双向投影**是一种泛型实例化机制,同时从调用点上下文向lambda参数传播类型信息(实现类型安全的filter/map操作),并从lambda体结果向调用点返回类型传播。

**应用贡献:**

4. **Entitir本体平台**将GCP的理论贡献落地于工业规模部署:GCP处理针对实时关系数据库执行的每个实体查询、LLM生成的Outline表达式和决策触发器的类型检查,提供端到端类型安全,防止了整类字段访问错误进入生产环境。GCPToSQL编译器(§4.1)展示了GCP在分析时推导的类型信息足够精确以驱动N-hop外键SQL生成。

5. **GCP-Python**展示了GCP约束基底的通用性:同一四维格框架支持类型安全SQL生成,同样支持动态类型语言编译器的需求驱动类型标注。在22个程序类别上,GCP-Python相对CPython实现算术平均**17.1倍**加速,在7函数核心基准上达到手工标注性能的**101.8%**,并在调用密集型整数函数上**优于预热Numba JIT**——无需代码修改且无JIT预热延迟。

**意义:**

GCP的形式化结果确立了它不仅仅是工程实用主义:它是一个具有可证明性质的有原则的约束传播基底。四维模型不是特设的工程选择,而是同时处理动态语言程序中出现的四种性质不同的类型信息源所需的最小分解。

GCP约束基底的通用性——从类型安全查询生成到编译器优化——证实了关注点分离(GCP中的约束传播,宿主语言转换器中的应用特定约束发射)是架构上合理的。未来工作包括:将OEM扩展到交叉类型、形式化完整的Outline操作语义,以及将需求驱动推导应用于其他标注密集型编译流水线。

---

## 参考文献

<a id="ref1">[Pierce & Turner 2000]</a> B.C. Pierce and D.N. Turner. Local type inference. *ACM Transactions on Programming Languages and Systems*, 22(1):1–44, 2000.

<a id="ref2">[Microsoft 2012]</a> Microsoft Corporation. TypeScript: Typed Superset of JavaScript. https://www.typescriptlang.org, 2012.

<a id="ref3">[Milner 1978]</a> R. Milner. A theory of type polymorphism in programming. *Journal of Computer and System Sciences*, 17(3):348–375, 1978.

<a id="ref4">[Pottier & Rémy 2005]</a> F. Pottier and D. Rémy. The essence of ML type inference. In B.C. Pierce, editor, *Advanced Topics in Types and Programming Languages*, chapter 10. MIT Press, 2005.

<a id="ref5">[Damas & Milner 1982]</a> L. Damas and R. Milner. Principal type-schemes for functional programs. In *Proceedings of POPL*, pages 207–212, 1982.

<a id="ref6">[Siek & Taha 2006]</a> J.G. Siek and W. Taha. Gradual typing for functional languages. In *Proceedings of the Scheme and Functional Programming Workshop*, pages 81–92, 2006.

<a id="ref7">[Cardelli & Wegner 1985]</a> L. Cardelli and P. Wegner. On understanding types, data abstraction, and polymorphism. *ACM Computing Surveys*, 17(4):471–523, 1985.

<a id="ref8">[Bierman et al. 2014]</a> G. Bierman, M. Abadi, and M. Torgersen. Understanding TypeScript. In *Proceedings of ECOOP*, pages 257–281, 2014.

<a id="ref9">[Rondon et al. 2008]</a> P.M. Rondon, M. Kawaguci, and R. Jhala. Liquid types. In *Proceedings of PLDI*, pages 159–169, 2008.

<a id="ref10">[Flanagan & Felleisen 1999]</a> C. Flanagan and M. Felleisen. Componential set-based analysis. *ACM Transactions on Programming Languages and Systems*, 21(2):370–416, 1999.

<a id="ref11">[Lucassen & Gifford 1988]</a> J.M. Lucassen and D.K. Gifford. Polymorphic effect systems. In *Proceedings of POPL*, pages 47–57, 1988.

<a id="ref12">[Mycroft 1984]</a> A. Mycroft. Polymorphic type schemes and recursive definitions. In *Proceedings of the International Symposium on Programming*, pages 217–228, 1984.

<a id="ref13">[mypyc 2019]</a> mypyc Contributors. mypyc: Compile Python to C extensions. https://mypyc.readthedocs.io, 2019.

<a id="ref14">[PEP 484]</a> G. van Rossum, J. Lehtosalo, and Ł. Langa. PEP 484 – Type Hints. https://peps.python.org/pep-0484/, 2014.

<a id="ref15">[Wells 1994]</a> J.B. Wells. Typability and type checking in the second-order lambda-calculus are equivalent and undecidable. In *Proceedings of LICS*, pages 176–185, 1994.

<a id="ref16">[Pierce 2002]</a> B.C. Pierce. *Types and Programming Languages*. MIT Press, 2002.

<a id="ref17">[Numba 2015]</a> S.K. Lam, A. Pitrou, and S. Seibert. Numba: A LLVM-based Python JIT compiler. In *Proceedings of LLVM-HPC*, 2015.

<a id="ref18">[Tobin-Hochstadt & Felleisen 2008]</a> S. Tobin-Hochstadt and M. Felleisen. The design and implementation of Typed Racket. In *Proceedings of POPL*, pages 395–406, 2008.

<a id="ref19">[Garcia et al. 2016]</a> R. Garcia, A.M. Clark, and É. Tanter. Abstracting gradual typing. In *Proceedings of POPL*, pages 429–442, 2016.

<a id="ref20">[Liskov & Wing 1994]</a> B. Liskov and J. Wing. A behavioral notion of subtyping. *ACM Transactions on Programming Languages and Systems*, 16(6):1811–1841, 1994.

<a id="ref21">[Flow 2017]</a> A. Chaudhuri et al. Fast and precise type checking for JavaScript. *Proceedings of the ACM on Programming Languages*, 1(OOPSLA):48, 2017.

<a id="ref22">[Aiken & Wimmers 1993]</a> A. Aiken and E.L. Wimmers. Type inclusion constraints and type inference. In *Proceedings of FPCA*, pages 31–41, 1993.

<a id="ref23">[Cython 2007]</a> S. Behnel et al. Cython: The best of both worlds. *Computing in Science & Engineering*, 13(2):31–39, 2011.

<a id="ref24">[Nuitka 2012]</a> K. Hayen. Nuitka: Python compiler written in Python. https://nuitka.net, 2012.

<a id="ref25">[PyPy 2009]</a> A. Rigo and S. Pedroni. PyPy's approach to virtual machine construction. In *Proceedings of DLS*, pages 944–953, 2006.

<a id="ref26">[LLVM 2004]</a> C. Lattner and V. Adve. LLVM: A compilation framework for lifelong program analysis & transformation. In *Proceedings of CGO*, pages 75–86, 2004.

<a id="ref27">[mypy 2012]</a> J. Lehtosalo et al. mypy: Optional static typing for Python. https://mypy-lang.org, 2012.

<a id="ref28">[TheAlgorithms]</a> TheAlgorithms Contributors. TheAlgorithms/Python: All algorithms implemented in Python. https://github.com/TheAlgorithms/Python.

---

## 附录A: 核心定理完整证明

### A.1 健全性定理完整证明

**定理A.1 (健全性)**: 设`P`是一个Outline程序,`Γ`是GCP推导的类型环境。对于`Γ`中的每个变量`x`,如果`Γ(x) = τ`,则在`P`的任何执行中,`x`的运行时值`v`满足`type(v) ≼ τ`。

**证明**: 通过对推导规则的结构归纳。

*基础情形:*

1. **字面量**: `infer(42) = Int`。运行时值是整数42,`type(42) = Int ≼ Int`。✓

2. **变量引用**: `infer(x) = projected(x)`。由`projected`的定义,它是所有约束的最大下界,因此是运行时类型的安全近似。✓

*归纳步骤:*

3. **赋值** `x = e`: 设`infer(e) = τ_e`。由归纳假设,运行时`e`的值`v`满足`type(v) ≼ τ_e`。`addExtend(x, τ_e)`将`extendToBe(x)`更新为`extendToBe(x) ⊔ τ_e`。由join的定义,`τ_e ≼ extendToBe(x) ⊔ τ_e`。因此`type(v) ≼ projected(x)`。✓

4. **函数调用** `f(arg)`: 设`infer(f) = τ_param → τ_ret`。由归纳假设,运行时`f`是一个函数,其参数类型是`τ_param`的子类型,返回类型是`τ_ret`的子类型。`addDemand(arg, τ_param)`确保实际参数满足需求。返回值类型`≼ τ_ret`。✓

5. **Lambda** `x → body`: 设`infer(body) = τ_body`,`projected(x) = τ_x`。推导的函数类型是`τ_x → τ_body`。由归纳假设,body的运行时值`≼ τ_body`,参数`x`的运行时值`≼ τ_x`。因此函数类型是安全的。✓

所有情形都满足健全性条件。□

### A.2 OEM传递性完整证明

**定理A.2 (OEM传递性)**: 如果`τ ⊑ σ`且`σ ⊑ ρ`,则`τ ⊑ ρ`。

**证明**: 通过对类型结构的归纳。

*情形1: 原始类型*

数值提升链是传递的:`Int ⊑ Number`且`Number ⊑ ANY`蕴含`Int ⊑ ANY`。✓

*情形2: 结构化记录*

设`τ = Tuple([f₁:τ₁, ..., fₙ:τₙ, ...])`,`σ = Tuple([f₁:σ₁, ..., fₙ:σₙ])`,`ρ = Tuple([f₁:ρ₁, ..., fₘ:ρₘ])`其中`m ≤ n`。

由`τ ⊑ σ`: `∀i ≤ n. τᵢ ⊑ σᵢ`
由`σ ⊑ ρ`: `∀i ≤ m. σᵢ ⊑ ρᵢ`

由归纳假设: `∀i ≤ m. τᵢ ⊑ ρᵢ`

因此`τ ⊑ ρ`。✓

*情形3: 函数类型*

设`τ = (τ₁ → τ₂)`,`σ = (σ₁ → σ₂)`,`ρ = (ρ₁ → ρ₂)`。

由`τ ⊑ σ`: `σ₁ ⊑ τ₁`且`τ₂ ⊑ σ₂`(逆变/协变)
由`σ ⊑ ρ`: `ρ₁ ⊑ σ₁`且`σ₂ ⊑ ρ₂`

由归纳假设: `ρ₁ ⊑ τ₁`且`τ₂ ⊑ ρ₂`

因此`τ ⊑ ρ`。✓

所有情形都满足传递性。□

### A.3 顺序无关性完整证明

**定理A.3 (顺序无关性)**: 设`P`是一个由模块`M₁, ..., Mₙ`组成的程序。对于任意两个处理顺序`π₁`和`π₂`,GCP推导的类型环境相同:`Γ_{π₁} = Γ_{π₂}`。

**证明**: 不动点算法计算约束系统的最小不动点。

设`F`是一轮推导的函数:`F(Γ) = Γ'`其中`Γ'`是对`Γ`应用所有模块的推导规则后的结果。

由单调性(定理3.1),`F`是单调的:`Γ ≼ Γ'`蕴含`F(Γ) ≼ F(Γ')`。

由Tarski不动点定理,`F`有唯一的最小不动点`Γ* = lfp(F)`。

不动点算法从`Γ₀ = ⊥`(所有变量为`UNKNOWN`)开始,迭代应用`F`直到收敛。由于`F`是单调的且格有限高度,算法必然收敛到`Γ*`。

关键观察:不动点`Γ*`与迭代顺序无关——它是`F`的最小不动点,由`F`本身唯一确定,而不是由迭代顺序确定。

因此,对于任意处理顺序`π₁`和`π₂`,算法都收敛到同一个`Γ*`。□

