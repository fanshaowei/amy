package com.amy.sunpalaceartspace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目状态（对应 {@code biz_spas_projects.status}，char(1)）
 *
 * <ul>
 *   <li>0 = 停用</li>
 *   <li>1 = 正常（启用，对用户可见、可放号）</li>
 * </ul>
 *
 * @author fantasyfan
 * @Date 2026-11-10
 */
@Getter
@AllArgsConstructor
public enum ProjectStatus {
    /** 0：停用（对用户不可见、不参与放号） */
    DISABLED("0", "停用"),

    /** 1：正常（启用，对用户可见、可放号） */
    NORMAL("1", "正常");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ProjectStatus fromCode(Object value) {
        if (value == null) {
            return null;
        }
        String code = value instanceof String ? (String) value : value.toString();
        for (ProjectStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的项目状态码: " + code);
    }
}
