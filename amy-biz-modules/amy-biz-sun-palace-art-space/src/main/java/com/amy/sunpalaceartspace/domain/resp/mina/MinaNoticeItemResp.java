package com.amy.sunpalaceartspace.domain.resp.mina;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 小程序公告列表项（不含正文，避免列表接口拉大字段）
 *
 * @author amy
 */
@Builder
@Data
public class MinaNoticeItemResp {
    /** 公告ID */
    private Long noticeId;

    /** 公告标题 */
    private String noticeTitle;

    /** 公告类型（1通知 2公告） */
    private String noticeType;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
