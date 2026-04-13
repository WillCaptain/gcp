# GCP 术语与符号统一约定

本文件用于统一 `spec/` 文档中的关键术语、符号和约束关系，避免同义不同写或语义冲突。

## 核心术语

| 术语 | 统一含义 | 备注 |
|---|---|---|
| `Genericable` | 未确定类型变量，携带多维约束 | GCP 推导核心抽象 |
| `extendToBe` | 来自赋值/投影值的约束（值侧） | 通常由 `addExtendToBe` 累积 |
| `declaredToBe` | 显式类型声明约束 | 若未声明通常为 `Any` |
| `hasToBe` | 使用上下文需求约束 | 来自参数传递、上下文要求 |
| `definedToBe` | 结构访问需求约束 | 来自成员访问与调用形状 |
| `projectedType` | 元数据辅助字段，不参与推导链 | 仅用于 IDE/LLM 补全等场景 |

## 符号约定

- `<:`：子类型关系（更具体 `<:` 更抽象）。
- `⊓`：meet（交），用于收紧并行约束的共同下界。
- `⊔`：join（并），用于值侧信息合并。
- `Any`：顶类型。
- `Nothing`：底类型。

## 约束关系（统一口径）

### 历史线性链（已废弃）

```text
extendToBe <: declaredToBe <: hasToBe <: definedToBe
```

该写法会错误地把 `hasToBe` 与 `definedToBe` 设为线性依赖，不再作为当前语义。

### 当前并行语义（生效）

```text
extendToBe <: declaredToBe <: (hasToBe ⊓ definedToBe)
```

- `hasToBe` 与 `definedToBe` 是并行下界约束，无先后次序。
- `min()` 计算时应基于两者合并结果，而非二选一。

## 书写规范

- 文档中出现“约束链”默认指当前并行语义。
- 提及历史行为时必须显式标注“已废弃/旧模型”。
- `projectedType` 必须注明“仅元数据用途，不参与推导约束”。
