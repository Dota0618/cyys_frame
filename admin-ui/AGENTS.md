<!-- antd-cli setup start -->
## Ant Design CLI Skill

Use the shared Ant Design skill at `.agents/skills/antd/SKILL.md` before working on Ant Design code in this repository.

The skill teaches agents when and how to call `@ant-design/cli` commands such as `antd info`, `antd doc`, `antd demo`, `antd token`, `antd semantic`, and `antd changelog`.

<!-- antd-cli setup end -->

## CYYS Frontend

- Read [template reuse](../docs/development/frontend/template-reuse.md) for page and component work, and [request/context](../docs/development/frontend/request-context.md) for API or identity work.
- Use the fixed workspace toolchain and the project CLI: `npm run antd -- <command> --format json`. Keep the locked CLI version; the skill's global install and automatic upgrade suggestions do not apply here.
- Query only the components needed for the task. Pro Components and page templates use the [template inventory](../docs/migration/ant-design-pro-baseline.md).
