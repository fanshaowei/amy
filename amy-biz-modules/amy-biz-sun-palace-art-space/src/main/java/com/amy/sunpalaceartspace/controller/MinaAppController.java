package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.sunpalaceartspace.annotation.MinaApiAuth;
import com.amy.sunpalaceartspace.domain.bo.ReservationOrderMqSendBO;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.req.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectReservationInfoResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaReservationUserInfoResp;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.amy.sunpalaceartspace.service.impl.MinaAppService;
import com.amy.sunpalaceartspace.service.impl.MinaAppLoginService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
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
public class MinaAppController extends BaseController {
    private final MinaAppService minaAppService;
    private final IReservationOrderService reservationOrderService;
    private final IReservationUserService reservationUserService;
    private final MinaAppLoginService minaAppLoginService;

    /**
     * 小程序调用后端接口前，获取鉴权token,后续调用业务接口，都要在请求头携带该token
     * eg: Authorization: Bearer <token>
     * @param req
     * @return
     * @throws WxErrorException
     */
    @PostMapping("/auth/login")
    public R<String> login(@RequestBody WechatApiAuthLoginReq req) throws WxErrorException {
        String token = minaAppLoginService.generalApiAuthToken(req);
        return R.ok(token);
    }

    @GetMapping("/reservation/order/mq")
    public void sendReservationOrderMq(ReservationOrderMqSendBO reservationOrderMqSendBO) {
        minaAppService.sendReservationOrderMq(reservationOrderMqSendBO);
    }

    /**
     * 小程序首页项目列表信息
     * @return
     */
    @MinaApiAuth
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
    @MinaApiAuth
    @GetMapping("/project/reservation/info/{projectId}")
    public R<MinaProjectReservationInfoResp> getProjectReservationInfo(
            @PathVariable("projectId") @NotNull @Min(1) Long projectId
    ) {
        MinaProjectReservationInfoResp resp = minaAppService.extractProjectReservationInfo(projectId);
        return R.ok(resp);
    }

    /**
     * 提交预约记录
     * @param request
     * @return
     */
    @MinaApiAuth
    @PostMapping("/reservation/submit")
    public R<String> submitReservation(@RequestHeader("Authorization") String token,
            @RequestBody @Validated SubmitReservationReq request) {
        return R.ok(minaAppService.submitReservationOrder(token, request));
    }

    /**
     * 获取预订记录
     * @param token
     * @return
     */
    @MinaApiAuth
    @GetMapping("/user/reservation/records")
    public R<TableDataInfo> userReservationRecords(@RequestHeader("Authorization") String token) {
        ReservationUser user = minaAppService.getUserByToken(token);
        ReservationOrderReq request = new ReservationOrderReq();
        request.setReservationUserId(user.getReservationUserId());

        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<ReservationOrderVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<ReservationOrderVO> result = reservationOrderService.selectReservationOrderList(page, request);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    @MinaApiAuth
    @GetMapping("/user/info")
    public R<MinaReservationUserInfoResp> getUserInfo(@RequestHeader("Authorization") String token) {
        return R.ok(minaAppService.extractUserInfo(token));
    }

    @MinaApiAuth
    @GetMapping("/reservation/{orderNum}")
    public R<ReservationOrderVO> getReservationOrder(@PathVariable("orderNum") String orderNum) {
        ReservationOrder reservationOrder = reservationOrderService.selectReservationOrderByNum(orderNum);
        ReservationOrderVO vo = new ReservationOrderVO();
        BeanUtils.copyProperties(reservationOrder, vo);
        return R.ok(vo);
    }

    @MinaApiAuth
    @PostMapping("/reservation/verify/{orderNum}")
    public R<Boolean> verifyReservationOrder(@RequestHeader("Authorization") String token,
            @PathVariable("orderNum") String orderNum) {
        return R.ok(minaAppService.verifyReservationOrder(token, orderNum));
    }
}
