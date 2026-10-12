package com.amy.sunpalaceartspace.domain.req;

import lombok.Data;

/**
 * @ClassName VerifyUserReq
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-13 01:29
 */
@Data
public class VerifyUserReq {
    /** 身份证号 */
    private String idNum;

    /** 证件类型（0身份证 1护照 2港澳台证件 3军官证） */
    private Integer idType;

    /** 用户姓名 */
    private String name;
}
