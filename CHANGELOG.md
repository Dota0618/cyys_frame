# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/lang/zh-CN/spec/v2.0.0.html).

## [Unreleased]

### Changed
- 启动 Phase 4.2：开放受保护的单位部门管理 API 和页面；成员及父部门字段使用真实组织树候选，候选按操作权限返回并在保存时重新校验。
- 完成 Phase 4.1：接入完整官方 Ant Design Pro 6.0.1 工程并实现受保护的单位成员管理；58 项后端测试和 6 组真实浏览器流程通过，Phase 4.2 / G2 待办。
- 对齐 `@rc-component/virtual-list` 与官方模板依赖，修复选择器运行时异常；补正主题设置抽屉关闭把手的层级，单位切换后主题保持可真实操作。
- V4 将本单位姓名和版本保存在成员关系；编辑不影响全局账号或其他单位，角色只替换当前单位授予。
- 前端统一请求、分页和错误处理；按标签页选择单位，清理旧表单及迟到响应；权限撤销后立即停止渲染失权页面并返回工作台，避免同路径路由缓存触发 403 请求循环。
- 完成 Phase 3 部门行级读写隔离和 G1；51 项 MySQL/Redis 测试通过，管理页面仍留到 Phase 4。
- SQL 默认创建时间倒序，部门、单位、菜单按 sort 升序；移除部门通用排序字段映射。
- V3 移除全部数据库外键，增加部门版本、自定义部门范围和成功操作审计；关联关系由文档和业务服务管理。
- 按已批准的 A 方案整理功能内职责目录，补正 Phase 2 的身份解析、权限回调、上下文清理、真实 HTTP 错误状态和审计字段。
- 撤下 G1 前未受保护的管理 CRUD；迁移结构去掉删表和默认密码，Flyway 作为唯一版本入口。
- 数据库测试改用用户确认的现有 MySQL 空库 cyys_frame；增加写入前目标检查，移除整表清空；测试数据不占用正式迁移版本。
- 长期指南归入后端目录，分别为身份与单位上下文、数据库与 SQL、缓存使用；正文仅保留开发条款，其他专题逐项确认。

### Added
- 前端按需规则《请求与单位切换》；真实浏览器验收脚本与 Phase 4.1 验收记录。
- 按操作合并的数据范围、平台显式读写权限、单位角色授予上限、批次回滚、危险 SQL 拒绝及事务上下文固定。
- 按单位隔离的用户名称缓存：命中前授权、批量回源、原子过期、提交后定向刷新；不缓存授权。
- 真实 MySQL 空结构、本地初始化与旧组织样本迁移演练，以及逐项 Phase 3 验收记录。
- 初始化项目脚手架：`backend/` 三模块（`cyys-common` / `cyys-admin` / `cyys-business`）
- 根目录基础文件：`README.md`、`CHANGELOG.md`、`.gitignore`、`.editorconfig`
- JDK 25 + Spring Boot 4.1.1 版本基线
- `migrations/` 迁移脚本目录骨架（`system/`、`business/`、`dev-data/`）
- `deploy/` 部署配置目录骨架（`backend/`、`nginx/`、`docker/`）
- `admin-ui/` 前端工程占位目录骨架（Ant Design Pro）
