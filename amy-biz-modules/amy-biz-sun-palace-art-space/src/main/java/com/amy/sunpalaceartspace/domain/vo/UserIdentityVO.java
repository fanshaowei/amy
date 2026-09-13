package com.amy.sunpalaceartspace.domain.vo;

import java.util.Date;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 用户身份信息视图对象 UserIdentityVO
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Data
public class UserIdentityVO {

    /** 记录ID */
    @Excel(name = "记录ID", cellType = ColumnType.NUMERIC)
    private Long userIdentityId;

    /** 用户姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 证件类型（0身份证 1护照 2港澳台证件 3军官证） */
    @Excel(name = "证件类型", readConverterExp = "0=身份证,1=护照,2=港澳台证件,3=军官证")
    private Integer idType;

    /** 证件号码 */
    @Excel(name = "证件号码")
    private String idNum;

    /** 性别（0女 1男） */
    @Excel(name = "性别", readConverterExp = "0=女,1=男")
    private Integer gender;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phone;

    /** 创建时间 */
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
