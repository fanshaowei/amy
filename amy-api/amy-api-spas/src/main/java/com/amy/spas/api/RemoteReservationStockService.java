package com.amy.spas.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import com.amy.common.core.constant.SecurityConstants;
import com.amy.common.core.constant.ServiceNameConstants;
import com.amy.common.core.domain.R;

/**
 * 太阳宫艺术空间 预约库存远程服务（仅允许微服务间 Feign 内部调用）
 *
 * <p>调用方需传入 {@link SecurityConstants#FROM_SOURCE} = {@link SecurityConstants#INNER}，
 * 由 spas 侧 {@code @InnerAuth} 切面校验，防止外部直接访问。</p>
 *
 * @author fantasyfan
 */
@FeignClient(contextId = "remoteReservationStockService", value = ServiceNameConstants.SPAS_SERVICE)
public interface RemoteReservationStockService
{
    /**
     * 释放第二天第一批名额（每时段 +10，共 60）。对应 spas 原 12:00 批次。
     */
    @PostMapping("/inner/reservationStock/releaseBatch1")
    R<Boolean> releaseBatch1(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 释放当天剩余名额（补足至满，共 60）。对应 spas 原 00:00 批次。
     */
    @PostMapping("/inner/reservationStock/releaseBatch2")
    R<Boolean> releaseBatch2(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
