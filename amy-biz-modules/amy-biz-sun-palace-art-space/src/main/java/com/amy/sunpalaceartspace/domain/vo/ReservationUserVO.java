package com.amy.sunpalaceartspace.domain.vo;

import java.util.Date;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 预约会员视图对象 ReservationUserVO
 *
 * @Description 预约会员列表/导出返回对象，不含敏感 token
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
public class ReservationUserVO {

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long reservationUserId;

    /** 会员编号 */
    @Excel(name = "用户编号")
    private String number;

    /** 微信昵称 */
    @Excel(name = "微信昵称")
    private String nickname;

    /** 真实姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 手机号 */
    @Excel(name = "电话")
    private String phone;

    /** 微信头像地址 */
    @Excel(name = "用户头像")
    private String headImgUrl;

    /** 微信 openid */
    @Excel(name = "openid")
    private String openId;

    /** 身份证号码 */
    @Excel(name = "身份证号")
    private String idNum;

    /** 状态（1正常 2禁用） */
    @Excel(name = "状态", readConverterExp = "1=正常,2=禁用")
    private String status;

    /** 类型（1用户 2核销员） */
    @Excel(name = "类型", readConverterExp = "1=用户,2=核销员")
    private Integer type;

    /** 是否实名认证 */
    @Excel(name = "实名认证", readConverterExp = "true=是,false=否")
    private Boolean isVerified;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "注册时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
