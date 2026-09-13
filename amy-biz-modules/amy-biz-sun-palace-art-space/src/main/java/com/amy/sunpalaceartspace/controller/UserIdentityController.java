package com.amy.sunpalaceartspace.controller;

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

import com.amy.common.core.domain.R;
import com.amy.common.core.utils.poi.ExcelUtil;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.sunpalaceartspace.domain.entity.UserIdentity;
import com.amy.sunpalaceartspace.domain.req.UserIdentityReq;
import com.amy.sunpalaceartspace.domain.vo.UserIdentityVO;
import com.amy.sunpalaceartspace.service.IUserIdentityService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户身份信息 信息操作处理
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@RestController
@RequestMapping("/identity")
public class UserIdentityController extends BaseController {

    @Autowired
    private IUserIdentityService userIdentityService;

    /**
     * 分页列表
     */
    @RequiresPermissions("sunpalaceartspace:userIdentity:list")
    @GetMapping("/list")
    public R<TableDataInfo> list(UserIdentity userIdentity)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<UserIdentityVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<UserIdentityVO> result = userIdentityService.selectUserIdentityList(page, userIdentity);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    /**
     * 导出 Excel
     */
    @RequiresPermissions("sunpalaceartspace:userIdentity:export")
    @Log(title = "用户身份信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserIdentity userIdentity)
    {
        List<UserIdentityVO> list = userIdentityService.selectUserIdentityExportList(userIdentity);
        ExcelUtil<UserIdentityVO> util = new ExcelUtil<>(UserIdentityVO.class);
        util.exportExcel(response, list, "用户身份信息");
    }

    /**
     * 详情
     */
    @RequiresPermissions("sunpalaceartspace:userIdentity:query")
    @GetMapping(value = "/{userIdentityId}")
    public R<UserIdentityVO> getInfo(@PathVariable("userIdentityId") Long userIdentityId)
    {
        return R.ok(userIdentityService.selectUserIdentityById(userIdentityId));
    }

    /**
     * 新增
     */
    @RequiresPermissions("sunpalaceartspace:userIdentity:add")
    @Log(title = "用户身份信息", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Boolean> add(@Validated @RequestBody UserIdentityReq req)
    {
        return R.ok(userIdentityService.saveUserIdentity(req));
    }

    /**
     * 修改
     */
    @RequiresPermissions("sunpalaceartspace:userIdentity:edit")
    @Log(title = "用户身份信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> edit(@Validated @RequestBody UserIdentityReq req)
    {
        return R.ok(userIdentityService.updateUserIdentity(req));
    }

    /**
     * 删除（软删除：UserIdentity 继承 BaseEntity 且 del_flag 标注 @TableLogic，
     * MP 的 removeByIds 会转换为 UPDATE ... SET del_flag='1'，物理行不会被移除）
     */
    @RequiresPermissions("sunpalaceartspace:userIdentity:remove")
    @Log(title = "用户身份信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIdentityIds}")
    public R<Boolean> remove(@PathVariable Long[] userIdentityIds)
    {
        return R.ok(userIdentityService.removeUserIdentityByIds(userIdentityIds));
    }
}
