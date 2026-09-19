package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectReservationInfoResp;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.amy.sunpalaceartspace.service.impl.MinaAppService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    private final IReservationOrderService reservationOrderService;

    /**
     * 小程序首页项目列表信息
     * @return
     */
    @GetMapping("/homepage/project")
    public R<TableDataInfo> getHomePage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<Projects> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<MinaHomePageProjectResp> result = minaAppService.extractHomePageProjectInfo(page);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    /**
     * 小程序预订页项目预订信息
     * @param projectId
     * @return
     */
    @GetMapping("/project/reservation/info/{projectId}")
    public R<MinaProjectReservationInfoResp> getProjectReservationInfo(@PathVariable("projectId") Long projectId) {
        MinaProjectReservationInfoResp resp = minaAppService.extractProjectReservationInfo(projectId);
        return R.ok(resp);
    }

    /**
     * 提交预约记录
     * @param request
     * @return
     */
    @PostMapping("/reservation/submit")
    public R<Boolean> createReservation(@RequestBody ReservationOrderReq request) {
        return R.ok(reservationOrderService.saveReservationOrder(request));
    }

    /**
     * 获取预订记录
     * @param reservationUserId
     * @return
     */
    @GetMapping("/reservation/list/{reservationUserId}")
    public R<TableDataInfo> list(@PathVariable("reservationUserId") Long reservationUserId) {
        ReservationOrderReq request = new ReservationOrderReq();
        request.setReservationUserId(reservationUserId);

        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<ReservationOrderVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<ReservationOrderVO> result = reservationOrderService.selectReservationOrderList(page, request);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

}
