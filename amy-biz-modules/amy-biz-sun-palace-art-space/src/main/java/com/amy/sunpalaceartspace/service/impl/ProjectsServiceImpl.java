package com.amy.sunpalaceartspace.service.impl;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.mapper.ProjectsMapper;
import com.amy.sunpalaceartspace.service.IProjectsService;

/**
 * 项目管理 服务层实现（MyBatis-Plus 模式）
 *
 * @author amy
 */
@Service
public class ProjectsServiceImpl extends ServiceImpl<ProjectsMapper, Projects> implements IProjectsService
{
    @Override
    public IPage<Projects> selectProjectsList(Page<Projects> page, Projects projects)
    {
        return baseMapper.selectProjectsList(page, projects);
    }

    @Override
    public List<Projects> selectProjectsExportList(Projects projects)
    {
        return baseMapper.selectProjectsExportList(projects);
    }

    @Override
    public boolean saveProjects(Projects projects)
    {
        if (projects.getStatus() == null)
        {
            projects.setStatus("0");
        }
        projects.setCreateTime(DateUtils.getNowDate());
        return save(projects);
    }

    @Override
    public boolean updateProjects(Projects projects)
    {
        projects.setUpdateTime(DateUtils.getNowDate());
        return updateById(projects);
    }

    @Override
    public boolean removeProjectsByIds(Long[] projectIds)
    {
        return removeByIds(Arrays.asList(projectIds));
    }
}
