package com.amy.sunpalaceartspace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约订单状态枚举
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Getter
@AllArgsConstructor
public enum ReservationOrderStatus {

    WAIT_VERIFY(0, "待核销"),
    COMPLETED(1, "已完成"),
    EXPIRED(2, "已过期"),
    CANCELED(3, "已取消");

    /** 状态码（数据库实际存储值） */
    private final Integer status;
    /** 中文名（前端展示 / Excel 导出） */
    private final String name;

    /**
     * Jackson 序列化：输出状态码（0/1/2/3），便于前端作为唯一标识
     */
    @JsonValue
    public Integer getStatus() {
        return status;
    }

    /**
     * Jackson 反序列化：按状态码字符串或整数还原枚举
     */
    @JsonCreator
    public static ReservationOrderStatus fromCode(Object value) {
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
                throw new IllegalArgumentException("无效的预约订单状态值: " + value);
            }
        }
        for (ReservationOrderStatus s : values()) {
            if (s.status.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的预约订单状态码: " + code);
    }

    /**
     * 提供按 code 字符串（"0"/"1"/"2"/"3"）还原的服务端辅助方法
     */
    public static ReservationOrderStatus fromCodeString(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return fromCode(code);
    }
}
