package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.domain.AjaxResult;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.sunpalaceartspace.domain.resp.DashboardStatisticsResp;
import com.amy.sunpalaceartspace.service.IStatisticService;
import lombok.RequiredArgsConstructor;

/**
 * 控制台统计 信息操作处理
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController extends BaseController {

    private final IStatisticService statisticService;

    /**
     * 获取控制台统计信息
     */
    @RequiresPermissions("sunpalaceartspace:dashboard:query")
    @GetMapping("/statistics")
    public R<DashboardStatisticsResp> getStatistics()
    {
        DashboardStatisticsResp resp = statisticService.getDashboardStatistics();
        return R.ok(resp);
    }
}
