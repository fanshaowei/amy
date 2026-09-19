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
    FILLED(3,"已满");

    private final int value;
    private final String description;
}
