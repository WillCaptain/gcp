# GCP

GCP (Generalized Constraint Projection) 是 Outline 语言的类型推导与解释执行核心，目标是在动态语言场景下同时兼顾表达灵活性与静态类型安全。

## 文档导航

- 用户/SDK/二次开发文档：`docs/index.md`
- 设计与理论文档：`spec/index.md`

## 快速入口

- 5 分钟上手：`docs/quickstart.md`
- SDK 参考：`docs/sdk-reference.md`
- Dot Completion 机制：`docs/dot-completion.md`

## 核心能力

- 四维约束驱动的类型推导（`extendToBe` / `declaredToBe` / `hasToBe` / `definedToBe`）
- 结构化子类型匹配（OEM）
- 多模块固定点推导（ASF）
- 运行时解释执行与插件扩展
