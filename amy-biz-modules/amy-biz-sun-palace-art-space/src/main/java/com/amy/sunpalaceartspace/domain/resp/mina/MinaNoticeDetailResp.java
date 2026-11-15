package com.amy.sunpalaceartspace.domain.resp.mina;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 小程序公告详情（含正文，正文为后台富文本 HTML，小程序用 rich-text 渲染）
 *
 * @author amy
 */
@Builder
@Data
public class MinaNoticeDetailResp {
    /** 公告ID */
    private Long noticeId;

    /** 公告标题 */
    private String noticeTitle;

    /** 公告类型（1通知 2公告） */
    private String noticeType;

    /** 公告内容（HTML 富文本） */
    private String noticeContent;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
