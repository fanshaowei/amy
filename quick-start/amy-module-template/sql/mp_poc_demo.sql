-- MP 集成验证用表（POC 模板）。仅用于验证 MyBatis-Plus 接入，可随时删除。
-- 注意：POC 测试本身会在 @BeforeEach 里执行 CREATE TABLE IF NOT EXISTS，因此本文件主要用于手动建表或重置。
DROP TABLE IF EXISTS mp_poc_demo;
CREATE TABLE mp_poc_demo (
    demo_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '示例ID',
    demo_name   VARCHAR(50)  DEFAULT '' COMMENT '示例名称',
    status      INT          DEFAULT 0 COMMENT '状态（0正常 1停用）',
    del_flag    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
    dept_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '部门ID（用于数据权限验证）',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    remark      VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (demo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MP集成验证表（POC）';
