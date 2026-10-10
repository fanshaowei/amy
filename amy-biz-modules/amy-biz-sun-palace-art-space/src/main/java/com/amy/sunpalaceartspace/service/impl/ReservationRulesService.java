package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.utils.DateUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.service.IReservationStockService;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static com.amy.sunpalaceartspace.constant.SpasConstant.RESERVATION_STOCK_KEY_PREFIX;

/**
 * @ClassName ReservationRulesService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 16:19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationRulesService {
    private final IProjectsService projectsService;
    private final RedisService redisService;
    private final IReservationStockService reservationStockService;
    private final ReservationUtil reservationUtil;

    /**
     * 免费开放时间为AM9:00-12:00，30分钟为一个时间段，全天共6个时段，
     * 因空间接纳有限及安全考虑，每个时段参观名额为20名，全天共计120名。
     *
     * 游客可对两日内时段免费进行预约，每天中午12:00系统将自动释放第二天预约名额60名，晚上24点将释放剩余预约名额60名，请提前做好预约安排并及时预约
     *
     * @param currentDate
     */
    public void createReservationStock(Date currentDate) {
        List<Projects> projects = projectsService.getBaseMapper().selectList(null);
        projects.forEach(project -> {
            createProjectReservationStock(project, currentDate);
        });
    }

    private void createProjectReservationStock(Projects project, Date currentDate) {
        String reservationStartTime = project.getReservationStartTime();
        String reservationEndTime = project.getReservationEndTime();
        Integer reservationIntervalSecond = project.getReservationIntervalSecond();
        Integer advanceReservationDays = project.getAdvanceReservationDays();
        Integer reservationCount = project.getReservationCount();

        // 提取预约时间段
        List<String> reservationTimes = reservationUtil.extractReservationTimes(reservationStartTime, reservationEndTime, reservationIntervalSecond);

        // 设置可提前预约的库存
        for (int i = 0; i < advanceReservationDays; i++) {
            String reservationDate = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, DateUtils.addDays(currentDate, i+1));

            reservationTimes.forEach(reservationTime -> {
                //创建库存记录
                String reservationTimeStr = reservationTime.split(":")[0] + reservationTime.split(":")[1];
                ReservationStock reservationStock = new ReservationStock();
                reservationStock.setProjectId(project.getProjectId());
                reservationStock.setReservationTime(reservationDate + reservationTimeStr);
                reservationStock.setReservationCount(reservationCount);
                boolean success = reservationStockService.saveReservationStock(reservationStock);

                //设置redis库存
                if(success) {
                    log.info("成功创建库存:{} {}", reservationStock.getReservationTime(), reservationCount);
                    redisService.setCacheObject(
                            String.format(RESERVATION_STOCK_KEY_PREFIX, project.getProjectId(), reservationStock.getReservationTime()),
                            reservationCount
                    );
                }
            });
        }
    }
}
