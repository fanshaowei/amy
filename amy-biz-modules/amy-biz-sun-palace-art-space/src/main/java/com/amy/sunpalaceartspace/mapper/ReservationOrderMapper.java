package com.amy.sunpalaceartspace.mapper;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 预约订单Mapper接口
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface ReservationOrderMapper extends BaseMapper<ReservationOrder> {

    /**
     * 查询预约订单列表（含项目名称）
     *
     * @param reservationOrder 查询条件
     * @return 预约订单视图列表
     */
    List<ReservationOrderVO> selectReservationOrderList(ReservationOrder reservationOrder);

    /**
     * 根据ID查询预约订单（含项目名称）
     *
     * @param reservationOrderId 预约订单ID
     * @return 预约订单视图对象
     */
    ReservationOrderVO selectReservationOrderById(Long reservationOrderId);

    /**
     * 按预约时间（日期）统计各时间段的预约订单数
     *
     * @param req 查询条件：项目ID（可选）、预约时间起止
     * @return 各时间段（天）的预约订单数
     */
    List<ReservationOrderStatisticVO> selectReservationOrderStatisticByDate(ReservationOrderStatisticCriteria req);

    /**
     * 按预约时间（日期+时间段）统计各时间段的预约订单数
     *
     * @param criteria 查询条件：项目ID（可选）、预约时间起止
     * @return 各日期+时间段的预约订单数，按日期、时间升序
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
