package com.amy.sunpalaceartspace.service;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticVO;
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
     * @param req 查询条件
     * @return 分页结果
     */
    IPage<ReservationOrderVO> selectReservationOrderList(Page<ReservationOrderVO> page, ReservationOrderReq req);

    /**
     * 查询预约订单导出列表
     *
     * @param req 查询条件
     * @return 视图列表
     */
    List<ReservationOrderVO> selectReservationOrderExportList(ReservationOrderReq req);

    /**
     * 根据ID查询预约订单详情
     *
     * @param reservationOrderId 预约订单ID
     * @return 视图对象
     */
    ReservationOrderVO selectReservationOrderById(Long reservationOrderId);

    /**
     * 根据订单号查询预约详情
     * @param orderNum
     * @return
     */
    ReservationOrder selectReservationOrderByNum(String orderNum);

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

    /**
     * 按时间段统计预约订单数（按 reservationTime 的日期分组）
     *
     * @param req 查询条件：projectId（可选）、reservationTimeStart、reservationTimeEnd
     * @return 各时间段（天）的预约订单数，按时间升序
     */
    List<ReservationOrderStatisticVO> selectReservationOrderStatisticByDate(ReservationOrderStatisticCriteria req);

    /**
     * 按日期+时间段统计预约订单数（按 reservationTime 的日期与时段分组）
     *
     * @param criteria 查询条件：projectId（可选）、reservationTimeStart、reservationTimeEnd
     * @return 各日期+时间段的预约订单数（含日期、时间、数量），按日期、时间升序
     */
    List<ReservationOrderStatisticByTimeVO> selectReservationOrderStatisticByTime(ReservationOrderStatisticCriteria criteria);

    /**
     * 按项目分组统计预约订单总数（按日期时间范围）
     *
     * @param criteria 查询条件：projectId（可选）、reservationTimeStart、reservationTimeEnd
     * @return 各项目的预约订单总数，按项目ID升序
     */
    List<ReservationOrderStatisticByProjectVO> selectReservationOrderStatisticByProject(ReservationOrderStatisticCriteria criteria);
}
