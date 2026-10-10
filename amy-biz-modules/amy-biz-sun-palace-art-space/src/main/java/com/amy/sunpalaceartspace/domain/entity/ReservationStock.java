package com.amy.sunpalaceartspace.domain.entity;

import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约库存对象 biz_spas_reservation_stock
 *
 * @Description 每个项目在每个可预约时段(如 202609280930)的库存记录，记录该时段可预约的数量与状态
 * @Author fantasyfan
 * @Date 2026-09-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_reservation_stock")
public class ReservationStock extends BaseEntity {

    /** 库存id */
    @TableId(type = IdType.AUTO)
    private Long reservationStockId;

    /** 项目id */
    private Long projectId;

    /** 预约时间,eg: 202609280930 */
    private String reservationTime;

    /** 可预约数量 */
    private Integer reservationCount;

    /** 已放号数量（分批释放累计，0~reservationCount） */
    private Integer releaseCount;

    /** 已预约数量 */
    private Integer reservedCount;

    /** 批次1是否已放号（12:00 第一批，每时段 +10） */
    private Boolean batch1Released;

    /** 批次2是否已放号（00:00 第二批，补足至 reservationCount） */
    private Boolean batch2Released;

    /** 库存状态（true=有效可预约，false=失效） */
    private Boolean status;
}
