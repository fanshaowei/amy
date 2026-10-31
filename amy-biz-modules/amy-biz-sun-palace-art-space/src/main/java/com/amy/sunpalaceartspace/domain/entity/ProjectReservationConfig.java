package com.amy.sunpalaceartspace.domain.entity;

import com.amy.common.core.annotation.Excel;
import com.amy.common.core.annotation.Excel.ColumnType;
import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目预约时段配置对象 biz_spas_project_reservation_config
 *
 * <p><b>本表不区分日期</b>：一条配置对项目<b>每一天</b>都生效，长期有效。
 * 不需要（也不应该）为每一天各插一条记录。</p>
 *
 * <p>配置某个项目的哪些时间段<b>可预约</b>（白名单）或<b>不可预约</b>（黑名单），
 * 由 {@code reservationEnabled} 控制；放号时每天都会读取这套配置，只对开放的时段放号。</p>
 *
 * <p>与项目默认规则的关系（优先级从高到低）：<br>
 * 不可预约(false) &gt; 可预约(true) &gt; 无配置（走 Projects 的 reservationStartTime~reservationEndTime）。</p>
 *
 * @Description 项目预约时段配置
 * @Author fantasyfan
 * @Date 2026-10-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_project_reservation_config")
public class ProjectReservationConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @Excel(name = "配置ID", cellType = ColumnType.NUMERIC)
    @TableId(type = IdType.AUTO)
    private Long configId;

    /** 项目ID */
    @Excel(name = "项目ID", cellType = ColumnType.NUMERIC)
    @NotNull(message = "项目不能为空")
    private Long projectId;

    /** 预约开始时间（HH:mm:ss），每天生效 */
    @Excel(name = "开始时间")
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 预约结束时间（HH:mm:ss，不含，左闭右开） */
    @Excel(name = "结束时间")
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 是否可预约（true=可预约 false=不可预约） */
    @Excel(name = "是否可预约", readConverterExp = "true=可预约,false=不可预约")
    @NotNull(message = "是否可预约不能为空")
    private Boolean reservationEnabled;
}
