---
name: cyys-deploy-dev
description: 用户要求日常部署或更新 cyys-framework 本机开发环境时使用。
---

# 开发环境部署

1. 按本次改动选择 `web`（前端）或 `api`（后端），两者都要更新才用 `all`；在项目根运行 `./scripts/deploy-dev.ps1 <目标>`，复用现有环境，不重复检查环境或读取其他开发规则。
2. 根据命令结果报告部署成功或失败；失败时查看本次日志，需要时再读项目内的 `deploy/development.md`。
