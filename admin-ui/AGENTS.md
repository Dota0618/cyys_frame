<!-- antd-cli setup start -->
## Ant Design CLI 技能

- 修改 Ant Design 代码前读取[Ant Design 技能](.agents/skills/antd/SKILL.md)，路径相对于本文件。

<!-- antd-cli setup end -->

## 前端入口

- 页面和组件遵循[模板规则](../docs/development/frontend/template-reuse.md)。
- API 和身份切换遵循[请求规则](../docs/development/frontend/request-context.md)。
- 表单遵循[表单规则](../docs/development/frontend/form-validation.md)。
- 工具链遵循所在工作区约定，组件查询使用锁定版本的 `npm run antd -- <command> --format json`。
- 不采用技能中的全局安装或自动升级建议。
- 只查询本次涉及的组件；Pro 组件查[模板清单](../docs/migration/ant-design-pro-baseline.md)。
