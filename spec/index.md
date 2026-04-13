# GCP 设计索引（架构 / 理论 / 研究）

本目录聚焦 GCP 的设计原理、约束模型与研究论文。

## 架构与机制

- `overview.md`：系统定位、模块地图、核心依赖关系。
- `type-system.md`：Outline 类型系统与约束传播细节。
- `parallel-constraints.md`：`hasToBe` / `definedToBe` 并行约束语义与修正。
- `error.md`：错误模型与诊断体系。

## 研究论文

- `papers.md`：迁移提示（论文已迁移至 `docs/paper/`）。
- `../docs/paper/papers.md`：论文分工、差异矩阵与推荐阅读路径。
- `../docs/paper/gcp-paper.md`：GCP 方法论英文主论文（理论主源）。
- `../docs/paper/gcp-python-cgo.md`：GCP 在 Python 零标注 AOT 场景的英文应用论文（应用主源）。
- `../docs/paper/gcp-软件学报.md`：中文整合版（方法论 + 应用）。

## 术语与符号

- `terminology.md`：跨文档统一术语、符号与约束关系约定。

## 图与附加说明

- `图表说明.md`：论文图表索引与说明。
- `architecture.puml` / `inference-flow.puml` / `type-system.puml`：结构与流程图源文件。
