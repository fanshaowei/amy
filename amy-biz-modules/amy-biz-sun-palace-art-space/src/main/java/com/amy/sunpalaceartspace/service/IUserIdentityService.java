package com.amy.sunpalaceartspace.service;

import com.amy.sunpalaceartspace.domain.entity.UserIdentity;
import com.amy.sunpalaceartspace.domain.req.UserIdentityReq;
import com.amy.sunpalaceartspace.domain.vo.UserIdentityVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 用户身份信息 Service
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface IUserIdentityService {

    /**
     * 分页列表
     */
    IPage<UserIdentityVO> selectUserIdentityList(Page<UserIdentityVO> page, UserIdentity userIdentity);

    /**
     * 导出列表
     */
    List<UserIdentityVO> selectUserIdentityExportList(UserIdentity userIdentity);

    /**
     * 详情
     */
    UserIdentityVO selectUserIdentityById(Long userIdentityId);

    /**
     * 新增
     */
    boolean saveUserIdentity(UserIdentityReq req);

    /**
     * 修改
     */
    boolean updateUserIdentity(UserIdentityReq req);

    /**
     * 删除（软删除）
     */
    boolean removeUserIdentityByIds(Long[] userIdentityIds);
}
