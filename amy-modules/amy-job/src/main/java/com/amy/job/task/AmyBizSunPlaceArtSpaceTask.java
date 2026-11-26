package com.amy.job.task;

import com.amy.common.core.constant.SecurityConstants;
import com.amy.common.core.domain.R;
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
        requireSuccess(remoteReservationStockService.releaseBatch1(SecurityConstants.INNER), "释放第二天第一批名额");
    }

    /**
     * 释放当天剩余名额（对应 spas 原 00:00 批次）。
     */
    public void releaseBatch2() {
        requireSuccess(remoteReservationStockService.releaseBatch2(SecurityConstants.INNER), "释放当天剩余名额");
    }

    /**
     * 更新库存过期
     */
    public void updateStockExpire() {
        requireSuccess(remoteReservationStockService.updateExpireStock(SecurityConstants.INNER), "更新过期预约库存");
    }

    /**
     * 批量将「待核销」状态的预约订单更新为「已过期」状态。
     */
    public void batchUpdateUnVerifyOrderToExpire() {
        requireSuccess(remoteReservationStockService.batchUpdateUnVerifyOrderToExpire(SecurityConstants.INNER),
                "批量更新未核销订单为已过期");
    }

    /**
     * Feign 只在 HTTP 非 2xx 时抛异常；服务端返回 {@code R.fail(...)} 时 HTTP 仍是 200，
     * 不做校验会让 Quartz 把业务失败误记成成功。这里统一转成异常，
     * 由 AbstractQuartzJob 写入 sys_job_log，便于在后台看到失败原因。
     */
    private static void requireSuccess(R<Boolean> result, String taskName) {
        if (result == null || R.isError(result) || !Boolean.TRUE.equals(result.getData())) {
            String reason = result == null ? "无响应" : String.valueOf(result.getMsg());
            throw new IllegalStateException(taskName + "执行失败：" + reason);
        }
    }

    public void makeUpReservationOrderFromMq(){
        requireSuccess(remoteReservationStockService.makeUpReservationOrderFromMq(SecurityConstants.INNER), "补偿处理预约订单");
    }
}
