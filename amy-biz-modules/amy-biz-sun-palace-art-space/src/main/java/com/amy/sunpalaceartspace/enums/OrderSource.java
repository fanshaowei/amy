package com.amy.sunpalaceartspace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约订单来源（对应 {@code biz_spas_reservation_order.order_source}）。
 *
 * @author fantasyfan
 * @Date 2026-11-14
 */
@Getter
@AllArgsConstructor
public enum OrderSource {
    MINA(1, "小程序预约"),
    MANUAL(2, "手动添加");

    private final Integer code;
    private final String name;

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static OrderSource fromCode(Object value) {
        if (value == null) {
            return null;
        }
        Integer code;
        if (value instanceof Number) {
            code = ((Number) value).intValue();
        } else {
            try {
                code = Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        for (OrderSource source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        return null;
    }
}
