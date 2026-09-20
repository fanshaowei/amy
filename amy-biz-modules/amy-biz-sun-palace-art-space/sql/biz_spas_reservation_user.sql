drop table if exists biz_spas_reservation_user;

create table biz_spas_reservation_user
(
    reservation_user_id    bigint(20)      not null auto_increment    comment '会员ID',
    user_number            varchar(32)     not null                   comment '会员编号（唯一）',
    nickname               varchar(100)    default ''                 comment '微信昵称',
    name                   varchar(50)     default ''                 comment '真实姓名',
    phone                  varchar(11)     not null                   comment '手机号',
    avatar_img_url         varchar(255)    default ''                 comment '微信头像地址',
    open_id                varchar(100)    default ''                 comment '微信 openid',
    id_num                  varchar(18)     default ''                 comment '身份证号码',
    token                  varchar(500)    default ''                 comment '微信接口 token',
    status                 char(1)         default '1'                comment '状态（1正常 2禁用）',
    type                   tinyint(1)      default 1                  comment '类型（1用户 2核销员）',
    is_verified            tinyint(1)      default 0                  comment '是否实名认证（0否 1是）',
    create_by              varchar(64)     default ''                 comment '创建者',
    create_time            datetime        default null               comment '创建时间',
    update_by              varchar(64)     default ''                 comment '更新者',
    update_time            datetime        default null               comment '更新时间',
    remark                 varchar(500)    default ''                 comment '备注',
    del_flag               char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    primary key (reservation_user_id),
    unique key uk_user_number (user_number),
    key idx_phone (phone),
    key idx_open_id (open_id)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='预约会员表';
