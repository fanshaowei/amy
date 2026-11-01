create table biz_spas_project_reservation_config
(
    config_id           bigint auto_increment comment '配置ID'
        primary key,
    project_id          bigint                  not null comment '项目ID',
    start_time          char(8)                 not null comment '时段开始时间（HH:mm:ss），每天生效',
    end_time            char(8)                 not null comment '时段结束时间（HH:mm:ss，不含，左闭右开）',
    reservation_enabled tinyint(1)  default 1   not null comment '是否可预约（1=可预约 0=不可预约）',
    del_flag            char        default '0' not null comment '删除标志（0代表存在 1代表删除）',
    create_by           varchar(64) default ''  null comment '创建者',
    create_time         datetime                null comment '创建时间',
    update_by           varchar(64) default ''  null comment '更新者',
    update_time         datetime                null comment '更新时间',
    remark              varchar(500)            null comment '备注',
    constraint uk_project_time
        unique (project_id, start_time, end_time, del_flag),
    constraint chk_config_time_range
        check (`start_time` < `end_time`)
)
    comment '项目预约时段配置表';

create table biz_spas_projects
(
    project_id                  bigint auto_increment comment '项目ID'
        primary key,
    project_name                varchar(100)            not null comment '项目名称',
    cover_art_url               varchar(255)            not null comment '封面图URL（200*200）',
    cut_off_time                char(8)                 null comment '每日可预约截止时间（HH:mm:ss），过了该时间只能预约第二天的',
    reservation_stay_second     int                     null comment '预约停留秒数（用户在预约界面可停留的时长）',
    reservation_start_time      char(8)                 null comment '预约开始时间（HH:mm:ss）',
    reservation_end_time        char(8)                 null comment '预约结束时间（HH:mm:ss）',
    reservation_interval_second int                     null comment '预约间隔',
    reservation_count           int                     null comment '预约人数（每个时间段可预约的人数）',
    advance_reservation_days    int                     null comment '可提前预约天数',
    traveler_count              int                     null comment '随行人数',
    reservation_notes           text                    null comment '预约须知（富文本）',
    sort                        int         default 0   null comment '排序',
    status                      char        default '0' null comment '状态（0正常/启用 1停用）',
    create_by                   varchar(64) default ''  null comment '创建者',
    create_time                 datetime                null comment '创建时间',
    update_by                   varchar(64) default ''  null comment '更新者',
    update_time                 datetime                null comment '更新时间',
    remark                      varchar(500)            null comment '备注',
    del_flag                    char        default '0' null comment '删除标志（0代表存在 1代表删除）'
)
    comment '项目管理表';

create index idx_project_name
    on biz_spas_projects (project_name);

create table biz_spas_reservation_message
(
    reservation_message_id bigint auto_increment comment '预约消息ID'
        primary key,
    msg_content            longtext                 null comment '消息内容（MQ 发送载荷，通常为 JSON 字符串）',
    status                 int          default 1   not null comment '预约消息状态（0失败 1待发送 2已发送 3成功，见 ReservationMessageStatus）',
    retry_count            int          default 0   not null comment '重试次数',
    create_by              varchar(64)  default ''  null comment '创建者',
    create_time            datetime                 null comment '创建时间',
    update_by              varchar(64)  default ''  null comment '更新者',
    update_time            datetime                 null comment '更新时间',
    remark                 varchar(500) default ''  null comment '备注',
    del_flag               char         default '0' null comment '删除标志（0代表存在 1代表删除）'
)
    comment '预约消息发送记录表（outbox）' charset = utf8mb4;

create index idx_status
    on biz_spas_reservation_message (status);

create table biz_spas_reservation_order
(
    reservation_order_id bigint auto_increment comment '预约订单ID'
        primary key,
    reservation_num      varchar(32)              not null comment '预约单号（根据预约时间生成，如 202609131210095112）',
    project_id           bigint                   not null comment '预约项目ID',
    reservation_user_id  bigint                   null comment '预约会员ID（关联 biz_spas_reservation_user.reservation_user_id，可空表示匿名预约）',
    name                 varchar(50)              not null comment '预约人姓名',
    phone                varchar(11)              not null comment '联系电话',
    id_type              varchar(20)              not null comment '证件类型（身份证、护照等）',
    id_num               varchar(50)              not null comment '证件号码',
    guests_num           int                      null comment '预约人数（含预约人本人）',
    reservation_time     datetime                 null comment '预约时间',
    reservation_status   char         default '0' null comment '状态（0待核销 1已完成 2已过期 3已取消）',
    verify_by            varchar(64)  default ''  null comment '核销人员',
    verify_time          datetime                 null comment '核销时间',
    comp_users           varchar(2000)            null comment '预约随行人身份信息（JSON 数组字符串，每个元素为 UserIdentity 字段序列化结果）',
    create_by            varchar(64)  default ''  null comment '创建者',
    create_time          datetime                 null comment '创建时间',
    update_by            varchar(64)  default ''  null comment '更新者',
    update_time          datetime                 null comment '更新时间',
    remark               varchar(500) default ''  null comment '备注',
    del_flag             char         default '0' null comment '删除标志（0代表存在 1代表删除）',
    constraint uk_reservation_num
        unique (reservation_num)
)
    comment '预约订单表' charset = utf8mb4;

