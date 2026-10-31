package com.amy.sunpalaceartspace.domain.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 按项目分组的「预约时段配置」返回对象
 *
 * <p>一个项目对应一组时段配置（多条）。用于「传多个 projectId，一次性拿回各项目的时段配置」
 * 这类场景，避免调用方循环查库。</p>
 *
 * <p><b>约定</b>：请求里传入的每个 projectId 都会返回一条分组记录，
 * 即使该项目<b>没有任何配置</b>，也会返回 {@code configs = []}（不会从结果里消失），
 * 这样调用方可以按入参顺序一一对应，不需要自己再做存在性判断。</p>
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
@Data
public class ProjectReservationConfigGroupVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 项目名称（项目被删除或不存在时为 null） */
    private String projectName;

    /** 该项目的时段配置列表（按开始时间升序；无配置时为空数组，不会是 null） */
    private List<ProjectReservationConfigVO> configs = new ArrayList<>();
}
