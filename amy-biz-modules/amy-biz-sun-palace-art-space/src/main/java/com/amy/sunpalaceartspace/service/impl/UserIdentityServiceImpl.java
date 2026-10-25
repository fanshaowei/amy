package com.amy.sunpalaceartspace.service.impl;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amy.common.core.utils.DateUtils;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.UserIdentity;
import com.amy.sunpalaceartspace.domain.req.UserIdentityReq;
import com.amy.sunpalaceartspace.domain.vo.UserIdentityVO;
import com.amy.sunpalaceartspace.mapper.UserIdentityMapper;
import com.amy.sunpalaceartspace.service.IUserIdentityService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

/**
 * 用户身份信息 服务层实现（MyBatis-Plus 模式）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Service
public class UserIdentityServiceImpl extends ServiceImpl<UserIdentityMapper, UserIdentity>
        implements IUserIdentityService {

    @Override
    public IPage<UserIdentityVO> selectUserIdentityList(Page<UserIdentityVO> page, UserIdentity userIdentity) {
        return baseMapper.selectUserIdentityList(page, userIdentity);
    }

    @Override
    public List<UserIdentityVO> selectUserIdentityExportList(UserIdentity userIdentity) {
        return baseMapper.selectUserIdentityExportList(userIdentity);
    }

    @Override
    public List<UserIdentityVO> selectUserIdentityById(Long userIdentityId, List<Boolean> isFollower) {
        return baseMapper.selectUserIdentityById(userIdentityId, isFollower);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUserIdentity(UserIdentityReq req) {
        UserIdentity entity = convertToEntity(req);
        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(DateUtils.getNowDate());
        return save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserIdentity(UserIdentityReq req) {
        UserIdentity entity = convertToEntity(req);
        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(DateUtils.getNowDate());
        return updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserIdentityByIds(Long[] userIdentityIds) {
        return removeByIds(Arrays.asList(userIdentityIds));
    }

    @Override
    public void updateOrSaveUserIdentityAfterRealVerify(UserIdentityReq req) {
        LambdaQueryWrapper<UserIdentity> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(UserIdentity::getReservationUserId, req.getReservationUserId())
                .eq(UserIdentity::getIdNum, req.getIdNum())
                .eq(UserIdentity::getIsFollower, req.getIsFollower());
        UserIdentity userIdentity = this.baseMapper.selectOne(queryWrapper);
        if(userIdentity == null) {
            this.saveUserIdentity(req);
        } else {
            req.setUserIdentityId(userIdentity.getUserIdentityId());
            this.updateUserIdentity(req);
        }
    }

    @Override
    public boolean countByIdNum(String idNum) {
        LambdaQueryWrapper<UserIdentity> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(UserIdentity::getIdNum, idNum);
        return this.baseMapper.exists(queryWrapper);
    }

    /**
     * 请求对象转实体（Controller 只负责入参接收，业务转换放 Service）
     */
    private UserIdentity convertToEntity(UserIdentityReq req) {
        UserIdentity entity = new UserIdentity();
        entity.setUserIdentityId(req.getUserIdentityId());
        entity.setReservationUserId(req.getReservationUserId());
        entity.setName(req.getName());
        entity.setIdType(req.getIdType().getType());
        entity.setIdNum(req.getIdNum());
        entity.setGender(req.getGender());
        entity.setPhone(req.getPhone());
        entity.setIsFollower(req.getIsFollower() != null ? req.getIsFollower() : Boolean.FALSE);
        return entity;
    }
}
