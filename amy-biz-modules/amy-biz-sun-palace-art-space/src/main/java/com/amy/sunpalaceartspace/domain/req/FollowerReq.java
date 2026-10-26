package com.amy.sunpalaceartspace.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName FollowerReq
 * @Description 随从人
 * @Author fantasyfan
 * @Date 2026-10-24 22:02
 */
@Data
public class FollowerReq {
    @NotBlank(message = "随从人姓名不能为空")
    private String name;

    @NotBlank(message = "随从人身份证号不能为空")
    private String idNum;

    @NotBlank(message = "随从人电话不能为空")
    private String phone;
}
