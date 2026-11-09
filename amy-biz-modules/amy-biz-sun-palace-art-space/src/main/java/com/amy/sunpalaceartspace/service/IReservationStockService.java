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
     * 查询指定项目、指定时段的库存（一个项目+时段唯一对应一条库存记录；返回 null 表示不存在）
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时段（Date，列存 DATETIME）
     * @return 库存记录
     */
    ReservationStock selectByProjectAndTime(Long projectId, Date reservationTime);

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
     * 预约成功后累加已预约数（用于库存统计：可用 = 已放号 - 已预约）
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时段（Date，列存 DATETIME）
     * @return 是否成功
     */
    boolean incrementReservedCount(Long projectId, Date reservationTime);

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

    /**
     * 根据项目的截止预约时间，将当天之前的库存设置为过期不可预约
     */
    int setReservationStockExpireByProjectCutOffTime();
}
