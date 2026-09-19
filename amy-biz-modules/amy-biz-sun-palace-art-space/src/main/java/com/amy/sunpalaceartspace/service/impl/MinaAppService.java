package com.amy.sunpalaceartspace.service.impl;

import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectInfoResp;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName MinaAppService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-19 12:00
 */
@AllArgsConstructor
@Service
public class MinaAppService {
    private final IProjectsService projectsService;

    public IPage<MinaProjectInfoResp> extractProjectInfo(Page<Projects> page) {
        Projects criteria = new Projects();
        criteria.setStatus("0");
        IPage<Projects> projectsIPage = projectsService.selectProjectsList(page, criteria);

        IPage<MinaProjectInfoResp> pageResp = new Page();
        List<MinaProjectInfoResp> list = projectsIPage.getRecords().stream().map(project -> {
            MinaProjectInfoResp resp = new MinaProjectInfoResp();
            BeanUtils.copyProperties(project, resp);
            return resp;
        }).toList();
        pageResp.setRecords(list);
        BeanUtils.copyProperties(projectsIPage, pageResp);

        return pageResp;
    }
}
