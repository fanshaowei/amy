package com.amy.common.core.enums;

/**
 * @ClassName DataStatus
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-13 15:02
 */
public enum DataStatus {
    OK("0", "正常"),  DELETED("1", "删除");

    private final String code;
    private final String info;

    DataStatus(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }
}
