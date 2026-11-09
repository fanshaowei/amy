package com.amy.sunpalaceartspace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约库存状态（对应 {@code biz_spas_reservation_stock.status}）
 *
 * @author fantasyfan
 * @Date 2026-11-01
 */
@Getter
@AllArgsConstructor
public enum ReservationStockStatus {
    EXPIRED(0, "已过可预约时间"),

    /** 1：正常可预约（放号中/已放号，正常写入 Redis 库存缓存，默认值） */
    AVAILABLE(1, "正常可预约"),

    /**
     * 2：该时段被 {@code projectReservationConfig} 手动设置为「不可预约」（休息/维护/包场等）。
     */
    MANUAL_CLOSED(2, "手动设置不可预约");

    private final Integer code;
    private final String description;

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static ReservationStockStatus fromCode(Object value) {
        if (value == null) {
            return null;
        }
        Integer code;
        if (value instanceof Integer) {
            code = (Integer) value;
        } else if (value instanceof Number) {
            code = ((Number) value).intValue();
        } else {
            try {
                code = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的预约库存状态值: " + value);
            }
        }
        for (ReservationStockStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的预约库存状态码: " + code);
    }
}
