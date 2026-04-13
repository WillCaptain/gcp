# GCP 论文分工与去重矩阵

本文档用于明确 `docs/paper/` 下论文文档的职责边界，减少重复维护与重复阅读。

## 论文角色

| 文件 | 角色定位 | 建议读者 | 与其它文档关系 |
|---|---|---|---|
| `gcp-paper.md` | GCP 方法论主论文（英文） | 研究人员、英文投稿场景 | 理论主源；与 `gcp-软件学报.md` 有内容映射 |
| `gcp-python-cgo.md` | Python 零标注 AOT 应用论文（英文） | 编译优化、Python 场景读者 | 应用主源；理论部分引用 `gcp-paper.md` |
| `gcp-软件学报.md` | 中文整合版论文（方法论 + Python 应用） | 中文评审、中文传播 | 中文主源；对英文两篇做聚合与本地化表达 |

## 维护原则（去重）

- 理论定义、定理、复杂度证明以 `gcp-paper.md` 为准。
- Python 应用实验与工程细节以 `gcp-python-cgo.md` 为准。
- 中文长文以“解释与整合”为主，不再重复扩展英文论文中的细节实现差异。
- 当三文出现冲突时，优先级：`gcp-paper.md` > `gcp-python-cgo.md` > `gcp-软件学报.md`。

## 推荐阅读路径

- **只看 GCP 理论**：`gcp-paper.md` → `../../spec/type-system.md` → `../../spec/parallel-constraints.md`
- **只看 Python 编译应用**：`gcp-python-cgo.md` → `../sdk-reference.md`
- **中文完整理解**：`gcp-软件学报.md` → 对照 `gcp-paper.md` 与 `gcp-python-cgo.md`
