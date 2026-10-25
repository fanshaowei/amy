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

    @JsonProperty("error_code")
    private Integer errorCode;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("result")
    private ResultDTO result;
    @JsonProperty("sn")
    private String sn;

    @NoArgsConstructor
    @Data
    public static class ResultDTO {
        @JsonProperty("realname")
        private String realname;
        @JsonProperty("idcard")
        private String idcard;
        @JsonProperty("isok")
        private Boolean isok;
        @JsonProperty("IdCardInfor")
        private IdCardInforDTO idCardInfor;

        @NoArgsConstructor
        @Data
        public static class IdCardInforDTO {
            @JsonProperty("province")
            private String province;
            @JsonProperty("city")
            private String city;
            @JsonProperty("district")
            private String district;
            @JsonProperty("area")
            private String area;
            @JsonProperty("sex")
            private String sex;
            @JsonProperty("birthday")
            private String birthday;
        }
    }
}
