package com.amy.sunpalaceartspace.enums;

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
    PASSPORT(2, "护照"),
    ID_CARD(1, "身份证"),
    HONGKONG_MACAO_PASS(3, "港澳通行证"),
    TAIWAN_PASS(4, "台湾通行证"),
    MILITARY_OFFICER_CARD(5, "军官证");

    //证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证
    private Integer type;
    private String name;


}
