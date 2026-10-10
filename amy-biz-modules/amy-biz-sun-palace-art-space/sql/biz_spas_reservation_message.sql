drop table if exists biz_spas_reservation_message;

create table biz_spas_reservation_message
(
    reservation_message_id  bigint(20)      not null auto_increment    comment '预约消息ID',
    reservation_id          varchar(64)     default null               comment '预约ID（关联预约单号或预约业务ID）',
    project_id              bigint(20)      default null               comment '项目ID',
    user_id                 bigint(20)      default null               comment '用户ID',
    reservation_time        varchar(32)     default null               comment '预约时间（如 202609281030）',
    status                  int(11)         not null default '1'        comment '预约消息状态（0失败 1待发送 2已发送 3成功，见 ReservationMessageStatus）',
    retry_count             int(11)         not null default '0'        comment '重试次数',
    create_by               varchar(64)     default ''                 comment '创建者',
    create_time             datetime        default null               comment '创建时间',
    update_by               varchar(64)     default ''                 comment '更新者',
    update_time             datetime        default null               comment '更新时间',
    remark                  varchar(500)    default ''                 comment '备注',
    del_flag                char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    primary key (reservation_message_id),
    key idx_user_id (user_id),
    key idx_reservation_id (reservation_id),
    key idx_status (status)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='预约消息发送记录表（outbox）';
