package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationStock;
import com.amy.sunpalaceartspace.enums.ReservationStockStatus;
import com.amy.sunpalaceartspace.mapper.ReservationStockMapper;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.service.IReservationStockService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 预约库存 服务层实现（MyBatis-Plus 模式，单表 CRUD）
 *
 * @author fantasyfan
 * @date 2026-09-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationStockServiceImpl extends ServiceImpl<ReservationStockMapper, ReservationStock>
        implements IReservationStockService {
    private final IProjectsService projectsService;

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
    public ReservationStock selectByProjectAndTime(Long projectId, Date reservationTime) {
        LambdaQueryWrapper<ReservationStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationStock::getProjectId, projectId)
                .eq(ReservationStock::getReservationTime, reservationTime)
                .last("LIMIT 1");
        return getOne(wrapper);
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
    public boolean incrementReservedCount(Long projectId, Date reservationTime) {
        return baseMapper.incrementReservedCount(projectId, reservationTime, DateUtils.getNowDate()) > 0;
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

    @Override
    public int setReservationStockExpireByProjectCutOffTime(){
        int update = this.baseMapper.updateReservationStockExpireByProjectCutOffTime();
        log.info("stock expire update count: {}", update);
        return update;
    }
}
