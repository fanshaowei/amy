package com.amy.sunpalaceartspace.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知公告（只读映射后台表 sys_notice）
 *
 * <p>小程序端只读展示，不继承 {@link com.amy.common.core.web.domain.BaseEntity}：
 * sys_notice 表没有 del_flag / params 等列，挂 BaseEntity 会导致 MyBatis-Plus
 * 按 del_flag 生成 SQL 报「unknown column」。注意 notice_content 是 longblob。</p>
 *
 * @author amy
 */
@Data
@TableName("sys_notice")
public class MinaNotice implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公告ID */
    @TableId(value = "notice_id", type = IdType.AUTO)
    private Long noticeId;

    /** 公告标题 */
    private String noticeTitle;

    /** 公告类型（1通知 2公告） */
    private String noticeType;

    /** 公告内容（longblob，需自行按 UTF-8 转 String） */
    private byte[] noticeContent;

    /** 公告状态（0正常 1关闭） */
    private String status;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;
}
