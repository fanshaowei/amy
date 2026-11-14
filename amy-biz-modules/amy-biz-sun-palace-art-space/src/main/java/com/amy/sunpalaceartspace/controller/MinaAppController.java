package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.page.PageDomain;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.core.web.page.TableSupport;
import com.amy.sunpalaceartspace.annotation.MinaApiAuth;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.req.UserIdentityReq;
import com.amy.sunpalaceartspace.domain.req.mina.SubmitReservationReq;
import com.amy.sunpalaceartspace.domain.req.mina.UpdateReservationUserReq;
import com.amy.sunpalaceartspace.domain.req.mina.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.domain.resp.mina.*;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.domain.vo.UserIdentityVO;
import com.amy.sunpalaceartspace.service.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    private final SubmitReservationOrderService submitReservationOrderService;

    /**
     * 上传预约用户微信头像
     * @param file multipart 表单字段名固定为 file
     * @return 头像持久化访问地址
     */
    @PostMapping("/user/avatar/upload")
    public R<String> uploadReservationUserAvatar(@RequestParam("file") MultipartFile file) {
        String url = minaAppLoginService.uploadReservationUserAvatar(file);
        return R.ok(url);
    }

    /**
     * 小程序调用后端接口前，获取鉴权token,后续调用业务接口，都要在请求头携带该token
     * eg: Authorization: Bearer <token>
     * @param req
     * @return
     * @throws WxErrorException
     */
    @PostMapping("/auth/login")
    public R<WechatApiAuthLoginResp> login(@Validated @RequestBody WechatApiAuthLoginReq req) throws WxErrorException {
        WechatApiAuthLoginResp resp = minaAppLoginService.generalApiAuthToken(req);
        return R.ok(resp);
    }

    @MinaApiAuth
    @PostMapping("/reservation/user/update")
    public R<ReservationUser> updateReservationUser(@RequestHeader("Authorization") String token,
            @Validated @RequestBody UpdateReservationUserReq req) {
        ReservationUser reservationUser = minaAppLoginService.updateReservationUserInfo(token, req);
        return R.ok(reservationUser);
    }

    /**
     * 小程序首页项目列表信息
     * @return
     */
    //@MinaApiAuth
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
    public R<ReservationOrder> submitReservation(@RequestHeader("Authorization") String token,
            @RequestBody @Validated SubmitReservationReq request) {
        return R.ok(submitReservationOrderService.submitReservationOrder(token, request));
    }

    /**
     * 获取用户所有预订记录
     * @param token
     * @return
     */
    @MinaApiAuth
    @GetMapping("/user/reservation/records")
    public R<TableDataInfo> userReservationRecords(@RequestHeader("Authorization") String token) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        ReservationOrderReq request = new ReservationOrderReq();
        request.setReservationUserId(user.getReservationUserId());

        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<ReservationOrderVO> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        IPage<ReservationOrderVO> result = reservationOrderService.selectReservationOrderList(page, request);
        return R.ok(new TableDataInfo(result.getRecords(), (int) result.getTotal()));
    }

    /**
     * 获取用户信息
     * @param token
     * @return
     */
    @MinaApiAuth
    @GetMapping("/user/info")
    public R<MinaReservationUserInfoResp> getUserInfo(@RequestHeader("Authorization") String token) {
        return R.ok(minaAppService.extractReservationUserInfo(token));
    }

    /**
     * 预约用户实名验证，同时会添加用户信息实名记录
     * @param token
     * @param userIdentityReq
     * @return
     */
    @MinaApiAuth
    @PostMapping("/user/verify")
    public R<Boolean> realNameverifyUser(@RequestHeader("Authorization") String token, @RequestBody @Validated UserIdentityReq userIdentityReq) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        userIdentityReq.setReservationUserId(user.getReservationUserId());
        userIdentityReq.setIsFollower(false);
        return R.ok(minaAppService.realNameVerifyUser(user, userIdentityReq));
    }

    /**
     * 添加随从人,并返回预约会员的所有相关随行人
     * @param token
     * @param userIdentityReq
     * @return
     */
    @MinaApiAuth
    @PostMapping("/user/follower/add")
    public R<List<UserIdentityVO>> addFollower(@RequestHeader("Authorization") String token,
                                               @RequestBody @Validated UserIdentityReq userIdentityReq) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        userIdentityReq.setReservationUserId(user.getReservationUserId());
        userIdentityReq.setIsFollower(true);
        return R.ok(minaAppService.addFollower(user, userIdentityReq));
    }

    @MinaApiAuth
    @GetMapping("/user/followers/get")
    public R<List<UserIdentityVO>> getReservationUserAndFollowers(@RequestHeader("Authorization") String token) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        return R.ok(minaAppService.getUserAndFollowerIdentity(user.getReservationUserId()));
    }

    /**
     * 修改随行人信息（仅本人名下的随行人记录，userIdentityId 必填）
     * @param token
     * @param userIdentityReq
     * @return 修改后的所有随行人列表
     */
    @MinaApiAuth
    @PostMapping("/user/follower/update")
    public R<List<UserIdentityVO>> updateFollower(@RequestHeader("Authorization") String token,
                                                  @RequestBody @Validated UserIdentityReq userIdentityReq) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        return R.ok(minaAppService.updateFollower(user, userIdentityReq));
    }

    /**
     * 删除随行人（软删除，仅本人名下的随行人记录）
     * @param token
     * @param userIdentityId
     * @return 删除后的所有随行人列表
     */
    @MinaApiAuth
    @PostMapping("/user/follower/delete/{userIdentityId}")
    public R<List<UserIdentityVO>> deleteFollower(@RequestHeader("Authorization") String token,
                                                  @PathVariable("userIdentityId") @NotNull Long userIdentityId) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        return R.ok(minaAppService.deleteFollower(user, userIdentityId));
    }

    /**
     * 获取预约订单信息（含关联项目名称）
     * @param orderNum
     * @return
     */
    @MinaApiAuth
    @GetMapping("/reservation/{orderNum}")
    public R<ReservationOrderVO> getReservationOrder(@PathVariable("orderNum") String orderNum) {
        ReservationOrderVO vo = minaAppService.extractReservationOrderInfo(orderNum);
        return R.ok(vo);
    }

    /**
     * 取消预约订单（仅订单本人、仅「待核销」状态可取消）
     * @param token
     * @param orderNum
     * @return
     */
    @MinaApiAuth
    @PostMapping("/reservation/cancel/{orderNum}")
    public R<Boolean> cancelReservationOrder(@RequestHeader("Authorization") String token,
            @PathVariable("orderNum") String orderNum) {
        return R.ok(minaAppService.cancelReservationOrder(token, orderNum));
    }

    /**
     * 核销预约订单
     * @param orderNum
     * @return
     */
    @MinaApiAuth
    @PostMapping("/reservation/verify/{orderNum}/{verifyCode}")
    public R<Boolean> verifyReservationOrder(@PathVariable("orderNum") String orderNum,
                                             @PathVariable("verifyCode") String verifyCode) {
        return R.ok(minaAppService.verifyReservationOrder(orderNum, verifyCode));
    }

    /**
     * 获取第二天预约记录,用于小程序实时数据看板
     * @return
     */
    @MinaApiAuth
    @GetMapping("/reservation/next/day/order")
    public R<List<NextDayReservationOrderResp>> getNextDayReservationOrder() {
        return R.ok(minaAppService.getNextDayReservationOrder());
    }
}
