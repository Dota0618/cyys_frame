<!-- antd-cli setup start -->
## Ant Design CLI Skill

Before working on Ant Design code in this repository, read the shared Ant Design skill at [.agents/skills/antd/SKILL.md](.agents/skills/antd/SKILL.md), resolved relative to the directory containing this AGENTS.md (`admin-ui`). The full path is `F:\git\frame\cyys-framework\admin-ui\.agents\skills\antd\SKILL.md`.

The skill teaches agents when and how to call `@ant-design/cli` commands such as `antd info`, `antd doc`, `antd demo`, `antd token`, `antd semantic`, and `antd changelog`.

<!-- antd-cli setup end -->

## 前端入口

- 页面和组件遵循[模板规则](../docs/development/frontend/template-reuse.md)。
- API 和身份切换遵循[请求规则](../docs/development/frontend/request-context.md)。
- 表单遵循[表单规则](../docs/development/frontend/form-validation.md)。
- 使用工作区固定工具链和锁定版本的 `npm run antd -- <command> --format json`。
- 不采用技能中的全局安装或自动升级建议。
- 只查询本次涉及的组件；Pro 组件查[模板清单](../docs/migration/ant-design-pro-baseline.md)。
