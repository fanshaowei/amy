package com.amy.sunpalaceartspace.controller;

import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.amy.common.core.utils.poi.ExcelUtil;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.domain.AjaxResult;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.Projects;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 项目管理 信息操作处理（MyBatis-Plus 模式）
 *
 * @author amy
 */
@RestController
@RequestMapping("/project")
public class ProjectsController extends BaseController
{
    @Autowired
    private IProjectsService projectsService;

    /**
     * 查询项目列表（MP IPage 分页）
     */
    @RequiresPermissions("sunpalaceartspace:project:list")
    @GetMapping("/list")
    public TableDataInfo list(Projects projects)
    {
        // 读取前端分页参数（pageNum/pageSize），不再依赖 PageHelper
        com.amy.common.core.web.page.PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<Projects> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<Projects> result = projectsService.selectProjectsList(page, projects);
        return new TableDataInfo(result.getRecords(), (int) result.getTotal());
    }

    /**
     * 导出项目列表
     */
    @RequiresPermissions("sunpalaceartspace:project:export")
    @Log(title = "项目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Projects projects)
    {
        List<Projects> list = projectsService.selectProjectsExportList(projects);
        ExcelUtil<Projects> util = new ExcelUtil<Projects>(Projects.class);
        util.exportExcel(response, list, "项目数据");
    }

    /**
     * 获取项目详细信息
     */
    @RequiresPermissions("sunpalaceartspace:project:query")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectsService.getById(projectId));
    }

    /**
     * 新增项目
     */
    @RequiresPermissions("sunpalaceartspace:project:add")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Projects projects)
    {
        projects.setCreateBy(SecurityUtils.getUsername());
        return toAjax(projectsService.saveProjects(projects));
    }

    /**
     * 修改项目
     */
    @RequiresPermissions("sunpalaceartspace:project:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Projects projects)
    {
        projects.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(projectsService.updateProjects(projects));
    }

    /**
     * 删除项目（物理删除，biz_projects 无 del_flag 列）
     */
    @RequiresPermissions("sunpalaceartspace:project:remove")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        return toAjax(projectsService.removeProjectsByIds(projectIds));
    }
}
