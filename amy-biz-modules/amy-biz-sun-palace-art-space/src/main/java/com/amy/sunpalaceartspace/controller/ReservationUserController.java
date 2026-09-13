package com.amy.sunpalaceartspace.controller;

import java.util.List;

import com.amy.common.core.domain.R;
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
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationUserVO;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 预约会员 信息操作处理（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@RestController
@RequestMapping("/reservation/user")
public class ReservationUserController extends BaseController {

    @Autowired
    private IReservationUserService reservationUserService;

    /**
     * 查询预约会员列表（MP IPage 分页）
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:list")
    @GetMapping("/list")
    public R<TableDataInfo> list(ReservationUser reservationUser)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<ReservationUserVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<ReservationUserVO> result = reservationUserService.selectReservationUserList(page, reservationUser);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    /**
     * 导出预约会员列表
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:export")
    @Log(title = "预约会员管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ReservationUser reservationUser)
    {
        List<ReservationUserVO> list = reservationUserService.selectReservationUserExportList(reservationUser);
        ExcelUtil<ReservationUserVO> util = new ExcelUtil<>(ReservationUserVO.class);
        util.exportExcel(response, list, "预约会员数据");
    }

    /**
     * 获取预约会员详细信息
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:query")
    @GetMapping(value = "/{reservationUserId}")
    public R<ReservationUserVO> getInfo(@PathVariable("reservationUserId") Long reservationUserId)
    {
        return R.ok(reservationUserService.selectReservationUserById(reservationUserId));
    }

    /**
     * 新增预约会员
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:add")
    @Log(title = "预约会员管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<AjaxResult> add(@Validated @RequestBody ReservationUserReq req)
    {
        return R.ok(toAjax(reservationUserService.saveReservationUser(req)));
    }

    /**
     * 修改预约会员
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:edit")
    @Log(title = "预约会员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<AjaxResult> edit(@Validated @RequestBody ReservationUserReq req)
    {
        return R.ok(toAjax(reservationUserService.updateReservationUser(req)));
    }

    /**
     * 删除预约会员（软删除：ReservationUser 继承 BaseEntity 且 del_flag 标注 @TableLogic，
     * MP 的 removeByIds 会转换为 UPDATE ... SET del_flag='1'，物理行不会被移除）
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:remove")
    @Log(title = "预约会员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reservationUserIds}")
    public R<AjaxResult> remove(@PathVariable Long[] reservationUserIds)
    {
        return R.ok(toAjax(reservationUserService.removeReservationUserByIds(reservationUserIds)));
    }
}
