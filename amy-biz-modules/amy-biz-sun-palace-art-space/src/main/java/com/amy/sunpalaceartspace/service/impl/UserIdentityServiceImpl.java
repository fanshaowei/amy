package com.amy.sunpalaceartspace.service.impl;

import java.util.Arrays;
import java.util.List;

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
        List<UserIdentityVO> rows = baseMapper.selectUserIdentityList(userIdentity);
        return page.setRecords(rows);
    }

    @Override
    public List<UserIdentityVO> selectUserIdentityExportList(UserIdentity userIdentity) {
        return baseMapper.selectUserIdentityList(userIdentity);
    }

    @Override
    public UserIdentityVO selectUserIdentityById(Long userIdentityId) {
        return baseMapper.selectUserIdentityById(userIdentityId);
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

    /**
     * 请求对象转实体（Controller 只负责入参接收，业务转换放 Service）
     */
    private UserIdentity convertToEntity(UserIdentityReq req) {
        UserIdentity entity = new UserIdentity();
        entity.setUserIdentityId(req.getUserIdentityId());
        entity.setName(req.getName());
        entity.setIdType(req.getIdType());
        entity.setIdNum(req.getIdNum());
        entity.setGender(req.getGender());
        entity.setPhone(req.getPhone());
        return entity;
    }
}
