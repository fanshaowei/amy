package com.amy.sunpalaceartspace.controller;

import java.util.List;

import com.amy.common.core.domain.R;
import com.amy.common.core.utils.poi.ExcelUtil;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigGroupVO;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigVO;
import com.amy.sunpalaceartspace.service.IProjectReservationConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目预约时段配置 信息操作处理（MyBatis-Plus 模式）
 *
 * <p>配置某个项目在具体日期的具体时段「可预约 / 不可预约」。</p>
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
@RestController
@RequestMapping("/reservationConfig")
public class ProjectReservationConfigController extends BaseController
{
    private final IProjectReservationConfigService projectReservationConfigService;

    public ProjectReservationConfigController(IProjectReservationConfigService projectReservationConfigService) {
        this.projectReservationConfigService = projectReservationConfigService;
    }

    /**
     * 按项目ID批量查询时段配置，并按项目分组返回
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:list")
    @GetMapping("/groupByProject")
    public R<List<ProjectReservationConfigGroupVO>> groupByProject(
            @RequestParam("projectIds") List<Long> projectIds)
    {
        return R.ok(projectReservationConfigService.selectGroupByProjectIds(projectIds));
    }

    /**
     * 查询预约时段配置列表（MP IPage 分页）
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:list")
    @GetMapping("/list")
    public R<TableDataInfo> list(ProjectReservationConfig config)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<ProjectReservationConfigVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<ProjectReservationConfigVO> result = projectReservationConfigService.selectConfigList(page, config);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    /**
     * 导出预约时段配置列表
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:export")
    @Log(title = "预约时段配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, @RequestBody ProjectReservationConfig config)
    {
        List<ProjectReservationConfigVO> list = projectReservationConfigService.selectConfigExportList(config);
        ExcelUtil<ProjectReservationConfigVO> util = new ExcelUtil<>(ProjectReservationConfigVO.class);
        util.exportExcel(response, list, "项目预约时段配置");
    }

    /**
     * 获取预约时段配置详细信息
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:query")
    @GetMapping(value = "/{configId}")
    public R<ProjectReservationConfigVO> getInfo(@PathVariable("configId") Long configId)
    {
        return R.ok(projectReservationConfigService.selectConfigById(configId));
    }

    /**
     * 新增预约时段配置
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:add")
    @Log(title = "预约时段配置", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Boolean> add(@Validated @RequestBody ProjectReservationConfig config)
    {
        config.setCreateBy(SecurityUtils.getUsername());
        return R.ok(projectReservationConfigService.saveConfig(config));
    }

    /**
     * 修改预约时段配置
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:edit")
    @Log(title = "预约时段配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> edit(@Validated @RequestBody ProjectReservationConfig config)
    {
        config.setUpdateBy(SecurityUtils.getUsername());
        return R.ok(projectReservationConfigService.updateConfig(config));
    }

    /**
     * 删除预约时段配置（软删除）
     */
    @RequiresPermissions("sunpalaceartspace:reservationConfig:remove")
    @Log(title = "预约时段配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public R<Boolean> remove(@PathVariable Long[] configIds)
    {
        return R.ok(projectReservationConfigService.removeConfigByIds(configIds));
    }
}
