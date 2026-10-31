package com.amy.sunpalaceartspace.utils;

import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;

/**
 * 项目预约时段开关判定器
 *
 * <p>判定「某个项目的某个时段」是否开放预约。配置<b>不区分日期</b>，对项目每一天都生效。</p>
 *
 * <p>规则（优先级从高到低）：</p>
 * <ol>
 *     <li><b>不可预约（reservationEnabled=false）</b>：命中即否决，立即返回 false —— 采用黑名单优先的安全默认；</li>
 *     <li><b>可预约（reservationEnabled=true）</b>：记录为开放；</li>
 *     <li><b>无任何配置命中</b>：返回 true，走项目自身的默认开放时段（Projects.reservationStartTime~EndTime）。</li>
 * </ol>
 *
 * <p>命中判定：以<b>时段起点</b>是否落在配置的 {@code [startTime, endTime)} 左闭右开区间为准。
 * 例如 09:30 的时段，会被 09:00-12:00 的配置命中。</p>
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
@Slf4j
public final class ProjectReservationConfigChecker {

    /** 时段时间格式 HH:mm:ss */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ProjectReservationConfigChecker() {
    }

    /**
     * 判定指定时段是否开放预约（不区分日期，每天通用）
     *
     * @param configs   该项目的全部时段配置（可为空）
     * @param slotStart 时段起点（如 09:30）
     * @return true=可预约；false=不可预约
     */
    public static boolean isOpen(Collection<ProjectReservationConfig> configs, LocalTime slotStart) {
        if (configs == null || configs.isEmpty()) {
            // 无配置 → 按项目默认规则开放
            return true;
        }
        for (ProjectReservationConfig config : configs) {
            if (!matches(config, slotStart)) {
                continue;
            }
            if (!Boolean.TRUE.equals(config.getReservationEnabled())) {
                // 黑名单优先：命中「不可预约」立即否决
                return false;
            }
        }
        // 没有命中任何规则，或命中的全是开放规则
        return true;
    }

    /**
     * 判断某条配置是否覆盖指定时段
     */
    public static boolean matches(ProjectReservationConfig config, LocalTime slotStart) {
        if (config == null || slotStart == null) {
            return false;
        }
        LocalTime start = parseTime(config.getStartTime());
        LocalTime end = parseTime(config.getEndTime());
        if (start == null || end == null) {
            return false;
        }
        // 左闭右开 [start, end)
        return !slotStart.isBefore(start) && slotStart.isBefore(end);
    }

    /**
     * 解析 HH:mm:ss 时间，格式非法返回 null（不抛异常，避免脏数据导致整段链路 500）
     */
    public static LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String normalized = time.trim().length() == 5 ? time.trim() + ":00" : time.trim();
        try {
            return LocalTime.parse(normalized, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("预约时段配置时间格式非法：{}", time);
            return null;
        }
    }

    /**
     * 空集合安全包装
     */
    public static boolean isOpenQuietly(Collection<ProjectReservationConfig> configs, LocalTime slotStart) {
        return isOpen(configs == null ? Collections.emptyList() : configs, slotStart);
    }
}
