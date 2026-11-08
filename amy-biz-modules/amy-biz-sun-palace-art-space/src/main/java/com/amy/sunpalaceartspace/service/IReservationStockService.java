package com.amy.sunpalaceartspace.service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.amy.sunpalaceartspace.enums.ReservationStockStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 预约库存 服务层
 *
 * @author fantasyfan
 * @date 2026-09-28
 */
public interface IReservationStockService extends IService<ReservationStock> {

    /**
     * 新增预约库存（默认状态=正常可预约 1）
     *
     * @param stock 预约库存
     * @return 是否成功
     */
    boolean saveReservationStock(ReservationStock stock);

    /**
     * 分页查询预约库存（可按项目、时段、状态过滤；入参为空表示不限制）
     *
     * @param page           分页参数
     * @param projectId      项目ID（可空）
     * @param reservationTime 预约时段（可空，精确到秒；列存 DATETIME）
     * @param status         库存状态枚举（可空，为空表示不限制）
     * @return 分页结果
     */
    IPage<ReservationStock> selectReservationStockPage(Page<ReservationStock> page,
            Long projectId, Date reservationTime, ReservationStockStatus status);

    /**
     * 查询指定项目、指定时段的库存（一个项目+时段唯一对应一条库存记录；返回 null 表示不存在）
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时段（Date，列存 DATETIME）
     * @return 库存记录
     */
    ReservationStock selectByProjectAndTime(Long projectId, Date reservationTime);

    /**
     * 查询指定项目的全部库存（按预约时段升序）
     *
     * @param projectId 项目ID
     * @return 库存列表
     */
    List<ReservationStock> selectByProjectId(Long projectId);

    /**
     * 查询指定项目在指定日期的全部时段库存，按预约时段升序。
     *
     * <p>reservationTime 按「当天 00:00:00 &lt;= t &lt; 次日 00:00:00」范围查询。</p>
     *
     * @param projectId  项目ID
     * @param datePrefix 日期（按天）
     * @return 库存列表
     */
    List<ReservationStock> selectByProjectAndDatePrefix(Long projectId, LocalDate datePrefix);

    /**
     * 查询指定项目「某一天及以后」的全部时段库存，按预约时段升序。
     *
     * <p>reservationTime DATETIME，传一个 Date 下界（含）即可筛出该时刻及之后的所有库存。</p>
     *
     * @param projectId 项目ID
     * @param minTime   时间下界（含）
     * @return 库存列表
     */
    List<ReservationStock> selectByProjectIdAndTimeGe(Long projectId, Date minTime);

    /**
     * 扣减库存（原子操作：reservation_count = reservation_count - delta，用于用户预约占名额）
     *
     * @param reservationStockId 库存ID
     * @param delta              扣减数量（正数）
     * @return 是否成功
     */
    boolean deductReservationCount(Long reservationStockId, int delta);

    /**
     * 回补库存（原子操作：reservation_count = reservation_count + delta，用于取消/释放预约名额）
     *
     * @param reservationStockId 库存ID
     * @param delta              回补数量（正数）
     * @return 是否成功
     */
    boolean incrementReservationCount(Long reservationStockId, int delta);

    /**
     * 预约成功后累加已预约数（用于库存统计：可用 = 已放号 - 已预约）
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时段（Date，列存 DATETIME）
     * @return 是否成功
     */
    boolean incrementReservedCount(Long projectId, Date reservationTime);

    /**
     * 更新库存状态
     *
     * @param reservationStockId 库存ID
     * @param status             目标状态枚举
     * @return 是否成功
     */
    boolean updateStatus(Long reservationStockId, ReservationStockStatus status);

    /**
     * 批量删除预约库存（软删除）
     *
     * @param reservationStockIds 库存ID集合
     * @return 是否成功
     */
    boolean removeReservationStockByIds(Long[] reservationStockIds);

    /**
     * 查询指定状态的全部库存（用于只把「正常可预约」的时段重建到 Redis 缓存）
     *
     * @param status 状态枚举（非空）
     * @return 库存列表
     */
    List<ReservationStock> selectAllByStatus(ReservationStockStatus status);

    /**
     * 查询「除指定状态以外」的全部库存（用于清除过期/手动关闭时段可能残留的 Redis 缓存 key）
     *
     * @param excludeStatus 需要排除的状态枚举（非空）
     * @return 库存列表
     */
    List<ReservationStock> selectAllExcludeStatus(ReservationStockStatus excludeStatus);
}
