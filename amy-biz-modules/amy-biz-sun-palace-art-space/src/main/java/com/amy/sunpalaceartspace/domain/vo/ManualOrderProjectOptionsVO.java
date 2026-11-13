package com.amy.sunpalaceartspace.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 后台「手动添加预约订单」表单的初始化数据。
 *
 * <p>单独走 {@code /reservation/order/manual/options}（权限 :add），避免操作员为了拿项目下拉
 * 还要额外拥有「项目管理」的 {@code sunpalaceartspace:project:list} 权限。</p>
 *
 * @author fantasyfan
 */
@Data
public class ManualOrderProjectOptionsVO {

    /** 可预约项目（仅状态正常的项目） */
    private List<ProjectOption> projects;

    @Data
    public static class ProjectOption {
        /** 项目ID */
        private Long projectId;
        /** 项目名称 */
        private String projectName;
        /** 项目允许的最大随行人数（不含预约人本人） */
        private Integer travelerCount;
        /** 预约开始时间（HH:mm:ss） */
        private String reservationStartTime;
        /** 预约结束时间（HH:mm:ss） */
        private String reservationEndTime;
        /**
         * 可选时段（HH:mm:ss），由项目开始/结束时间与间隔生成，与放号口径一致。
         * 项目未配置时段规则时为空数组，此时前端允许手动输入时间。
         */
        private List<String> timeSlots;
    }
}
