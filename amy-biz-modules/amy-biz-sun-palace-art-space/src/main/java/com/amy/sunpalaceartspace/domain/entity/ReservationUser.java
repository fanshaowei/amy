package com.amy.sunpalaceartspace.domain.entity;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约会员对象 biz_spas_reservation_user
 *
 * @Description 管理预约会员/核销员（编号、微信信息、实名信息、状态等）
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_reservation_user")
public class ReservationUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    @TableId(type = IdType.AUTO)
    private Long reservationUserId;

    /** 会员编号（唯一） */
    @Excel(name = "用户编号")
    @NotBlank(message = "会员编号不能为空")
    @Size(min = 0, max = 32, message = "会员编号长度不能超过32个字符")
    @TableField("user_number")
    private String number;

    /** 微信昵称 */
    @Excel(name = "微信昵称")
    @Size(min = 0, max = 100, message = "微信昵称长度不能超过100个字符")
    private String nickname;

    /** 真实姓名 */
    @Excel(name = "姓名")
    @Size(min = 0, max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    /** 手机号 */
    @Excel(name = "电话")
    @NotBlank(message = "手机号不能为空")
    @Size(min = 0, max = 11, message = "手机号长度不能超过11个字符")
    private String phone;

    /** 微信头像地址 */
    @Excel(name = "用户头像")
    @Size(min = 0, max = 255, message = "头像地址长度不能超过255个字符")
    @TableField("head_img_url")
    private String headImgUrl;

    /** 微信 openid */
    @Excel(name = "openid")
    @Size(min = 0, max = 100, message = "openid 长度不能超过100个字符")
    @TableField("open_id")
    private String openId;

    /** 身份证号码 */
    @Excel(name = "身份证号")
    @Size(min = 0, max = 18, message = "身份证号长度不能超过18个字符")
    @TableField("id_num")
    private String idNum;

    /** 微信接口 token（敏感，不导出） */
    @Size(min = 0, max = 500, message = "token 长度不能超过500个字符")
    private String token;

    /** 状态（1正常 2禁用） */
    @Excel(name = "状态", readConverterExp = "1=正常,2=禁用")
    @NotBlank(message = "状态不能为空")
    @Size(min = 0, max = 1, message = "状态长度不能超过1个字符")
    private String status;

    /** 类型（1用户 2核销员） */
    @Excel(name = "类型", readConverterExp = "1=用户,2=核销员")
    @NotNull(message = "类型不能为空")
    private Integer type;

    /** 是否实名认证 */
    @Excel(name = "实名认证", readConverterExp = "true=是,false=否")
    @NotNull(message = "实名认证状态不能为空")
    @TableField("is_verified")
    private Boolean isVerified;
}
