package com.amy.sunpalaceartspace.domain.entity;

import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 预约库存对象 biz_spas_reservation_stock
 *
 * @Description 每个项目在每个可预约时段(如 2026-09-28 09:30:00)的库存记录，记录该时段可预约的数量与状态
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

    /** 预约时间,eg: 2026-09-28 09:30:00 */
    private Date reservationTime;

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

    /**
     * 库存状态，取值见 {@link com.amy.sunpalaceartspace.enums.ReservationStockStatus}：
     * 0=已过可预约时间、1=正常可预约、2=手动设置该时段不可预约。
     *
     * <p>列存数值码（TINYINT），赋值/比较统一走枚举的 {@code getCode()}，不要直接写布尔或字面量。</p>
     */
    private Integer status;

    /** 售罄时间（已预约数达到总容量 reservation_count 的时刻；仅首次达满时记录） */
    private Date soldOutTime;
}
