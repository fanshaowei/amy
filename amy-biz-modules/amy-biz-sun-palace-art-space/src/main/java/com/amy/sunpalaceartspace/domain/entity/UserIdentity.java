package com.amy.sunpalaceartspace.domain.entity;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户身份信息对象 biz_spas_user_identity
 *
 * @Description 记录用户的实名/证件信息（姓名、证件类型、证件号、性别、手机号等）
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_user_identity")
public class UserIdentity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Excel(name = "记录ID", cellType = ColumnType.NUMERIC)
    @TableId(type = IdType.AUTO)
    private Long userIdentityId;

    /** 用户姓名 */
    @Excel(name = "姓名")
    @NotBlank(message = "用户姓名不能为空")
    @Size(min = 0, max = 50, message = "用户姓名长度不能超过50个字符")
    private String name;

    /** 证件类型（建议字典：0身份证 1护照 2港澳台证件 3军官证 等） */
    @Excel(name = "证件类型", readConverterExp = "0=身份证,1=护照,2=港澳台证件,3=军官证")
    @TableField("id_type")
    private Integer idType;

    /** 证件号码 */
    @Excel(name = "证件号码")
    @NotBlank(message = "证件号码不能为空")
    @Size(min = 0, max = 50, message = "证件号码长度不能超过50个字符")
    @TableField("id_num")
    private String idNum;

    /** 性别（0女 1男） */
    @Excel(name = "性别", readConverterExp = "0=女,1=男")
    private Integer gender;

    /** 手机号码 */
    @Excel(name = "手机号码")
    @NotBlank(message = "手机号码不能为空")
    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    private String phone;
}
