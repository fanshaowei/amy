package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.common.core.utils.poi.ExcelUtil;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.domain.AjaxResult;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public R<TableDataInfo> list(Projects projects)
    {
        // 读取前端分页参数（pageNum/pageSize），不再依赖 PageHelper
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<Projects> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<Projects> result = projectsService.selectProjectsList(page, projects);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
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
    public R<Projects> getInfo(@PathVariable("projectId") Long projectId)
    {
        return R.ok(projectsService.getById(projectId));
    }

    /**
     * 新增项目
     */
    @RequiresPermissions("sunpalaceartspace:project:add")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Boolean> add(@Validated @RequestBody Projects projects)
    {
        projects.setCreateBy(SecurityUtils.getUsername());
        return R.ok(projectsService.saveProjects(projects));
    }

    /**
     * 修改项目
     */
    @RequiresPermissions("sunpalaceartspace:project:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> edit(@Validated @RequestBody Projects projects)
    {
        projects.setUpdateBy(SecurityUtils.getUsername());
        return R.ok(projectsService.updateProjects(projects));
    }

    /**
     * 删除项目（软删除：Projects 继承 BaseEntity 且 del_flag 标注 @TableLogic，
     * MP 的 removeByIds 会转换为 UPDATE ... SET del_flag='1'，物理行不会被移除）
     */
    @RequiresPermissions("sunpalaceartspace:project:remove")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectIds}")
    public R<Boolean> remove(@PathVariable Long[] projectIds)
    {
        return R.ok(projectsService.removeProjectsByIds(projectIds));
    }
}
