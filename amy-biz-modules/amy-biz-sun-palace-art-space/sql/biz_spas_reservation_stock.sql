-- 预约库存表
-- 记录每个项目在每个可预约时段(如 202609280930)的库存数量与状态
DROP TABLE IF EXISTS `biz_spas_reservation_stock`;
CREATE TABLE `biz_spas_reservation_stock`
(
    `reservation_stock_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存id',
    `project_id`           BIGINT       NOT NULL                COMMENT '项目id',
    `reservation_time`     VARCHAR(20)  NOT NULL                COMMENT '预约时间,eg:202609280930',
    `reservation_count`    INT          NOT NULL DEFAULT 0      COMMENT '可预约数量',
    `status`               TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '库存状态(1=有效可预约 0=失效)',
    `del_flag`             CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志（0代表存在 1代表删除）',
    `create_by`            VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    `create_time`          DATETIME                               COMMENT '创建时间',
    `update_by`            VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    `update_time`          DATETIME                               COMMENT '更新时间',
    `remark`               VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`reservation_stock_id`),
    UNIQUE KEY `uk_project_time` (`project_id`, `reservation_time`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_reservation_time` (`reservation_time`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '预约库存表';
