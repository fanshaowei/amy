package com.amy.sunpalaceartspace.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 项目管理 数据层（MyBatis-Plus 模式）
 * <p>
 * 通用 CRUD（getById / save / updateById / removeByIds 等）由 BaseMapper 提供；
 * 复杂列表查询（模糊搜索 + 时间范围 + 数据权限 + 排序）仍走自定义 XML。
 *
 * @author amy
 */
@Mapper
public interface ProjectsMapper extends BaseMapper<Projects>
{
    /**
     * 分页查询项目列表（含模糊搜索、时间范围、可扩展数据权限）。
     * <p>
     * 第一个参数为 MP 分页对象，由 PaginationInnerInterceptor 自动改写 SQL 完成 count + limit；
     * 第二个参数 entity 用于条件拼装，并预留 ${entity.params.dataScope} 供 @DataScope 切面注入。
     *
     * @param page     分页对象（返回时会被填充 total/records）
     * @param projects 查询条件
     * @return 分页结果
     */
    IPage<Projects> selectProjectsList(@Param("page") Page<Projects> page,
                                       @Param("entity") Projects projects);

    /**
     * 不分页查询全部项目（导出用）。
     *
     * @param projects 查询条件
     * @return 项目集合
     */
    List<Projects> selectProjectsExportList(@Param("entity") Projects projects);
}
