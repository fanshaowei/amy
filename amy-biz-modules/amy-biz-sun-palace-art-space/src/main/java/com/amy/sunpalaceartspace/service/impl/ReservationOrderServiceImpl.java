package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.utils.DateUtils;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.amy.sunpalaceartspace.mapper.ReservationOrderMapper;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 预约订单 服务层实现（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@AllArgsConstructor
@Service
public class ReservationOrderServiceImpl extends ServiceImpl<ReservationOrderMapper, ReservationOrder>
        implements IReservationOrderService {

    private final ReservationUtil reservationUtil;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<ReservationOrderVO> selectReservationOrderList(Page<ReservationOrderVO> page, ReservationOrderReq req)
    {
        ReservationOrder order = convertToEntity(req);
        IPage<ReservationOrderVO> result = baseMapper.selectReservationOrderList(page, order);
        result.getRecords().forEach(ReservationOrderVO::fillReservationStatusName);
        return result;
    }

    @Override
    public List<ReservationOrderVO> selectReservationOrderExportList(ReservationOrderReq req)
    {
        ReservationOrder order = convertToEntity(req);
        List<ReservationOrderVO> list = baseMapper.selectReservationOrderExportList(order);
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
    public ReservationOrder selectReservationOrderByNum(String orderNum) {
        LambdaQueryWrapper<ReservationOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationOrder::getReservationNum, orderNum);
        ReservationOrder one = getOne(queryWrapper);
        return one;
    }

    @Override
    public long countUserOrders(Long userId, String idNum, List<Integer> statuses, Date since) {
        // 注：del_flag 由 BaseEntity 的 @TableLogic 自动过滤（仅统计未删除订单），无需手动 eq
        return this.count(
                new LambdaQueryWrapper<ReservationOrder>()
                        .and(w -> w.eq(ReservationOrder::getReservationUserId, userId)
                                .or().eq(ReservationOrder::getIdNum, idNum))
                        .in(ReservationOrder::getReservationStatus, statuses)
                        .ge(ReservationOrder::getCreateTime, since)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean saveReservationOrder(ReservationOrderReq req)
    {
        ReservationOrder order = convertToEntity(req);
        order.setReservationStatus(ReservationOrderStatus.WAIT_VERIFY.getStatus());
        if(Strings.isBlank(req.getReservationNum())) {
            order.setReservationNum(reservationUtil.generateReservationNum(order.getReservationTime()));
        }
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

    @Override
    public List<ReservationOrderStatisticVO> selectReservationOrderStatisticByDate(ReservationOrderStatisticCriteria req)
    {
        return baseMapper.selectReservationOrderStatisticByDate(req);
    }

    @Override
    public List<ReservationOrderStatisticByTimeVO> selectReservationOrderStatisticByTime(ReservationOrderStatisticCriteria criteria)
    {
        return baseMapper.selectReservationOrderStatisticByTime(criteria);
    }

    @Override
    public List<ReservationOrderStatisticByProjectVO> selectReservationOrderStatisticByProject(ReservationOrderStatisticCriteria criteria)
    {
        return baseMapper.selectReservationOrderStatisticByProject(criteria);
    }

    /**
     * 请求对象转实体（Controller 只负责入参接收，业务转换放 Service）
     */
    private ReservationOrder convertToEntity(ReservationOrderReq req)
    {
        ReservationOrder order = new ReservationOrder();
        order.setReservationOrderId(req.getReservationOrderId());
        order.setReservationNum(req.getReservationNum());
        order.setProjectId(req.getProjectId());
        order.setReservationUserId(req.getReservationUserId());
        order.setName(req.getName());
        order.setPhone(req.getPhone());
        order.setIdType(req.getIdType());
        order.setIdNum(req.getIdNum());
        order.setGuestsNum(req.getGuestsNum());
        order.setReservationTime(req.getReservationTime());
        order.setReservationStatus(req.getReservationStatus());

        // 随行人身份信息：序列化为 JSON 字符串后直接存入 comp_users 列（实体字段类型为 String）
        String compUsersJson = null;
        if(req.getCompUsers() != null && !req.getCompUsers().isEmpty()) {
            try {
                compUsersJson = objectMapper.writeValueAsString(req.getCompUsers());
                order.setCompUsers(compUsersJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return order;
    }
}
