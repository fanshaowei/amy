-- =====================================================================
-- 预约相关查询配套索引（控制台统计 StatisticMapper + 订单列表 ReservationOrderMapper）
--
-- 背景：原 SQL 在 where 中对时间列套了 date(col) / date_add(curdate(), ...)，
--       对列做函数运算后 MySQL 无法用该列的 B+ 树索引做 range scan。
--       已统一改为裸列 + 左闭右开区间 [start, end)，边界由 Service 层计算传入。
--
-- 索引设计原则：
--   1. 等值条件列（del_flag / reservation_status / project_id）放最左，范围列（时间）放最后；
--   2. 不过度加列，保持索引够小，便于被优化器选中。
--
-- 执行前请先确认现有索引，避免重复创建：
--   SHOW INDEX FROM biz_spas_reservation_order;
--   SHOW INDEX FROM biz_spas_reservation_user;
--   -- MySQL 不支持 ADD INDEX IF NOT EXISTS，已存在同名索引会报错 1061，跳过即可
-- =====================================================================

-- 预约订单表
-- 1) 今日/明日预约人数 + 近30天预约趋势：where del_flag='0' and reservation_time >= ? and < ?
ALTER TABLE biz_spas_reservation_order
    ADD INDEX idx_order_del_res_time (del_flag, reservation_time);

-- 2) 今日核销人数：where del_flag='0' and reservation_status='1' and verify_time >= ? and < ?
ALTER TABLE biz_spas_reservation_order
    ADD INDEX idx_order_del_status_verify (del_flag, reservation_status, verify_time);

-- 4) 订单列表 / 导出「按日期 + 项目」筛选：
--    where del_flag='0' and project_id=? and reservation_time >= ? and < ?
--    （ReservationOrderMapper.xml 原写法 date(reservation_time)=date(?) 已改为裸列半开区间）
--    实测 5400 行样本：带项目筛选时 rows 由 1783 降到 30，无项目筛选时由 5345 降到 89。
ALTER TABLE biz_spas_reservation_order
    ADD INDEX idx_order_del_project_time (del_flag, project_id, reservation_time);

-- 会员表
-- 5) 今日新增会员：where del_flag='0' and create_time >= ? and < ?
ALTER TABLE biz_spas_reservation_user
    ADD INDEX idx_user_del_create_time (del_flag, create_time);

-- 说明：
--   「预约总数」「会员总数」只有 del_flag='0' 一个条件，基数极低，
--   InnoDB 会选择体积最小的二级索引做全索引扫描（index scan）而非全表扫，
--   上面 1)/3) 的最左前缀 del_flag 已覆盖该场景，无需再单独建 (del_flag) 索引。
