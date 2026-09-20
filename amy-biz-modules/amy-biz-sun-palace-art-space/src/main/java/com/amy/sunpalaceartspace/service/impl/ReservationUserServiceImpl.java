package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.enums.UserStatus;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationUserVO;
import com.amy.sunpalaceartspace.mapper.ReservationUserMapper;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预约会员 服务层实现（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Service
public class ReservationUserServiceImpl extends ServiceImpl<ReservationUserMapper, ReservationUser>
        implements IReservationUserService {

    @Override
    public IPage<ReservationUserVO> selectReservationUserList(Page<ReservationUserVO> page, ReservationUser entity) {
        return page.setRecords(baseMapper.selectReservationUserList(entity));
    }

    @Override
    public List<ReservationUserVO> selectReservationUserExportList(ReservationUser entity) {
        return baseMapper.selectReservationUserList(entity);
    }

    @Override
    public ReservationUserVO selectReservationUserById(Long reservationUserId) {
        return baseMapper.selectReservationUserById(reservationUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReservationUser(ReservationUserReq req) {
        ReservationUser user = convertToEntity(req);
        user.setStatus(Integer.parseInt(UserStatus.OK.getCode()));

        if (user.getNumber() == null || user.getNumber().trim().isEmpty()) {
            user.setNumber(generateUserNumber());
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setCreateTime(DateUtils.getNowDate());
        return save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateReservationUser(ReservationUserReq req) {
        ReservationUser user = convertToEntity(req);
        user.setUpdateBy(SecurityUtils.getUsername());
        user.setUpdateTime(DateUtils.getNowDate());
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeUserType(Long reservationUserId, Integer type) {
        ReservationUser user = new ReservationUser();
        user.setReservationUserId(reservationUserId);
        user.setType(type);
        user.setUpdateBy(SecurityUtils.getUsername());
        user.setUpdateTime(DateUtils.getNowDate());
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeReservationUserByIds(Long[] reservationUserIds) {
        return removeByIds(Arrays.asList(reservationUserIds));
    }

    @Override
    public boolean checkUserExistById(Long reservationUserId) {
        LambdaQueryWrapper<ReservationUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationUser::getReservationUserId, reservationUserId);
        return this.baseMapper.exists(queryWrapper);
    }

    /**
     * 请求对象转实体（Controller 只负责入参接收，业务转换放 Service）
     */
    private ReservationUser convertToEntity(ReservationUserReq req) {
        ReservationUser user = new ReservationUser();
        user.setReservationUserId(req.getReservationUserId());
        user.setNumber(req.getNumber());
        user.setNickname(req.getNickname());
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setAvatarImgUrl(req.getAvatarImgUrl());
        user.setOpenId(req.getOpenId());
        user.setIdNum(req.getIdNum());
        user.setToken(req.getToken());
        if(req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        user.setType(req.getType());
        user.setIsVerified(req.getIsVerified());
        return user;
    }

    /**
     * 生成会员编号：9 位随机数字（如 467078662）
     */
    private String generateUserNumber()
    {
        return String.format("%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
    }
}
