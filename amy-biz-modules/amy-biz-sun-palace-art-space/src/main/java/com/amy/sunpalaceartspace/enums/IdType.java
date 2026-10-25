package com.amy.sunpalaceartspace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName IdType
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-13 15:54
 */
@AllArgsConstructor
@Getter
public enum IdType {
    ID_CARD(1, "身份证"),
    PASSPORT(2, "护照"),
    HONGKONG_MACAO_PASS(3, "港澳通行证"),
    TAIWAN_PASS(4, "台湾通行证"),
    MILITARY_OFFICER_CARD(5, "军官证");

    //证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证
    private Integer type;
    private String name;

    /**
     * 按编码查找证件类型。
     *
     * @param type 证件类型编码（1~5）
     * @return 对应枚举；编码无法识别或为 null 时返回 null
     */
    public static IdType fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (IdType item : values()) {
            if (item.type.equals(type)) {
                return item;
            }
        }
        return null;
    }

    /**
     * JSON 反序列化入口：按<b>业务编码（1~5）</b>解析，而不是 Jackson 默认的「枚举下标 ordinal」。
     *
     * <p><b>为什么必须有这个方法</b>：枚举字段若不声明 {@code @JsonCreator}，Jackson 会把数字
     * 当成枚举下标处理——{@code 0→ID_CARD}、{@code 1→PASSPORT}……与接口约定的 1~5 整整差一位，
     * 且 {@code 0} 会被静默当成「身份证」<b>不报任何错</b>（0 是合法下标）。
     * 声明后：数字按业务编码解析，非法编码直接抛错；字符串兼容枚举名与中文名。</p>
     *
     * @param value JSON 原值：数字(1~5) / 数字字符串("1") / 枚举名("ID_CARD") / 中文名("身份证")
     * @return 对应枚举
     * @throws IllegalArgumentException 编码无法识别时抛出（Jackson 会包装成 400 返回给前端）
     */
    @JsonCreator
    public static IdType deserialize(Object value) {
        if (value == null) {
            return null;
        }
        String tip = "（可选：1身份证 2护照 3港澳通行证 4台湾通行证 5军官证）";
        if (value instanceof Number number) {
            return require(number.intValue(), String.valueOf(number.intValue()), tip);
        }
        String text = value.toString().trim();
        if (text.matches("\\d+")) {
            return require(Integer.parseInt(text), text, tip);
        }
        for (IdType item : values()) {
            if (item.name().equalsIgnoreCase(text) || item.name.equals(text)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的证件类型：" + text + tip);
    }

    /** 按业务编码取枚举，取不到就抛错（供 {@link #deserialize(Object)} 使用） */
    private static IdType require(Integer code, String raw, String tip) {
        IdType idType = fromType(code);
        if (idType == null) {
            throw new IllegalArgumentException("不支持的证件类型：" + raw + tip);
        }
        return idType;
    }

}