create table biz_spas_reservation_stock
(
    reservation_stock_id bigint auto_increment comment '库存id'
        primary key,
    project_id           bigint                  not null comment '项目id',
    reservation_time     varchar(20)             not null comment '预约时间,eg:202609280930',
    reservation_count    int         default 0   not null comment '可预约总容量(每时段名额,如20)',
    release_count        int         default 0   not null comment '已放号数量(分批释放累计,0~reservation_count)',
    reserved_count       int         default 0   not null comment '已预约数量',
    batch1_released      tinyint(1)  default 0   not null comment '批次1是否已放号(12:00第一批,每时段+10)',
    batch2_released      tinyint(1)  default 0   not null comment '批次2是否已放号(00:00第二批,补足至满)',
    status               tinyint(1)  default 1   not null comment '库存状态(1=有效可预约 0=失效)',
    del_flag             char        default '0' not null comment '删除标志（0代表存在 1代表删除）',
    create_by            varchar(64) default ''  null comment '创建者',
    create_time          datetime                null comment '创建时间',
    update_by            varchar(64) default ''  null comment '更新者',
    update_time          datetime                null comment '更新时间',
    remark               varchar(500)            null comment '备注',
    constraint uk_project_time
        unique (project_id, reservation_time)
)
    comment '预约库存表';

create index idx_project_id
    on biz_spas_reservation_stock (project_id);

create index idx_reservation_time
    on biz_spas_reservation_stock (reservation_time);

create index idx_status
    on biz_spas_reservation_stock (status);

create table biz_spas_reservation_user
(
    reservation_user_id bigint auto_increment comment '会员ID'
        primary key,
    nickname            varchar(255) default ''  null comment '微信昵称',
    name                varchar(50)  default ''  null comment '真实姓名',
    phone               varchar(11)  default ''  null comment '手机号',
    avatar_img_url      varchar(255)             null comment '微信头像地址',
    open_id             varchar(100) default ''  null comment '微信 openid',
    id_num              varchar(18)  default ''  null comment '身份证号码',
    token               varchar(500) default ''  null comment '微信接口 token',
    status              varchar(2)   default '1' null comment '状态（0停用 1正常 -1删除）',
    type                tinyint(1)   default 1   null comment '类型（1用户 2核销员）',
    is_verified         tinyint(1)   default 0   null comment '是否实名认证（0否 1是）',
    create_by           varchar(64)  default ''  null comment '创建者',
    create_time         datetime                 null comment '创建时间',
    update_by           varchar(64)  default ''  null comment '更新者',
    update_time         datetime                 null comment '更新时间',
    remark              varchar(500) default ''  null comment '备注',
    del_flag            char         default '0' null comment '删除标志（0代表存在 1代表删除）',
    constraint uk_open_id
        unique (open_id)
)
    comment '预约会员表' charset = utf8mb4;

create index idx_phone
    on biz_spas_reservation_user (phone);

create table biz_spas_user_identity
(
    user_identity_id    bigint auto_increment comment '记录ID'
        primary key,
    reservation_user_id bigint                   null comment '所属会员ID（关联 biz_spas_reservation_user.reservation_user_id，可空表示未绑定会员/随行人员）',
    name                varchar(50)              not null comment '用户姓名',
    id_type             int                      null comment '证件类型（0身份证 1护照 2港澳台证件 3军官证）',
    id_num              varchar(50)              not null comment '证件号码',
    gender              int                      null comment '性别（0女 1男）',
    phone               varchar(11)              not null comment '手机号码',
    is_follower         tinyint(1)   default 0   not null comment '是否跟随者（随行人）：0=否 1=是',
    create_by           varchar(64)  default ''  null comment '创建者',
    create_time         datetime                 null comment '创建时间',
    update_by           varchar(64)  default ''  null comment '更新者',
    update_time         datetime                 null comment '更新时间',
    remark              varchar(500) default ''  null comment '备注',
    del_flag            char         default '0' null comment '删除标志（0代表存在 1代表删除）',
    constraint uk_id_num
        unique (id_num)
)
    comment '用户身份信息表' charset = utf8mb4;

create index idx_phone
    on biz_spas_user_identity (phone);

create index idx_reservation_user_id
    on biz_spas_user_identity (reservation_user_id);

