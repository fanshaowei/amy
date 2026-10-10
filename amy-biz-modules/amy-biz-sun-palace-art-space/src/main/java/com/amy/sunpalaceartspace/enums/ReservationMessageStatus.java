package com.amy.sunpalaceartspace.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName ReservationMessageStatus
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-28 01:22
 */
@AllArgsConstructor
@Getter
public enum ReservationMessageStatus {
    WAITING(1),

    SENT(2),

    SUCCESS(3),

    FAIL(0);

    private final int code;
}
