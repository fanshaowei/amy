package com.amy.sunpalaceartspace.mapper;

import java.util.Date;

import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 预约库存Mapper接口
 *
 * @author fantasyfan
 * @date 2026-09-28
 */
public interface ReservationStockMapper extends BaseMapper<ReservationStock> {

    /**
     * 预约成功后累加已预约数；当已预约数达到总容量时记录售罄时间（仅首次达满写入）。
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时段（Date，列存 DATETIME）
     * @param updateTime      更新时间
     * @return 受影响行数
     */
    int incrementReservedCount(@Param("projectId") Long projectId,
                               @Param("reservationTime") Date reservationTime,
                               @Param("updateTime") Date updateTime);
}
