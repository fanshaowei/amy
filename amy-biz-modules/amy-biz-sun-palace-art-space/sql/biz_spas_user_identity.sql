drop table if exists biz_spas_user_identity;

create table biz_spas_user_identity
(
    user_identity_id         bigint(20)      not null auto_increment    comment '记录ID',
    reservation_user_id      bigint(20)      default null               comment '所属会员ID（关联 biz_spas_reservation_user.reservation_user_id，可空表示未绑定会员/随行人员）',
    name                     varchar(50)     not null                   comment '用户姓名',
    id_type                  int(11)         default null               comment '证件类型（0身份证 1护照 2港澳台证件 3军官证）',
    id_num                   varchar(50)     not null                   comment '证件号码',
    gender                   int(1)          default null               comment '性别（0女 1男）',
    phone                    varchar(11)     not null                   comment '手机号码',
    create_by                varchar(64)     default ''                 comment '创建者',
    create_time              datetime        default null               comment '创建时间',
    update_by                varchar(64)     default ''                 comment '更新者',
    update_time              datetime        default null               comment '更新时间',
    remark                   varchar(500)    default ''                 comment '备注',
    del_flag                 char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    primary key (user_identity_id),
    key idx_reservation_user_id (reservation_user_id),
    key idx_phone (phone),
    key idx_id_num (id_num)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='用户身份信息表';

-- 增量脚本：表已存在时执行（与上面的建表语句二选一，不要同时执行）
-- alter table biz_spas_user_identity
--     add column reservation_user_id bigint(20) default null comment '所属会员ID（关联 biz_spas_reservation_user.reservation_user_id，可空表示未绑定会员/随行人员）' after user_identity_id;
-- alter table biz_spas_user_identity add index idx_reservation_user_id (reservation_user_id);
