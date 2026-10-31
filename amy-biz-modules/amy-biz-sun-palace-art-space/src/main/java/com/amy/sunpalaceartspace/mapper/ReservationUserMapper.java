package com.amy.sunpalaceartspace.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.vo.ReservationUserVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 预约会员 数据层
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface ReservationUserMapper extends BaseMapper<ReservationUser> {

    /**
     * 分页查询预约会员列表。
     *
     * @param page   分页对象（返回时会被填充 total/records）
     * @param entity 查询条件
     * @return 分页结果
     */
    IPage<ReservationUserVO> selectReservationUserList(@Param("page") Page<ReservationUserVO> page,
                                                       @Param("entity") ReservationUser entity);

    /**
     * 不分页查询全部会员（导出用）。
     *
     * @param entity 查询条件
     * @return 会员视图列表
     */
    List<ReservationUserVO> selectReservationUserExportList(@Param("entity") ReservationUser entity);

    /**
     * 统计当前条件下待导出的行数（条件与 selectReservationUserExportList 完全一致）。
     *
     * <p>用于判断导出的量级：行数少时允许不按时间分片导出，行数大时才强制要求限定时间范围。
     * 仅在时间跨度不合规时调用，正常分片导出的场景不会走到这里。</p>
     *
     * @param entity 查询条件
     * @return 命中行数
     */
    long selectReservationUserExportCount(@Param("entity") ReservationUser entity);

    /**
     * 根据ID查询预约会员详情
     *
     * @param reservationUserId 会员ID
     * @return 会员视图对象
     */
    ReservationUserVO selectReservationUserById(Long reservationUserId);
}
