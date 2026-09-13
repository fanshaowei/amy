package com.amy.sunpalaceartspace.mapper;

import com.amy.sunpalaceartspace.domain.entity.UserIdentity;
import com.amy.sunpalaceartspace.domain.vo.UserIdentityVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户身份信息 Mapper
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentity> {

    /**
     * 分页列表（支持 name/idType/idNum/gender/phone 条件）
     *
     * @param userIdentity 查询条件
     * @return 列表
     */
    List<UserIdentityVO> selectUserIdentityList(UserIdentity userIdentity);

    /**
     * 详情
     *
     * @param userIdentityId 记录ID
     * @return 详情
     */
    UserIdentityVO selectUserIdentityById(Long userIdentityId);
}
