package com.amy.sunpalaceartspace.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName ReservationUserStatusEnum
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-10 12:33
 */
@AllArgsConstructor
@Getter
public enum ReservationUserStatusEnum {
    INACTIVE(0, "停用"),
    ACTIVE(1, "正常"),
    DELETED(-1, "删除");

    private final Integer code;
    private final String description;
}
