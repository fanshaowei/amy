drop table if exists biz_spas_reservation_order;

create table biz_spas_reservation_order
(
    reservation_order_id   bigint(20)      not null auto_increment    comment '预约订单ID',
    reservation_num        varchar(32)     not null                   comment '预约单号（根据预约时间生成，如 202609131210095112）',
    project_id             bigint(20)      not null                   comment '预约项目ID',
    reservation_user_id    bigint(20)      default null               comment '预约会员ID（关联 biz_spas_reservation_user.reservation_user_id，可空表示匿名预约）',
    name                   varchar(50)     not null                   comment '预约人姓名',
    phone                  varchar(11)     not null                   comment '联系电话',
    id_type                varchar(20)     not null                   comment '证件类型（身份证、护照等）',
    id_num                 varchar(50)     not null                   comment '证件号码',
    guests_num             int(11)         default null               comment '预约人数（含预约人本人）',
    reservation_time       datetime        default null               comment '预约时间',
    reservation_status       char(1)         default '0'                comment '状态（0待核销 1已完成 2已过期 3已取消）',
    verify_by              varchar(64)     default ''                 comment '核销人员',
    verify_time            datetime        default null               comment '核销时间',
    comp_users             varchar(2000)   default null               comment '预约随行人身份信息（JSON 数组字符串，每个元素为 UserIdentity 字段序列化结果）',
    create_by              varchar(64)     default ''                 comment '创建者',
    create_time            datetime        default null               comment '创建时间',
    update_by              varchar(64)     default ''                 comment '更新者',
    update_time            datetime        default null               comment '更新时间',
    remark                 varchar(500)    default ''                 comment '备注',
    del_flag               char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    primary key (reservation_order_id),
    unique key uk_reservation_num (reservation_num)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='预约订单表';
