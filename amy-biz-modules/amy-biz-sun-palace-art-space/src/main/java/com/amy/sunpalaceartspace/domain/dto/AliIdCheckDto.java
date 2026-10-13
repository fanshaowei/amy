package com.amy.sunpalaceartspace.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName AliIdCheckDto
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-14 01:08
 */
@NoArgsConstructor
@Data
public class AliIdCheckDto {

    @JsonProperty("msg")
    private String msg;
    @JsonProperty("success")
    private Boolean success;
    @JsonProperty("code")
    private Integer code;
    @JsonProperty("data")
    private DataDTO data;

    @NoArgsConstructor
    @Data
    public static class DataDTO {
        @JsonProperty("birthday")
        private String birthday;
        @JsonProperty("result")
        private Integer result;
        @JsonProperty("address")
        private String address;
        @JsonProperty("orderNo")
        private String orderNo;
        @JsonProperty("sex")
        private String sex;
        @JsonProperty("desc")
        private String desc;
    }
}
