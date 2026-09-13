package com.amy.sunpalaceartspace.domain;

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
 * 项目管理对象 biz_spas_projects
 *
 * @Description 管理每个项目的预约信息（项目名称、封面、预约时间规则、预约须知等）
 * @Author fantasyfan
 * @Date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_projects")
public class Projects extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    @Excel(name = "项目ID", cellType = ColumnType.NUMERIC)
    @TableId(type = IdType.AUTO)
    private Long projectId;

    /** 项目名称 */
    @Excel(name = "项目名称")
    @NotBlank(message = "项目名称不能为空")
    @Size(min = 0, max = 100, message = "项目名称长度不能超过100个字符")
    private String projectName;

    /** 封面图URL（200*200） */
    @Excel(name = "封面图URL")
    @NotBlank(message = "封面图不能为空")
    @Size(min = 0, max = 255, message = "封面图URL长度不能超过255个字符")
    private String coverArtUrl;

    /** 每日可预约截止时间（HH:mm:ss），过了该时间只能预约第二天的 */
    @Excel(name = "截止时间")
    @Size(min = 0, max = 8, message = "截止时间格式不正确")
    private String cutOffTime;

    /** 预约停留秒数（用户在预约界面可停留的时长） */
    @Excel(name = "预约停留秒数", cellType = ColumnType.NUMERIC)
    private Integer reservationStaySecond;

    /** 预约开始时间（HH:mm:ss） */
    @Excel(name = "预约开始时间")
    @Size(min = 0, max = 8, message = "预约开始时间格式不正确")
    private String reservationStartTime;

    /** 预约结束时间（HH:mm:ss） */
    @Excel(name = "预约结束时间")
    @Size(min = 0, max = 8, message = "预约结束时间格式不正确")
    private String reservationEndTime;

    /** 预约间隔 */
    @Excel(name = "预约间隔", cellType = ColumnType.NUMERIC)
    private Integer reservationIntervalSecond;

    /** 预约人数（每个时间段可预约的人数） */
    @Excel(name = "预约人数", cellType = ColumnType.NUMERIC)
    private Integer reservationCount;

    /** 可提前预约天数 */
    @Excel(name = "可提前预约天数", cellType = ColumnType.NUMERIC)
    private Integer advanceReservationDays;

    /** 随行人数 */
    @Excel(name = "随行人数", cellType = ColumnType.NUMERIC)
    private Integer travelerCount;

    /** 预约须知（富文本） */
    @NotBlank(message = "预约须知不能为空")
    private String reservationNotes;

    /** 排序 */
    @Excel(name = "排序", cellType = ColumnType.NUMERIC)
    private Integer sort;

    /** 状态（0正常/启用 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    @TableField("status")
    private String status;
}
