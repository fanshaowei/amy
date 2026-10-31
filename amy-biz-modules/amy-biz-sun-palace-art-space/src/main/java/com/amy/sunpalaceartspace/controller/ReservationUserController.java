package com.amy.sunpalaceartspace.controller;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.amy.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
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
import com.amy.sunpalaceartspace.utils.DateRangeParamUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 预约会员 信息操作处理（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Slf4j
@RestController
@RequestMapping("/reservation/user")
public class ReservationUserController extends BaseController {

    /**
     * 会员导出要求按时间分片的最大跨度（天）
     *
     * <p>只在导出量级超过 {@link #MAX_EXPORT_ROWS_WITHOUT_RANGE} 时才强制要求，
     * 结果行数很少时无需分片，可任意跨度导出。</p>
     */
    private static final int MAX_EXPORT_RANGE_DAYS = 7;

    /**
     * 不限制时间跨度、可直接导出的最大行数
     */
    private static final int MAX_EXPORT_ROWS_WITHOUT_RANGE = 5000;

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
    public void export(HttpServletResponse response,
                       @RequestBody(required = false) ReservationUser reservationUser)
    {
        // 无请求体时为 null，XML 中的 OGNL（entity.xxx）会因 null 报错，故兜底为空条件对象
        ReservationUser query = reservationUser == null ? new ReservationUser() : reservationUser;
        // 归一化时间格式：列表走 query string 传 '2026-10-10'，导出走 JSON body 可能被 dayjs
        // 序列化成 '2026-10-09T16:00:00.000Z'，统一成 yyyy-MM-dd 后再校验跨度才不会算错
        DateRangeParamUtils.normalize(query);

        /*
         * 按「量级」决定是否要求时间分片，而不是一刀切：
         *   1. 时间跨度合规（≤ 7 天）→ 用户已主动分片，无论多少行都放行，且不额外查 count；
         *   2. 跨度不合规 → 先数行数：
         *        - 不超过 MAX_EXPORT_ROWS_WITHOUT_RANGE 条：内存可控，放行；
         *        - 超过：拒绝，并提示当前行数，引导用户缩小时间范围。
         * 绝大多数导出都是第 1 种（前端引导用户按时间筛选），因此这里几乎不会多出 count 开销。
         */
        String rangeError = DateRangeParamUtils.validateExportRange(query, MAX_EXPORT_RANGE_DAYS);
        if (rangeError != null)
        {
            long total = reservationUserService.countReservationUserExport(query);
            if (total > MAX_EXPORT_ROWS_WITHOUT_RANGE)
            {
                writeJsonError(response, rangeError
                        + "（当前查询结果 " + total + " 条，超过 " + MAX_EXPORT_ROWS_WITHOUT_RANGE
                        + " 条请缩小注册时间范围后重新查询再导出）");
                return;
            }
            log.info("会员导出：时间跨度超出 {} 天，但结果仅 {} 条，放行", MAX_EXPORT_RANGE_DAYS, total);
        }

        List<ReservationUserVO> list = reservationUserService.selectReservationUserExportList(query);
        ExcelUtil<ReservationUserVO> util = new ExcelUtil<>(ReservationUserVO.class);
        util.exportExcel(response, list, "预约会员数据");
    }

    /**
     * 校验不通过时写 JSON 错误体
     *
     * <p>导出接口返回的是文件流，全局响应拦截器读不到 {@code code} 字段（body 是 Blob），
     * 所以这里直接写 {@code application/json}，由前端按 Content-Type 区分成功文件与错误提示，
     * 见 {@code amy-ui/src/utils/download.ts} 的 JSON 错误识别逻辑。</p>
     */
    private void writeJsonError(HttpServletResponse response, String msg)
    {
        response.reset();
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");
        try {
            response.getWriter().write(JSON.toJSONString(AjaxResult.error(msg)));
        }
        catch (IOException e) {
            log.error("会员导出校验失败，且错误信息回写失败: {}", msg, e);
        }
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
     * 切换会员身份（1用户 2核销员）
     */
    @RequiresPermissions("sunpalaceartspace:reservationUser:edit")
    @Log(title = "预约会员管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeType/{reservationUserId}/{type}")
    public R<AjaxResult> changeType(@PathVariable Long reservationUserId, @PathVariable Integer type)
    {
        return R.ok(toAjax(reservationUserService.changeUserType(reservationUserId, type)));
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
