-- 与现有身份/部门资源的连接键保持一致；V9 已运行，使用前向迁移修正。
ALTER TABLE sys_file_entry CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_file_part CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
