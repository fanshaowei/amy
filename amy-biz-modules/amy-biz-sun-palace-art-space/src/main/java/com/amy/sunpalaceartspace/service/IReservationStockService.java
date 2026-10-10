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
}
