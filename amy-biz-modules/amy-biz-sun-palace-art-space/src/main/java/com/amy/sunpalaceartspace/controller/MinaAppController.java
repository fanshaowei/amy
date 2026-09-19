package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectReservationInfoResp;
import com.amy.sunpalaceartspace.service.impl.MinaAppService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName MinaAppController
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-14 00:49
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mina/app")
public class MinaAppController {
    private final MinaAppService minaAppService;

    @GetMapping("/homepage/project")
    public R<TableDataInfo> getHomePage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<Projects> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<MinaHomePageProjectResp> result = minaAppService.extractHomePageProjectInfo(page);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    @GetMapping("/project/reservation/info/{projectId}")
    public R<MinaProjectReservationInfoResp> getProjectReservationInfo(@PathVariable("projectId") Long projectId) {
        MinaProjectReservationInfoResp resp = minaAppService.extractProjectReservationInfo(projectId);
        return R.ok(resp);
    }

}
