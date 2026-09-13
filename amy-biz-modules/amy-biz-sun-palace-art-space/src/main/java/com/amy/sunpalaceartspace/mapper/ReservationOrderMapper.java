package com.amy.sunpalaceartspace.mapper;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
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
}
