package com.amy.sunpalaceartspace.service;

import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderManualReq;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.enums.IdType;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.Date;
import java.util.List;

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
     * 根据订单号查询预约详情（VO，含关联项目名称）
     *
     * @param orderNum 预约单号
     * @return 预约订单视图对象；未命中返回 null
     */
    ReservationOrderVO selectReservationOrderVoByNum(String orderNum);

    /**
     *
     * @param userId
     * @param idNum
     * @param statuses
     * @param since
     * @return
     */
    long countUserOrders(Long userId, String idNum, List<Integer> statuses, Date since);

    /**
     * 新增预约订单
     *
     * @param req 预约订单入参对象
     * @return 是否成功
     */
    ReservationOrder saveReservationOrder(ReservationOrderReq req);

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

    /**
     * 查询预约了<b>次日（明天）</b>当天的号的订单。
     * @return 次日预约订单列表，按预约时间、项目ID升序
     */
    List<ReservationOrderVO> selectNextDayReservationOrder();

    /**
     * 后台手动添加预约订单的入参校验。
     *
     * <p>校验姓名/电话/证件类型/证件号/预约时间，并把证件类型解析成枚举返回，
     * 由调用方把中文名写回请求对象，保证与小程序端入库口径一致。</p>
     *
     * @return 规范化后的证件类型（非 null）
     */
    IdType verifyManualGenerate(ReservationOrderManualReq req);

    boolean updateReservationOrderStatus(Long orderId, ReservationOrderStatus status,String statusMsg);

    /**
     * 后台取消预约订单（仅「待核销」状态可取消）。
     *
     * <p>与小程序端取消口径保持一致：只变更订单状态并记录取消时间，
     * 不回补时段库存（取消释放的名额不参与当天重新放号）。</p>
     *
     * @param reservationOrderId 预约订单ID
     * @return 是否取消成功
     */
    boolean cancelReservationOrder(Long reservationOrderId);

    ReservationOrder manualGenerateOrder(ReservationOrderManualReq req);
}
