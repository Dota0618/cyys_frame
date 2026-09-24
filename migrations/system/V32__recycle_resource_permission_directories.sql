-- 每个回收站资源使用独立菜单节点承载查看、恢复和彻底删除权限。
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,visible,status,created_by)
SELECT REPLACE(UUID(),'-',''), CONCAT('recycle:resource:', r.code), r.name, p.id, 2, r.sort, 0, 1, 'migration-v32'
FROM sys_recycle_resource r
JOIN sys_menu p ON p.code='nav:menu:recycle-page' AND p.deleted=0
WHERE r.deleted=0
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.code COLLATE utf8mb4_unicode_ci=CONCAT('recycle:resource:', r.code));

UPDATE sys_menu m
JOIN sys_recycle_resource r ON r.deleted=0
JOIN sys_menu p ON p.code COLLATE utf8mb4_unicode_ci=CONCAT('recycle:resource:', r.code) AND p.deleted=0
SET m.parent_id=p.id, m.updated_by='migration-v32', m.updated_at=CURRENT_TIMESTAMP
WHERE m.type=3
  AND m.code COLLATE utf8mb4_unicode_ci LIKE CONCAT('recycle:', r.code, ':%');
