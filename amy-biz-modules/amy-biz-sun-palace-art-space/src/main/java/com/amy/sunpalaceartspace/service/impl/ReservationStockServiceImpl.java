package com.amy.sunpalaceartspace.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.amy.sunpalaceartspace.enums.ReservationStockStatus;
import com.amy.sunpalaceartspace.mapper.ReservationStockMapper;
import com.amy.sunpalaceartspace.service.IReservationStockService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预约库存 服务层实现（MyBatis-Plus 模式，单表 CRUD）
 *
 * @author fantasyfan
 * @date 2026-09-28
 */
@Service
public class ReservationStockServiceImpl extends ServiceImpl<ReservationStockMapper, ReservationStock>
        implements IReservationStockService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReservationStock(ReservationStock stock) {
        // 未显式指定状态时默认「正常可预约」；调用方（如放号逻辑）会显式设置 MANUAL_CLOSED 等值
        if (stock.getStatus() == null) {
            stock.setStatus(ReservationStockStatus.AVAILABLE.getCode());
        }
        return save(stock);
    }

    @Override
    public IPage<ReservationStock> selectReservationStockPage(Page<ReservationStock> page,
            Long projectId, Date reservationTime, ReservationStockStatus status) {
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(projectId != null, ReservationStock::getProjectId, projectId)
                .eq(reservationTime != null, ReservationStock::getReservationTime, reservationTime)
                .eq(status != null, ReservationStock::getStatus, status != null ? status.getCode() : null)
                .orderByAsc(ReservationStock::getProjectId)
                .orderByAsc(ReservationStock::getReservationTime);
        return page(page, wrapper);
    }

    @Override
    public ReservationStock selectByProjectAndTime(Long projectId, Date reservationTime) {
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationStock::getProjectId, projectId)
                .eq(ReservationStock::getReservationTime, reservationTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<ReservationStock> selectByProjectId(Long projectId) {
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationStock::getProjectId, projectId)
                .orderByAsc(ReservationStock::getReservationTime);
        return list(wrapper);
    }

    @Override
    public List<ReservationStock> selectByProjectAndDatePrefix(Long projectId, LocalDate datePrefix) {
        // reservation_time 已为 DATETIME：按「当天 00:00:00 <= t < 次日 00:00:00」范围查询，可命中 idx_reservation_time
        Date start = Date.from(datePrefix.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endExclusive = Date.from(datePrefix.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationStock::getProjectId, projectId)
                .ge(ReservationStock::getReservationTime, start)
                .lt(ReservationStock::getReservationTime, endExclusive)
                .orderByAsc(ReservationStock::getReservationTime);
        return list(wrapper);
    }

    @Override
    public List<ReservationStock> selectByProjectIdAndTimeGe(Long projectId, Date minTime) {
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationStock::getProjectId, projectId)
                .ge(ReservationStock::getReservationTime, minTime)
                .orderByAsc(ReservationStock::getReservationTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductReservationCount(Long reservationStockId, int delta) {
        LambdaUpdateWrapper<ReservationStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservationStock::getReservationStockId, reservationStockId)
                .setSql("reservation_count = reservation_count - " + delta)
                .set(ReservationStock::getUpdateTime, DateUtils.getNowDate());
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementReservationCount(Long reservationStockId, int delta) {
        LambdaUpdateWrapper<ReservationStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservationStock::getReservationStockId, reservationStockId)
                .setSql("reservation_count = reservation_count + " + delta)
                .set(ReservationStock::getUpdateTime, DateUtils.getNowDate());
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementReservedCount(Long projectId, Date reservationTime) {
        LambdaUpdateWrapper<ReservationStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservationStock::getProjectId, projectId)
                .eq(ReservationStock::getReservationTime, reservationTime)
                .setSql("reserved_count = reserved_count + 1")
                .set(ReservationStock::getUpdateTime, DateUtils.getNowDate());
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long reservationStockId, ReservationStockStatus status) {
        ReservationStock update = new ReservationStock();
        update.setStatus(status.getCode());
        update.setUpdateTime(DateUtils.getNowDate());
        LambdaUpdateWrapper<ReservationStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservationStock::getReservationStockId, reservationStockId);
        return update(update, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeReservationStockByIds(Long[] reservationStockIds) {
        return removeByIds(Arrays.asList(reservationStockIds));
    }

    @Override
    public List<ReservationStock> selectAllByStatus(ReservationStockStatus status) {
        LambdaQueryWrapper<ReservationStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationStock::getStatus, status.getCode());
        return list(queryWrapper);
    }

    @Override
    public List<ReservationStock> selectAllExcludeStatus(ReservationStockStatus excludeStatus) {
        LambdaQueryWrapper<ReservationStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(ReservationStock::getStatus, excludeStatus.getCode());
        return list(queryWrapper);
    }
}
