package com.amy.job.task;

import com.amy.common.core.constant.SecurityConstants;
import com.amy.spas.api.RemoteReservationStockService;
import org.springframework.stereotype.Service;

/**
 * 太阳宫艺术空间 相关业务定时触发。
 *
 * <p>由 amy-job 的 Quartz 定时任务调用，通过 Feign 内部接口触发 spas 的预约库存放号，
 * 传入 {@link SecurityConstants#INNER} 以通过 spas 侧 {@code @InnerAuth} 校验。</p>
 *
 * @ClassName AmyBizSunPlaceArtSpaceService
 * @Author fantasyfan
 * @Date 2026-10-24 17:39
 */
@Service
public class AmyBizSunPlaceArtSpaceTask {

    private final RemoteReservationStockService remoteReservationStockService;

    public AmyBizSunPlaceArtSpaceTask(RemoteReservationStockService remoteReservationStockService) {
        this.remoteReservationStockService = remoteReservationStockService;
    }

    /**
     * 释放第二天第一批名额（对应 spas 原 12:00 批次）。
     */
    public void releaseBatch1() {
        remoteReservationStockService.releaseBatch1(SecurityConstants.INNER);
    }

    /**
     * 释放当天剩余名额（对应 spas 原 00:00 批次）。
     */
    public void releaseBatch2() {
        remoteReservationStockService.releaseBatch2(SecurityConstants.INNER);
    }
}
