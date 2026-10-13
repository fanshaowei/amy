package com.amy.sunpalaceartspace.enums;

import lombok.Getter;

import java.util.List;

/**
 * @ClassName GenderEnum
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-14 01:15
 */
@Getter
public enum GenderEnum {
    MALE(0, "男"),
    FEMALE(1, "女");

    private final int code;
    private final String desc;

    GenderEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static int getCodeFromDesc(String desc) {
        return List.of(GenderEnum.values()).stream()
                .filter(genderEnum -> genderEnum.getDesc().equals(desc))
                .findFirst()
                .map(GenderEnum::getCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid gender description: " + desc));
    }
}
