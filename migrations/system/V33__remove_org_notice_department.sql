ALTER TABLE sys_org_notice
    DROP KEY idx_notice_org,
    DROP COLUMN org_id;

UPDATE sys_recycle_resource
SET department_column=NULL,
    version=version+1,
    updated_by='migration-v33',
    updated_at=CURRENT_TIMESTAMP
WHERE table_name='sys_org_notice'
  AND department_column='org_id'
  AND deleted=0;
