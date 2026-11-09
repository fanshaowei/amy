package com.amy.sunpalaceartspace.domain.req.mina;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * @ClassName UpdateReservationUserReq
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-11-09 23:56
 */
@Data
public class UpdateReservationUserReq {
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    @NotBlank(message = "头像不能为空")
    private String avatarImgUrl;
}
