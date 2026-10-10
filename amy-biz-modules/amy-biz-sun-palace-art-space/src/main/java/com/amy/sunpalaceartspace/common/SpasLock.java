package com.amy.sunpalaceartspace.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName SpasLock
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-10 19:37
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SpasLock {
    private final RedissonClient redissonClient;

    private Long WAIT_TIME = 3L;
    private Long LEASE_TIME = 15L;

    public void run(Runnable runnable, String lockName, String jobName) {
        RLock lock = redissonClient.getLock(lockName);
        try {
            // 尝试加锁，最多等待 3 秒，上锁以后 15 秒钟自动解锁
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (isLocked) {
                log.info("Get the lock: {}, start to execute jobName: {}", lockName, jobName);
                runnable.run();
                log.info("Finish to execute the jobName: {}", jobName);
            }
        } catch (InterruptedException e) {
            log.error("Fail to execute the jobName: {}", jobName, e);
            Thread.currentThread().interrupt();
        } finally {
            // 确认当前线程持有锁再进行解锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Release lock: {} successfully", lockName);
            }
        }
    }
}
