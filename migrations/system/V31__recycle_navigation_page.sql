-- 数据回收站目录下提供独立的恢复/彻底删除页面，配置页作为同级子菜单。
UPDATE sys_menu
SET parent_id='0', type=1, path=NULL, updated_by='migration-v31', updated_at=CURRENT_TIMESTAMP
WHERE code='nav:menu:data-recycle' AND deleted=0;

INSERT INTO sys_menu(id,code,name,parent_id,type,path,icon,sort,created_by)
SELECT 'nav-menu-recycle-page','nav:menu:recycle-page','回收站',
       (SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle' AND deleted=0),
       2,'/system/recycle','DeleteOutlined',1,'migration-v31'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='nav:menu:recycle-page');

UPDATE sys_menu m
JOIN sys_menu old ON old.code='nav:menu:data-recycle' AND old.deleted=0
JOIN sys_menu p ON p.code='nav:menu:recycle-page' AND p.deleted=0
SET m.parent_id=p.id, m.updated_by='migration-v31', m.updated_at=CURRENT_TIMESTAMP
WHERE m.parent_id=old.id
  AND m.type=3 AND m.code LIKE 'recycle:%' AND m.code NOT LIKE 'recycle:config:%';
