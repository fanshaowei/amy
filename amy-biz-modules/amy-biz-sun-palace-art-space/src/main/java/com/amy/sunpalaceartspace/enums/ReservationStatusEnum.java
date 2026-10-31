package com.amy.sunpalaceartspace.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName ReservationStatusEnum
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-19 21:38
 */
@Getter
@AllArgsConstructor
public enum ReservationStatusEnum {
    AMPLE(1, "充足"),
    TIGHT(2,"紧张"),
    FILLED(3,"已满"),
    /**
     * 后台把该日期该时段配置为「不可预约」（休息/维护/包场等），前端应置灰不允许选择。
     * 注意：与 FILLED(已满) 语义不同 —— 已满只是号被约完，不可预约是压根不开放。
     */
    CLOSED(4,"不可预约");

    private final int value;
    private final String description;
}
