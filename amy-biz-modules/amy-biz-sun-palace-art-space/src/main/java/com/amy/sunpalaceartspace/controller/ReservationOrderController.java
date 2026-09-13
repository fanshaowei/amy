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
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 预约订单 信息操作处理（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@RestController
@RequestMapping("/reservation/order")
public class ReservationOrderController extends BaseController {

    @Autowired
    private IReservationOrderService reservationOrderService;

    /**
     * 查询预约订单列表（MP IPage 分页）
     */
    @RequiresPermissions("sunpalaceartspace:reservationOrder:list")
    @GetMapping("/list")
    public R<TableDataInfo> list(ReservationOrder reservationOrder) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<ReservationOrderVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<ReservationOrderVO> result = reservationOrderService.selectReservationOrderList(page, reservationOrder);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    /**
     * 导出预约订单列表
     */
    @RequiresPermissions("sunpalaceartspace:reservationOrder:export")
    @Log(title = "预约订单管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ReservationOrder reservationOrder) {
        List<ReservationOrderVO> list = reservationOrderService.selectReservationOrderExportList(reservationOrder);
        ExcelUtil<ReservationOrderVO> util = new ExcelUtil<>(ReservationOrderVO.class);
        util.exportExcel(response, list, "预约订单数据");
    }

    /**
     * 获取预约订单详细信息
     */
    @RequiresPermissions("sunpalaceartspace:reservationOrder:query")
    @GetMapping(value = "/{reservationOrderId}")
    public R<ReservationOrderVO> getInfo(@PathVariable("reservationOrderId") Long reservationOrderId) {
        return R.ok(reservationOrderService.selectReservationOrderById(reservationOrderId));
    }

    /**
     * 新增预约订单
     */
    @RequiresPermissions("sunpalaceartspace:reservationOrder:add")
    @Log(title = "预约订单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Boolean> add(@Validated @RequestBody ReservationOrderReq req) {
        return R.ok(reservationOrderService.saveReservationOrder(req));
    }

    /**
     * 修改预约订单
     */
    @RequiresPermissions("sunpalaceartspace:reservationOrder:edit")
    @Log(title = "预约订单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> edit(@Validated @RequestBody ReservationOrderReq req) {
        return R.ok(reservationOrderService.updateReservationOrder(req));
    }

    /**
     * 删除预约订单（软删除：ReservationOrder 继承 BaseEntity 且 del_flag 标注 @TableLogic，
     * MP 的 removeByIds 会转换为 UPDATE ... SET del_flag='1'，物理行不会被移除）
     */
    @RequiresPermissions("sunpalaceartspace:reservationOrder:remove")
    @Log(title = "预约订单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reservationOrderIds}")
    public R<Boolean> remove(@PathVariable Long[] reservationOrderIds) {
        return R.ok(reservationOrderService.removeReservationOrderByIds(reservationOrderIds));
    }
}
