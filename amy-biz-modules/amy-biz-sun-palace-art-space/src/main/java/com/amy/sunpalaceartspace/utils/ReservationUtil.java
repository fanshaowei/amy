package com.amy.sunpalaceartspace.utils;

import com.amy.common.core.utils.DateUtils;
import com.amy.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.amy.sunpalaceartspace.constant.SpasConstant.RESERVATION_ORDER_NUM_SEQ;

/**
 * @ClassName ReservationUtil
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 22:20
 */
@RequiredArgsConstructor
@Component
public class ReservationUtil {
    private final RedisService redisService;

    /**
     * 生成预约单号
     * <p>规则：预约时间 yyyyMMddHHmmssSSS + 4 位随机数；若预约时间为空则取当前时间。</p>
     */
    public String generateReservationNum(Date reservationTime)
    {
        Date baseTime = reservationTime != null ? reservationTime : DateUtils.getNowDate();
        String timePart = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(baseTime);

        String key = String.format(RESERVATION_ORDER_NUM_SEQ, DateUtils.parseDateToStr("yyyyMMdd", baseTime));
        Long increment = redisService.increment(key);
        String sequencePart = String.format("%06d", increment);

        return timePart + sequencePart;
    }

    /**
     * 提取预约时间
     * <p>规则：从 startTime 开始，每隔 interval 秒，直到 endTime 结束。</p>
     */
    public List<String> extractReservationTimes(String startTime, String endTime, Integer interval){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtils.HH_MM_SS);
        LocalTime start = LocalTime.parse(startTime, formatter);
        LocalTime end = LocalTime.parse(endTime, formatter);
        int loop = 0;
        List<String> result = new ArrayList<>();
        while (start.isBefore(end)) {
            result.add(start.format(formatter));
            start = start.plusMinutes(interval / 60);
            loop++;
            if(loop > 6) break;
        }

        return result;
    }

    /**
     *
     * @param dateInterval
     * @return
     */
    public List<String> extractReservationDatesFromCurrent(Integer dateInterval) {
        List<String> dates = new LinkedList<>();

        Date dateStart = DateUtils.addDays(DateUtils.getNowDate(), 1);
        Date dateEnd = DateUtils.addDays(DateUtils.getNowDate(), dateInterval);

        while(!dateStart.after(dateEnd)) {
            dates.add(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, dateStart));
            dateStart = DateUtils.addDays(dateStart, 1);
        }

        return dates;
    }
}
