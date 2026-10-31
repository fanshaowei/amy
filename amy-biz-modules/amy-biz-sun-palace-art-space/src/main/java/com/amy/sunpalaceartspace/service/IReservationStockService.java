package com.amy.sunpalaceartspace.service;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
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
     * 新增预约库存（默认状态=有效 true）
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
     * @param reservationTime 预约时段，格式 yyyyMMddHHmm（可空）
     * @param status         库存状态（可空）
     * @return 分页结果
     */
    IPage<ReservationStock> selectReservationStockPage(Page<ReservationStock> page,
            Long projectId, String reservationTime, Boolean status);

    /**
     * 查询指定项目、指定时段的库存（一个项目+时段唯一对应一条库存记录；返回 null 表示不存在）
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时段，格式 yyyyMMddHHmm
     * @return 库存记录
     */
    ReservationStock selectByProjectAndTime(Long projectId, String reservationTime);

    /**
     * 查询指定项目的全部库存（按预约时段升序）
     *
     * @param projectId 项目ID
     * @return 库存列表
     */
    List<ReservationStock> selectByProjectId(Long projectId);

    /**
     * 查询指定项目在指定日期（yyyyMMdd 前缀）的全部时段库存，按预约时段升序。
     *
     * <p>reservationTime 形如 202610310930，按 8 位日期前缀 likeRight 匹配，可命中 idx_reservation_time。</p>
     *
     * @param projectId  项目ID
     * @param datePrefix 日期前缀，形如 20261031
     * @return 库存列表
     */
    List<ReservationStock> selectByProjectAndDatePrefix(Long projectId, String datePrefix);

    /**
     * 查询指定项目「某一天及以后」的全部时段库存，按预约时段升序。
     *
     * <p>reservationTime 形如 202610310930，传 8 位日期前缀即可筛出该日期及之后的所有库存
     * （字符串比较：'20261031' &lt;= '202610310930'）。用于时段配置变更后的库存开关同步。</p>
     *
     * @param projectId  项目ID
     * @param minDayTime 日期下界，形如 20261031（含）
     * @return 库存列表
     */
    List<ReservationStock> selectByProjectIdAndTimeGe(Long projectId, String minDayTime);

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
     * @param reservationTime 预约时段，格式 yyyyMMddHHmm
     * @return 是否成功
     */
    boolean incrementReservedCount(Long projectId, String reservationTime);

    /**
     * 更新库存状态
     *
     * @param reservationStockId 库存ID
     * @param status             目标状态
     * @return 是否成功
     */
    boolean updateStatus(Long reservationStockId, Boolean status);

    /**
     * 批量删除预约库存（软删除）
     *
     * @param reservationStockIds 库存ID集合
     * @return 是否成功
     */
    boolean removeReservationStockByIds(Long[] reservationStockIds);

    List<ReservationStock> selectAllByStatus(Boolean status);
}
