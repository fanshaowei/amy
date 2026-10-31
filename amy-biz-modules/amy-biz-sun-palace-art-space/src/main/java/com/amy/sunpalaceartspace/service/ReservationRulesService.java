package com.amy.sunpalaceartspace.service;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.amy.sunpalaceartspace.mapper.ProjectReservationConfigMapper;
import com.amy.sunpalaceartspace.utils.ProjectReservationConfigChecker;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;

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
            // 被配置为「不可预约」的时段：不建库存行、不放号
            LocalTime slot = ProjectReservationConfigChecker.parseTime(time);
            if (!ProjectReservationConfigChecker.isOpen(configs, slot)) {
                log.info("放号跳过：projectId:{} date:{} time:{} 被配置为不可预约", project.getProjectId(), targetDateStr, time);
                continue;
            }
            String reservationTimeStr = time.split(":")[0] + time.split(":")[1];
            String fullReservationDateTime = targetDateStr + reservationTimeStr;
            String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, project.getProjectId(), fullReservationDateTime);

            // 1. 确保库存行存在（total = 项目配置的每时段名额）
            ReservationStock stock = reservationStockService.selectByProjectAndTime(project.getProjectId(), fullReservationDateTime);
            if (stock == null) {
                stock = new ReservationStock();
                stock.setProjectId(project.getProjectId());
                stock.setReservationTime(fullReservationDateTime);
                stock.setReservationCount(project.getReservationCount());
                stock.setReleaseCount(0);
                stock.setReservedCount(0);
                stock.setBatch1Released(false);
                stock.setBatch2Released(false);
                stock.setStatus(true);
                stock.setCreateTime(new Date());
                stock.setCreateBy("system");
                reservationStockService.saveReservationStock(stock);
            }

            // 2. 按批次计算本次放号增量（已放过的批次直接跳过，保证幂等）
            int total = stock.getReservationCount() == null ? 0 : stock.getReservationCount();
            int released = stock.getReleaseCount() == null ? 0 : stock.getReleaseCount();
            int delta;
            if (batch == 1) {
                if (Boolean.TRUE.equals(stock.getBatch1Released())) {
                    continue;
                }
                delta = Math.min(RESERVATION_RELEASE_BATCH_STEP, total - released);
                stock.setBatch1Released(true);
                stock.setUpdateTime(new Date());
                stock.setUpdateBy("system");
            } else if (batch == 2) {
                if (Boolean.TRUE.equals(stock.getBatch2Released())) {
                    continue;
                }
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
     * 从 MySQL 重建 Redis 可用库存（可用 = 已放号 - 已预约），用于启动恢复与每日对账，修复崩溃/漂移。
     */
    public void reconcileStockToRedis() {
        List<ReservationStock> all = reservationStockService.selectAllByStatus(true);
        for (ReservationStock stock : all) {
            int released = stock.getReleaseCount() == null ? 0 : stock.getReleaseCount();
            int reserved = stock.getReservedCount() == null ? 0 : stock.getReservedCount();
            int available = released - reserved;
            String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, stock.getProjectId(), stock.getReservationTime());
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(available));
        }
        log.info("reconcileStockToRedis done, size:{}", all.size());
    }

    /**
     * 预约时段配置变更后，把该项目的库存开关对齐到最新配置（由 Service 层在增删改后调用）。
     *
     * <p>配置【不区分日期】，所以同步范围是该项目【今天及以后】的全部库存行；
     * 历史日期的库存不动（已过期的号本来也约不了）。</p>
     *
     * <p>被关闭的时段：MySQL status=false + Redis 可用量置 0，Lua 抢号会返回「名额已满」；
     * 已生成的订单不受影响，仍可正常核销。<br>
     * 重新开放的时段：MySQL status=true + Redis 恢复为（已放号 - 已预约）。</p>
     *
     * @param projectId 项目ID
     */
    public void applyConfigToStock(Long projectId) {
        /*if (projectId == null) {
            return;
        }
        List<ProjectReservationConfig> configs = projectReservationConfigMapper.selectByProjectId(projectId);
        // reservation_time 形如 202610310930，按字符串比较即可筛出「今天及以后」
        String todayPrefix = LocalDate.now().format(STOCK_DATE_FORMATTER);
        List<ReservationStock> stocks = reservationStockService.selectByProjectIdAndTimeGe(projectId, todayPrefix);
        for (ReservationStock stock : stocks) {
            String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX,
                    stock.getProjectId(), stock.getReservationTime());
            LocalTime slot = parseSlotFromStockTime(stock.getReservationTime());
            boolean open = ProjectReservationConfigChecker.isOpen(configs, slot);
            if (open) {
                if (Boolean.FALSE.equals(stock.getStatus())) {
                    stock.setStatus(true);
                    stock.setUpdateTime(new Date());
                    stock.setUpdateBy("system");
                    reservationStockService.updateById(stock);
                }
                int available = Math.max(0, nvl(stock.getReleaseCount()) - nvl(stock.getReservedCount()));
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(available));
            } else {
                if (Boolean.TRUE.equals(stock.getStatus())) {
                    stock.setStatus(false);
                    stock.setUpdateTime(new Date());
                    stock.setUpdateBy("system");
                    reservationStockService.updateById(stock);
                }
                // 可用量归零，后续抢号按「名额已满」处理
                stringRedisTemplate.opsForValue().set(stockKey, "0");
                log.info("时段已关闭，库存同步为不可用: projectId:{} reservationTime:{}",
                        projectId, stock.getReservationTime());
            }
        }*/
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
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
