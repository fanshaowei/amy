package com.amy.sunpalaceartspace.mapper;

import java.util.Collection;
import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 项目预约时段配置 数据层（MyBatis-Plus 模式）
 *
 * <p>通用 CRUD（getById / save / updateById / removeByIds 等）由 BaseMapper 提供；
 * 带项目名 join 的列表查询与导出走自定义 XML。</p>
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
@Mapper
public interface ProjectReservationConfigMapper extends BaseMapper<ProjectReservationConfig>
{
    /**
     * 分页查询配置列表（带项目名称）。
     *
     * <p>第一个参数为 MP 分页对象，由 PaginationInnerInterceptor 自动改写 SQL 完成 count + limit；
     * 第二个参数 entity 用于条件拼装。</p>
     *
     * @param page   分页对象（返回时会被填充 total/records）
     * @param config 查询条件
     * @return 分页结果
     */
    IPage<ProjectReservationConfigVO> selectConfigList(@Param("page") Page<ProjectReservationConfigVO> page,
            @Param("entity") ProjectReservationConfig config);

    /**
     * 不分页查询配置列表（导出用）。
     *
     * @param config 查询条件
     * @return 配置集合
     */
    List<ProjectReservationConfigVO> selectConfigExportList(@Param("entity") ProjectReservationConfig config);

    /**
     * 按主键查询单条配置（带项目名称）
     *
     * @param configId 配置ID
     * @return 配置对象，不存在返回 null
     */
    ProjectReservationConfigVO selectConfigVOById(@Param("configId") Long configId);

    /**
     * 查询某个项目的全部时段配置（用于一次性载入后判定开关，避免逐时段查库）。
     *
     * @param projectId 项目ID
     * @return 该项目的配置列表（按开始时间升序）
     */
    List<ProjectReservationConfig> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 按多个项目ID一次性查询时段配置（带项目名称，一次网络往返）。
     *
     * @param projectIds 项目ID集合（建议先去重）
     * @return 配置列表
     */
    List<ProjectReservationConfigVO> selectByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    /**
     * 批量插入时段配置（单条多值 insert，一次网络往返）。
     *
     * @param list 待插入的配置（projectId / startTime / endTime / reservationEnabled 必填）
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<ProjectReservationConfig> list);

    /**
     * 物理清除全部已软删（del_flag='1'）的历史行。
     * @return 清除的行数
     */
    int purgeDeleted();

    /**
     * @param projectId 项目ID
     * @return 删除行数
     */
    int hardDeleteByProjectId(@Param("projectId") Long projectId);
}
