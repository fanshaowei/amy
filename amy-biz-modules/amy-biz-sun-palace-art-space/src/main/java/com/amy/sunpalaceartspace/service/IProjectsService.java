package com.amy.sunpalaceartspace.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.amy.sunpalaceartspace.domain.Projects;

/**
 * 项目管理 服务层（MyBatis-Plus 模式）
 * <p>
 * 通用 CRUD 由 IService 继承提供（getById / save / updateById / removeByIds 等）；
 * 仅自定义「带条件的分页列表」与「导出列表」两个方法。
 *
 * @author amy
 */
public interface IProjectsService extends IService<Projects>
{
    /**
     * 分页查询项目列表（模糊搜索 + 时间范围 + 可扩展数据权限 + 排序）
     *
     * @param page     分页对象
     * @param projects 查询条件
     * @return 分页结果
     */
    IPage<Projects> selectProjectsList(Page<Projects> page, Projects projects);

    /**
     * 不分页查询全部项目（导出用）
     *
     * @param projects 查询条件
     * @return 项目集合
     */
    List<Projects> selectProjectsExportList(Projects projects);

    /**
     * 新增项目（自动填充创建时间）
     *
     * @param projects 项目信息
     * @return 是否成功
     */
    boolean saveProjects(Projects projects);

    /**
     * 修改项目（自动填充更新时间）
     *
     * @param projects 项目信息
     * @return 是否成功
     */
    boolean updateProjects(Projects projects);

    /**
     * 批量删除项目（软删除，由 BaseEntity 的 @TableLogic 实现，物理行不会被移除）
     *
     * @param projectIds 项目主键集合
     * @return 是否成功
     */
    boolean removeProjectsByIds(Long[] projectIds);
}
