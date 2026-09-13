package com.amy.sunpalaceartspace.service.impl;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amy.common.core.utils.DateUtils;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.amy.sunpalaceartspace.mapper.ReservationOrderMapper;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

/**
 * 预约订单 服务层实现（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Service
public class ReservationOrderServiceImpl extends ServiceImpl<ReservationOrderMapper, ReservationOrder>
        implements IReservationOrderService {

    @Override
    public IPage<ReservationOrderVO> selectReservationOrderList(Page<ReservationOrderVO> page, ReservationOrder reservationOrder)
    {
        List<ReservationOrderVO> rows = baseMapper.selectReservationOrderList(reservationOrder);
        rows.forEach(ReservationOrderVO::fillReservationStatusName);
        return page.setRecords(rows);
    }

    @Override
    public List<ReservationOrderVO> selectReservationOrderExportList(ReservationOrder reservationOrder)
    {
        List<ReservationOrderVO> list = baseMapper.selectReservationOrderList(reservationOrder);
        list.forEach(ReservationOrderVO::fillReservationStatusName);
        return list;
    }

    @Override
    public ReservationOrderVO selectReservationOrderById(Long reservationOrderId)
    {
        ReservationOrderVO vo = baseMapper.selectReservationOrderById(reservationOrderId);
        if (vo != null) {
            vo.fillReservationStatusName();
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReservationOrder(ReservationOrderReq req)
    {
        ReservationOrder order = convertToEntity(req);
        order.setReservationStatus(ReservationOrderStatus.WAIT_VERIFY.getStatus());
        order.setReservationNum(generateReservationNum(order.getReservationTime()));
        order.setCreateBy(SecurityUtils.getUsername());
        order.setCreateTime(DateUtils.getNowDate());
        return save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateReservationOrder(ReservationOrderReq req)
    {
        ReservationOrder order = convertToEntity(req);
        order.setUpdateBy(SecurityUtils.getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        return updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeReservationOrderByIds(Long[] reservationOrderIds)
    {
        return removeByIds(Arrays.asList(reservationOrderIds));
    }

    /**
     * 请求对象转实体（Controller 只负责入参接收，业务转换放 Service）
     */
    private ReservationOrder convertToEntity(ReservationOrderReq req)
    {
        ReservationOrder order = new ReservationOrder();
        order.setReservationOrderId(req.getReservationOrderId());
        order.setProjectId(req.getProjectId());
        order.setReservationUserId(req.getReservationUserId());
        order.setName(req.getName());
        order.setPhone(req.getPhone());
        order.setIdType(req.getIdType());
        order.setIdNum(req.getIdNum());
        order.setGuestsNum(req.getGuestsNum());
        order.setReservationTime(req.getReservationTime());
        if(req.getReservationStatus() != null) {
            order.setReservationStatus(req.getReservationStatus());
        }
        // 随行人身份信息：List<String> 直接透传，由字段注解上的 StringListJsonTypeHandler 自动 JSON 化
        order.setCompUsers(req.getCompUsers());
        return order;
    }

    /**
     * 生成预约单号
     * <p>规则：预约时间 yyyyMMddHHmmss + 4 位随机数；若预约时间为空则取当前时间。</p>
     */
    private String generateReservationNum(Date reservationTime)
    {
        Date baseTime = reservationTime != null ? reservationTime : DateUtils.getNowDate();
        String timePart = new SimpleDateFormat("yyyyMMddHHmmss").format(baseTime);
        String randomPart = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        return timePart + randomPart;
    }
}
