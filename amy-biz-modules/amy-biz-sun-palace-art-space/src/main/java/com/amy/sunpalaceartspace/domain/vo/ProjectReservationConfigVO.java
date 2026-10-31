package com.amy.sunpalaceartspace.domain.vo;

import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目预约时段配置 列表展示对象（附带项目名称，便于管理端展示）
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectReservationConfigVO extends ProjectReservationConfig {
    private static final long serialVersionUID = 1L;

    /** 项目名称（join biz_spas_projects 得到，非本表字段） */
    private String projectName;
}
