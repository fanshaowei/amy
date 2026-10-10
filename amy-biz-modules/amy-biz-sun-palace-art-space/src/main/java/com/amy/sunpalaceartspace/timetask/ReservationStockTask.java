package com.amy.sunpalaceartspace.timetask;

import com.amy.sunpalaceartspace.service.impl.ReservationRulesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName ReservationStockTask
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 16:17
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ReservationStockTask {
    private final ReservationRulesService reservationRulesService;
    private final RedissonClient redissonClient;
    private static final String LOCK = "reservation-stock-lock";

    @Scheduled(cron = "${reservation.stock.cron}")
    public void resetReservationStock() {
        RLock lock = redissonClient.getLock(LOCK);
        try {
            // 尝试加锁，最多等待 3 秒，上锁以后 10 秒钟自动解锁
            boolean isLocked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (isLocked) {
                // 执行业务逻辑
                log.info("加锁成功，执行业务...");
                reservationRulesService.createReservationStock(new Date());
                log.info("完成执行业务...");
            }
        } catch (InterruptedException e) {
            log.error("库存创建任务执行失败,{}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // 确认当前线程持有锁再进行解锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放锁成功");
            }
        }
    }
}
