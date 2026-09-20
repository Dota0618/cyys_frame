-- 平台统一维护、各单位共享；系统通知不记录阅读状态或回执。
CREATE TABLE sys_notice (
    id varchar(32) NOT NULL PRIMARY KEY,
    title varchar(100) NOT NULL,
    type varchar(20) NOT NULL,
    type_label varchar(20) NOT NULL,
    content varchar(2000) NOT NULL,
    is_show boolean NOT NULL DEFAULT FALSE,
    version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32) NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32) NOT NULL,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notice_list (deleted,created_at,id),
    CONSTRAINT ck_notice_show CHECK (is_show IN (0,1)),
    CONSTRAINT ck_notice_deleted CHECK (deleted IN (0,1))
);
