-- 历史授权关系保留供审计；废弃入口不再出现在菜单维护和角色勾选树。
UPDATE sys_menu SET deleted=1,status=0,visible=0,version=version+1
WHERE deleted=0 AND (code IN ('platform:scope:access','platform:data:operate','platform:data:read-all')
    OR code LIKE 'platform:role:%' OR path='/system/platform-roles');
