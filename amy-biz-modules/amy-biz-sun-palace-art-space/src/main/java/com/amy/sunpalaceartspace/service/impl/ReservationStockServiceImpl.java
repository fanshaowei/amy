package com.amy.sunpalaceartspace.service.impl;

import java.util.Arrays;
import java.util.List;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
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
        if (stock.getStatus() == null) {
            stock.setStatus(Boolean.TRUE);
        }
        return save(stock);
    }

    @Override
    public IPage<ReservationStock> selectReservationStockPage(Page<ReservationStock> page,
            Long projectId, String reservationTime, Boolean status) {
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(projectId != null, ReservationStock::getProjectId, projectId)
                .eq(reservationTime != null, ReservationStock::getReservationTime, reservationTime)
                .eq(status != null, ReservationStock::getStatus, status)
                .orderByAsc(ReservationStock::getProjectId)
                .orderByAsc(ReservationStock::getReservationTime);
        return page(page, wrapper);
    }

    @Override
    public ReservationStock selectByProjectAndTime(Long projectId, String reservationTime) {
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
    public boolean updateStatus(Long reservationStockId, Boolean status) {
        ReservationStock update = new ReservationStock();
        update.setStatus(status);
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
}
