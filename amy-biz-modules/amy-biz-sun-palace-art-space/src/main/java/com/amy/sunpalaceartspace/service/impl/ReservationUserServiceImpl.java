package com.amy.sunpalaceartspace.service.impl;

import com.amy.sunpalaceartspace.enums.ReservationUserStatusEnum;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.core.utils.StringUtils;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationUserVO;
import com.amy.sunpalaceartspace.enums.ReservationUserStatusEnum;
import com.amy.sunpalaceartspace.mapper.ReservationUserMapper;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.amy.sunpalaceartspace.utils.DateRangeParamUtils;
import com.amy.sunpalaceartspace.utils.FileUrlNormalizer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.server.ServerCloneException;
import java.util.Arrays;
import java.util.List;

/**
 * 预约会员 服务层实现（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@AllArgsConstructor
@Service
public class ReservationUserServiceImpl extends ServiceImpl<ReservationUserMapper, ReservationUser>
        implements IReservationUserService {

    @Override
    public IPage<ReservationUserVO> selectReservationUserList(Page<ReservationUserVO> page, ReservationUser entity) {
        // 统一时间参数格式：列表请求传 yyyy-MM-dd，导出请求传 ISO-8601（...Z），
        // 不归一化会出现「列表查得到、导出为空」。详见 DateRangeParamUtils 类注释。
        DateRangeParamUtils.normalize(entity);
        return baseMapper.selectReservationUserList(page, entity);
    }

    @Override
    public List<ReservationUserVO> selectReservationUserExportList(ReservationUser entity) {
        DateRangeParamUtils.normalize(entity);
        return baseMapper.selectReservationUserExportList(entity);
    }

    @Override
    public long countReservationUserExport(ReservationUser entity) {
        DateRangeParamUtils.normalize(entity);
        return baseMapper.selectReservationUserExportCount(entity);
    }

    @Override
    public ReservationUserVO selectReservationUserById(Long reservationUserId) {
        return baseMapper.selectReservationUserById(reservationUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReservationUser(ReservationUserReq req) {
        ReservationUser user = convertToEntity(req);
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

    @Override
    public boolean checkReservationUserByOpenId(String openId) {
        LambdaQueryWrapper<ReservationUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationUser::getOpenId, openId);
        return this.baseMapper.exists(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updatePhoneByOpenId(String openId, String phone) {
        LambdaQueryWrapper<ReservationUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationUser::getOpenId, openId);
        List<ReservationUser> reservationUsers = this.baseMapper.selectList(queryWrapper);
        if (null == reservationUsers || reservationUsers.isEmpty()) {
            throw new ServiceException("user not found by openId: " + openId);
        }
        if(reservationUsers.size() > 1) {
            throw new ServiceException("multiple users found by openId: " + openId);
        }
        ReservationUser reservationUser = reservationUsers.getFirst();
        LambdaUpdateWrapper<ReservationUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ReservationUser::getReservationUserId, reservationUser.getReservationUserId());
        updateWrapper.set(ReservationUser::getPhone, phone);
        return this.update(updateWrapper);
    }

    @Override
    public ReservationUser selectUserInfoByOpenId(String openId) {
        LambdaQueryWrapper<ReservationUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationUser::getOpenId, openId);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean updateVerifyStatus(Long reservationUserId, Boolean verifyStatus) {
        LambdaUpdateWrapper<ReservationUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ReservationUser::getReservationUserId, reservationUserId)
                .set(ReservationUser::getIsVerified, verifyStatus)
                .set(ReservationUser::getUpdateTime, DateUtils.getNowDate())
                .set(ReservationUser::getUpdateBy, SecurityUtils.getUsername());
        return this.update(updateWrapper);
    }

    /**
     * 请求对象转实体（Controller 只负责入参接收，业务转换放 Service）
     */
    private ReservationUser convertToEntity(ReservationUserReq req) {
        ReservationUser user = new ReservationUser();
        BeanUtils.copyProperties(req, user);
        // 后台新增/编辑会员、首登建会员都走这里：统一只把资源 key 写进 avatar_img_url。
        // 注意「编辑」场景可能根本不提交头像字段，此时必须保持 null，
        // 让 MyBatis-Plus 的 updateById 跳过该列；强行 set 空串会把已有头像清空。
        if (StringUtils.hasText(req.getAvatarImgUrl())) {
            String key = FileUrlNormalizer.normalize(req.getAvatarImgUrl());
            // 归一化结果为空说明是 wxfile:// 等临时路径：拒绝写入，保持库里的原值
            user.setAvatarImgUrl(StringUtils.hasText(key) ? key : null);
        }
        return user;
    }

}
