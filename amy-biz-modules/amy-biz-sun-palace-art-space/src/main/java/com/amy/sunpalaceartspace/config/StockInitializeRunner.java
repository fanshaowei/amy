package com.amy.sunpalaceartspace.config;

import com.amy.sunpalaceartspace.service.ReservationRulesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动补偿：服务启动时重建 Redis 可用库存，并对“今天”补足到满、对“明天”在过午时放第一批，
 * 保证服务刚启动当日即可预约，且 Redis 与 MySQL 库存一致。
 *
 * @author fantasyfan
 * @Date 2026-10-10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class StockInitializeRunner implements ApplicationRunner {
    private final ReservationRulesService reservationRulesService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            reservationRulesService.initStockOnStartup();
            log.info("预约库存启动初始化完成");
        } catch (Exception e) {
            log.error("预约库存启动初始化失败", e);
        }
    }
}
