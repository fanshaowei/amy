package com.amy.sunpalaceartspace.timetask;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.common.SpasLock;
import com.amy.sunpalaceartspace.service.ReservationRulesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.amy.sunpalaceartspace.constant.SpasConstant.SAPS_RELEASE_LOCK;

/**
 * 预约库存放号逻辑（拆为两个批次）。
 *
 * <p>调度入口已迁移到 amy-job（通过 Feign 内部接口 {@code ReservationStockInnerController} 触发），
 * 本类不再自带 {@code @Scheduled}，避免与 job 重复放号导致库存超发。</p>
 *
 * @ClassName ReservationStockTask
 * @Description 12:00 释放第二天第一批（每时段 +10，共 60）；00:00 释放当天剩余名额（补足至满，共 60）
 * @Author fantasyfan
 * @Date 2026-09-27 16:17
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ReservationStockTask {
    private final ReservationRulesService reservationRulesService;
    private final SpasLock spasLock;

    /**
     * 每天 12:00 释放“第二天”第一批名额（每时段 +10，共 60）。
     */
    public void releaseBatch1() {
        spasLock.run(() -> reservationRulesService.releaseStock(DateUtils.addDays(new Date(), 1), 1),
                SAPS_RELEASE_LOCK, "releaseBatch1");
    }

    /**
     * 每天 00:00 释放“当天”剩余名额（每时段补足至满，共 60）。
     */
    public void releaseBatch2() {
        spasLock.run(() -> reservationRulesService.releaseStock(new Date(), 2),
                SAPS_RELEASE_LOCK, "releaseBatch2");
    }
}
