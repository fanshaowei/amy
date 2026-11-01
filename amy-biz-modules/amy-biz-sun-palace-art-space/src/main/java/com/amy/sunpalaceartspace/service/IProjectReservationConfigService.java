package com.amy.sunpalaceartspace.service;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigGroupVO;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 项目预约时段配置 服务层
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
public interface IProjectReservationConfigService extends IService<ProjectReservationConfig>
{
    /**
     * 分页查询配置列表（带项目名称）
     *
     * @param page   分页对象
     * @param config 查询条件
     * @return 分页结果
     */
    IPage<ProjectReservationConfigVO> selectConfigList(Page<ProjectReservationConfigVO> page,
            ProjectReservationConfig config);

    /**
     * 不分页查询配置列表（导出用）
     *
     * @param config 查询条件
     * @return 配置集合
     */
    List<ProjectReservationConfigVO> selectConfigExportList(ProjectReservationConfig config);

    /**
     * 查询单条配置详情（带项目名称）
     *
     * @param configId 配置ID
     * @return 配置对象
     */
    ProjectReservationConfigVO selectConfigById(Long configId);

    /**
     * 新增配置（校验时间区间合法性 + 同项目同开始时间去重）
     *
     * @param config 配置对象
     * @return 是否成功
     */
    boolean saveConfig(ProjectReservationConfig config);

    /**
     * 修改配置（校验同项目同开始时间去重，排除自身）
     *
     * @param config 配置对象
     * @return 是否成功
     */
    boolean updateConfigReservationEnabled(ProjectReservationConfig config);

    /**
     * 批量删除配置（软删除）
     *
     * @param configIds 配置ID数组
     * @return 是否成功
     */
    boolean removeConfigByIds(Long[] configIds);

    /**
     * 查询某个项目的全部时段配置（不区分日期，每天通用）
     *
     * @param projectId 项目ID
     * @return 配置列表
     */
    List<ProjectReservationConfig> selectByProjectId(Long projectId);

    /**
     * 按多个项目ID批量查询时段配置，并按项目分组返回（一次网络往返）
     *
     * @param projectIds 项目ID集合，允许重复
     * @return 按入参顺序排列的分组结果
     */
    List<ProjectReservationConfigGroupVO> selectGroupByProjectIds(Collection<Long> projectIds);

    /**
     * 判定某个项目的指定时段是否开放预约（不区分日期，每天通用）
     *
     * <p>优先级：不可预约(false) &gt; 可预约(true) &gt; 无配置(默认开放)</p>
     *
     * @param projectId 项目ID
     * @param slotStart 时段起点（HH:mm:ss）
     * @return true=可预约
     */
    boolean isTimeSlotOpen(Long projectId, LocalTime slotStart);

    /**
     * 同上，入参为具体的预约时刻（自动取出其中的时段部分）
     *
     * @param projectId       项目ID
     * @param reservationTime 预约时刻
     * @return true=可预约
     */
    boolean isTimeSlotOpen(Long projectId, Date reservationTime);
}
