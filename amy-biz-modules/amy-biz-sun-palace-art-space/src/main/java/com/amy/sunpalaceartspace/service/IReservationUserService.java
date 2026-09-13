package com.amy.sunpalaceartspace.service;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.vo.ReservationUserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 预约会员 服务层
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface IReservationUserService extends IService<ReservationUser> {

    /**
     * 查询预约会员列表（分页）
     *
     * @param page   分页参数
     * @param entity 查询条件
     * @return 分页结果
     */
    IPage<ReservationUserVO> selectReservationUserList(Page<ReservationUserVO> page, ReservationUser entity);

    /**
     * 查询预约会员导出列表
     *
     * @param entity 查询条件
     * @return 视图列表
     */
    List<ReservationUserVO> selectReservationUserExportList(ReservationUser entity);

    /**
     * 根据ID查询预约会员详情
     *
     * @param reservationUserId 会员ID
     * @return 视图对象
     */
    ReservationUserVO selectReservationUserById(Long reservationUserId);

    /**
     * 新增预约会员
     *
     * @param req 会员入参
     * @return 是否成功
     */
    boolean saveReservationUser(ReservationUserReq req);

    /**
     * 修改预约会员
     *
     * @param req 会员入参
     * @return 是否成功
     */
    boolean updateReservationUser(ReservationUserReq req);

    /**
     * 批量删除预约会员（软删除）
     *
     * @param reservationUserIds 会员ID集合
     * @return 是否成功
     */
    boolean removeReservationUserByIds(Long[] reservationUserIds);
}
