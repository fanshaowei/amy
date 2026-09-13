package com.amy.sunpalaceartspace.service;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 预约订单 服务层
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface IReservationOrderService extends IService<ReservationOrder> {

    /**
     * 查询预约订单列表（分页）
     *
     * @param page            分页参数
     * @param reservationOrder 查询条件
     * @return 分页结果
     */
    IPage<ReservationOrderVO> selectReservationOrderList(Page<ReservationOrderVO> page, ReservationOrder reservationOrder);

    /**
     * 查询预约订单导出列表
     *
     * @param reservationOrder 查询条件
     * @return 视图列表
     */
    List<ReservationOrderVO> selectReservationOrderExportList(ReservationOrder reservationOrder);

    /**
     * 根据ID查询预约订单详情
     *
     * @param reservationOrderId 预约订单ID
     * @return 视图对象
     */
    ReservationOrderVO selectReservationOrderById(Long reservationOrderId);

    /**
     * 新增预约订单
     *
     * @param req 预约订单入参对象
     * @return 是否成功
     */
    boolean saveReservationOrder(ReservationOrderReq req);

    /**
     * 修改预约订单
     *
     * @param req 预约订单入参对象
     * @return 是否成功
     */
    boolean updateReservationOrder(ReservationOrderReq req);

    /**
     * 批量删除预约订单（软删除）
     *
     * @param reservationOrderIds 预约订单ID集合
     * @return 是否成功
     */
    boolean removeReservationOrderByIds(Long[] reservationOrderIds);
}
