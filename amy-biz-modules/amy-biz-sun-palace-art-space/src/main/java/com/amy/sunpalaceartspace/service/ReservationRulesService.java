package com.amy.sunpalaceartspace.service;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.amy.sunpalaceartspace.enums.ReservationStockStatus;
import com.amy.sunpalaceartspace.mapper.ProjectReservationConfigMapper;
import com.amy.sunpalaceartspace.utils.ProjectReservationConfigChecker;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static com.amy.sunpalaceartspace.constant.SpasConstant.RESERVATION_RELEASE_BATCH_STEP;
import static com.amy.sunpalaceartspace.constant.SpasConstant.RESERVATION_STOCK_KEY_PREFIX;

/**
 * 预约放号（库存）规则
 *
 * <p>放号规则：每天 6 时段 × 每时段 20 = 120 名；分两批释放，每批每时段 +10（共 60）。
 * 批次1（12:00）释放“第二天”的第一批,各时段各10名共约60；批次2（00:00）释放“当天”的剩余名额（补足至满）。</p>
 *
 * <p>幂等：基于 batch1Released / batch2Released 标记，定时器重跑或集群多节点重复触发均不会重复放号。</p>
 *
 * @author fantasyfan
 * @Date 2026-09-27 16:19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationRulesService {
    private final IProjectsService projectsService;
    private final IReservationStockService reservationStockService;
    private final ReservationUtil reservationUtil;
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 时段配置查询（直接注入 Mapper，避免与 ProjectReservationConfigServiceImpl 形成构造器循环依赖）
     */
    private final ProjectReservationConfigMapper projectReservationConfigMapper;

    /** 库存 reservation_time 里的日期部分格式 */
    private static final DateTimeFormatter STOCK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 从库存 reservation_time（yyyyMMddHHmm）还原时段起点 LocalTime */
    private static LocalTime parseSlotFromStockTime(String reservationTime) {
        if (reservationTime == null || reservationTime.length() < 12) {
            return null;
        }
        String hhmm = reservationTime.substring(8, 12);
        return ProjectReservationConfigChecker.parseTime(
                hhmm.substring(0, 2) + ":" + hhmm.substring(2, 4) + ":00");
    }

    /**
     * 分批放号核心逻辑：在每个项目、目标日的每个时段上，按批次累加“已放号”并同步 Redis 可用库存。
     *
     * @param date 定时器触发的日期
     * @param batch      1=12:00 第一批（每时段 +10）；2=00:00 第二批（补足至满）
     */
    public void releaseStock(Date date, int batch) {
        List<Projects> projects = projectsService.getBaseMapper().selectList(null);
        for (Projects project : projects) {
            /** 如果项目设置的提前几天可预约，那就得提前放几天的号 **/
            Integer advanceReservationDays = project.getAdvanceReservationDays();
            for(int i = 0; i < advanceReservationDays; i++) {
                Date targetDate = date;
                if(batch == 1) {
                    targetDate = DateUtils.addDays(targetDate, i + 1);
                } else {
                    targetDate = DateUtils.addDays(targetDate, i);
                }

                String targetDateStr = DateUtils.parseDateToStr("yyyyMMdd", targetDate);
                release(batch, project, targetDateStr);
            }
        }
    }

    private void release(Integer batch, Projects project, String targetDateStr) {
        // 项目全部时段配置（不区分日期，每天通用；一次载入，避免逐时段查库）
        List<ProjectReservationConfig> configs = projectReservationConfigMapper
                .selectByProjectId(project.getProjectId());

        List<String> times = reservationUtil.extractReservationTimes(
                project.getReservationStartTime(), project.getReservationEndTime(), project.getReservationIntervalSecond());
        for (String time : times) {
            String reservationTimeStr = time.split(":")[0] + time.split(":")[1];
            String fullReservationDateTime = targetDateStr + reservationTimeStr;
            String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, project.getProjectId(), fullReservationDateTime);

            // 1. 确保库存行存在（total = 项目配置的每时段名额）
            ReservationStock stock = reservationStockService.selectByProjectAndTime(project.getProjectId(), fullReservationDateTime);
            if(stock != null) {
                if(1 == batch && stock.getBatch1Released()) continue;
                if(2 == batch && stock.getBatch2Released()) continue;
                if(null != stock.getStatus() && ReservationStockStatus.MANUAL_CLOSED.getCode().equals(stock.getStatus())) {
                    continue;
                }
            }

            log.info("开始处理放号流程,projectId:{}, datetime: {}", project.getProjectId(), fullReservationDateTime);
            // 被 projectReservationConfig 配置为「不可预约」的时段：库存表保留一条 status=2 的记录
            // （便于追溯/对账），但不放号、不写入 Redis 库存缓存。
            LocalTime slot = ProjectReservationConfigChecker.parseTime(time);
            if (!ProjectReservationConfigChecker.isOpen(configs, slot)) {
                markSlotClosed(project.getProjectId(), fullReservationDateTime, project.getReservationCount(), stockKey);
                continue;
            }

            if (stock == null) {
                stock = new ReservationStock();
                stock.setProjectId(project.getProjectId());
                stock.setReservationTime(fullReservationDateTime);
                stock.setReservationCount(project.getReservationCount());
                stock.setReleaseCount(0);
                stock.setReservedCount(0);
                stock.setBatch1Released(false);
                stock.setBatch2Released(false);
                stock.setStatus(ReservationStockStatus.AVAILABLE.getCode());
                stock.setCreateTime(new Date());
                stock.setCreateBy("system");
                reservationStockService.saveReservationStock(stock);
            }

            // 2. 按批次计算本次放号增量（已放过的批次直接跳过，保证幂等）
            int total = stock.getReservationCount() == null ? 0 : stock.getReservationCount();
            int released = stock.getReleaseCount() == null ? 0 : stock.getReleaseCount();
            int delta;
            if (batch == 1) {
                delta = Math.min(RESERVATION_RELEASE_BATCH_STEP, total - released);
                stock.setBatch1Released(true);
                stock.setUpdateTime(new Date());
                stock.setUpdateBy("system");
            } else if (batch == 2) {
                delta = total - released; // 补足至满
                stock.setBatch2Released(true);
                stock.setUpdateTime(new Date());
                stock.setUpdateBy("system");
            } else {
                continue;
            }
            if (delta <= 0) {
                reservationStockService.updateById(stock);
                continue;
            }

            // 3. 持久化已放号数与批次标记
            stock.setReleaseCount(released + delta);
            reservationStockService.updateById(stock);

            // 4. 同步 Redis 可用库存（可用 = 已放号 - 已预约；INCRBY 与并发 DECR 可交换，不会错乱）
            stringRedisTemplate.opsForValue().increment(stockKey, delta);
            log.info("放号成功 projectId:{} time:{} batch:{} delta:{}", project.getProjectId(), fullReservationDateTime, batch, delta);
        }
    }

    /**
     * 时段被 projectReservationConfig 配置为「不可预约」时：确保库存表存在一条 status=2 的记录，
     * 且该时段不进入 Redis 库存缓存。
     *
     * <p>与「直接跳过不建行」相比，保留状态行可以回答「这一天这个时段为什么约不了」，也便于对账；
     * 而是否可约最终由 Redis 缓存决定——这里显式 {@code delete} 掉该 key，覆盖「该时段曾经开放过、
     * 缓存里还残留余量」的流转场景，避免配置改了却仍能抢到号。</p>
     *
     * <p>幂等：已有 status=2 的行不会重复更新；已有非 2 的行才翻转为 2。放号由分布式锁保护，
     * 不会与其它批次并发。</p>
     */
    private void markSlotClosed(Long projectId, String fullReservationDateTime,
            Integer reservationCount, String stockKey) {
        ReservationStock stock = reservationStockService.selectByProjectAndTime(projectId, fullReservationDateTime);
        if (stock == null) {
            stock = new ReservationStock();
            stock.setProjectId(projectId);
            stock.setReservationTime(fullReservationDateTime);
            stock.setReservationCount(reservationCount);
            stock.setReleaseCount(0);
            stock.setReservedCount(0);
            stock.setBatch1Released(false);
            stock.setBatch2Released(false);
            stock.setStatus(ReservationStockStatus.MANUAL_CLOSED.getCode());
            stock.setCreateTime(new Date());
            stock.setCreateBy("system");
            reservationStockService.saveReservationStock(stock);
        } else if (!ReservationStockStatus.MANUAL_CLOSED.getCode().equals(stock.getStatus())) {
            stock.setStatus(ReservationStockStatus.MANUAL_CLOSED.getCode());
            stock.setUpdateTime(new Date());
            stock.setUpdateBy("system");
            reservationStockService.updateById(stock);
        }
        // status=2（以及 0）不加入库存缓存；清除可能残留的旧 key，使抢号端读到「key 不存在」
        stringRedisTemplate.delete(stockKey);
        log.info("放号跳过（不可预约，库存标记 status=2 且不入缓存）: projectId:{} reservationTime:{}",
                projectId, fullReservationDateTime);
    }

    /**
     * 从 MySQL 重建 Redis 可用库存（可用 = 已放号 - 已预约），用于启动恢复与每日对账，修复崩溃/漂移。
     *
     * <p>只把「正常可预约(1)」的库存写入缓存；已过可预约时间(0)、手动设置不可预约(2) 的时段
     * 一律不入缓存，并清除可能残留的旧 key，使 {@code reserve.lua} 对它们返回 -1（尚未放号/不可预约）。
     * 这样缓存集合与状态机严格对齐，重启后也不会把已关闭时段的号放出去。</p>
     */
    public void reconcileStockToRedis() {
        List<ReservationStock> availableStocks = reservationStockService.selectAllByStatus(ReservationStockStatus.AVAILABLE);
        for (ReservationStock stock : availableStocks) {
            int released = stock.getReleaseCount() == null ? 0 : stock.getReleaseCount();
            int reserved = stock.getReservedCount() == null ? 0 : stock.getReservedCount();
            int available = released - reserved;
            String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, stock.getProjectId(), stock.getReservationTime());
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(available));
            log.info("项目启动初始化库存到redis: projectId:{} reservationTime:{} available:{}",
                    stock.getProjectId(), stock.getReservationTime(), available);
        }
        // 非「正常可预约」的时段清掉残留缓存，避免抢号端读到过期余量
        List<ReservationStock> excludedStocks = reservationStockService.selectAllExcludeStatus(ReservationStockStatus.AVAILABLE);
        for (ReservationStock stock : excludedStocks) {
            String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, stock.getProjectId(), stock.getReservationTime());
            stringRedisTemplate.delete(stockKey);
        }
        log.info("reconcileStockToRedis done, available:{} excluded:{}", availableStocks.size(), excludedStocks.size());
    }

    /**
     * 启动补偿：重建 Redis 后，对“今天”补足到满（批次2），若已过中午则对“明天”放第一批。
     * 保证服务刚启动时当日即可预约。
     */
    public void initStockOnStartup() {
        reconcileStockToRedis();
       /* releaseStock(new Date(), 2);
        if (LocalTime.now().getHour() >= 12) {
            releaseStock(new Date(), 1);
        }*/
    }
}
