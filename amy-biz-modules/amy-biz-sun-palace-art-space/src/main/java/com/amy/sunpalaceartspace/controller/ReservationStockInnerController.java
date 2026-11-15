package com.amy.sunpalaceartspace.controller;

import com.amy.common.core.domain.R;
import com.amy.common.security.annotation.InnerAuth;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.amy.sunpalaceartspace.timetask.ReservationStockTask;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约库存放号内部接口（仅允许微服务间 Feign 内部调用，由 {@link InnerAuth} 保护）
 *
 * <p>调度统一由 amy-job 通过本接口触发，避免与 spas 自身定时任务重复放号。</p>
 *
 * @author fantasyfan
 */
@RestController
@RequestMapping("/inner")
public class ReservationStockInnerController
{
    private final ReservationStockTask reservationStockTask;
    private final IReservationOrderService reservationOrderService;

    public ReservationStockInnerController(ReservationStockTask reservationStockTask,
                                           IReservationOrderService reservationOrderService)
    {
        this.reservationStockTask = reservationStockTask;
        this.reservationOrderService = reservationOrderService;
    }

    @InnerAuth
    @PostMapping("/reservationStock/releaseBatch1")
    public R<Boolean> releaseBatch1()
    {
        reservationStockTask.releaseBatch1();
        return R.ok(true);
    }

    @InnerAuth
    @PostMapping("/reservationStock/releaseBatch2")
    public R<Boolean> releaseBatch2()
    {
        reservationStockTask.releaseBatch2();
        return R.ok(true);
    }

    @InnerAuth
    @PostMapping("/reservationStock/updateExpireStock")
    public R<Boolean> updateExpireStock()
    {
        reservationStockTask.updateExpireStock();
        return R.ok(true);
    }


    @InnerAuth
    @PostMapping("/reservationOrder/batch/update/unverify/expire")
    public R<Boolean> batchUpdateUnVerifyOrderToExpire() {
        reservationOrderService.batchUpdateUnVerifyOrderToExpire();
        return R.ok(true);
    }
}
