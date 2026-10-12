package com.amy.sunpalaceartspace.mapper;

import com.amy.sunpalaceartspace.domain.entity.UserIdentity;
import com.amy.sunpalaceartspace.domain.vo.UserIdentityVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * 分页查询身份信息列表（支持 reservationUserId/name/idType/idNum/gender/phone 条件）。

     * @param page         分页对象（返回时会被填充 total/records）
     * @param userIdentity 查询条件
     * @return 分页结果
     */
    IPage<UserIdentityVO> selectUserIdentityList(@Param("page") Page<UserIdentityVO> page,
                                                 @Param("entity") UserIdentity userIdentity);

    /**
     * 不分页查询身份信息列表（导出用，与分页方法分开，避免复用导致拿不到 limit）。
     *
     * @param userIdentity 查询条件
     * @return 列表
     */
    List<UserIdentityVO> selectUserIdentityExportList(@Param("entity") UserIdentity userIdentity);

    /**
     * 详情
     *
     * @param userIdentityId 记录ID
     * @return 详情
     */
    UserIdentityVO selectUserIdentityById(Long userIdentityId);
}
