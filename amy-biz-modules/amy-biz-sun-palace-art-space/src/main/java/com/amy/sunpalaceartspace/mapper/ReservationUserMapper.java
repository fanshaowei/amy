package com.amy.sunpalaceartspace.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.vo.ReservationUserVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 预约会员 数据层
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface ReservationUserMapper extends BaseMapper<ReservationUser> {

    /**
     * 查询预约会员列表
     *
     * @param entity 查询条件
     * @return 会员视图列表
     */
    List<ReservationUserVO> selectReservationUserList(@Param("entity") ReservationUser entity);

    /**
     * 根据ID查询预约会员详情
     *
     * @param reservationUserId 会员ID
     * @return 会员视图对象
     */
    ReservationUserVO selectReservationUserById(Long reservationUserId);
}
