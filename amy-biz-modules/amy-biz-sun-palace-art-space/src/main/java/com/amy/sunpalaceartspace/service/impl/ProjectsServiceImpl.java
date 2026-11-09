package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.utils.DateUtils;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.mapper.ProjectReservationConfigMapper;
import com.amy.sunpalaceartspace.mapper.ProjectsMapper;
import com.amy.sunpalaceartspace.enums.ProjectStatus;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.utils.DateRangeParamUtils;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 项目管理 服务层实现（MyBatis-Plus 模式）
 *
 * <p>新增项目时会自动按项目的开放时段生成一套【默认全部可预约】的时段配置
 * （见 {@link #initReservationTimeConfig(Projects)}），便于后台直接逐条改为不可预约。</p>
 *
 * @author amy
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProjectsServiceImpl extends ServiceImpl<ProjectsMapper, Projects> implements IProjectsService
{
    /**
     * 时段配置写入（直接注入 Mapper，<b>不要改成注入 IProjectReservationConfigService</b>）：
     * ProjectReservationConfigServiceImpl → ReservationRulesService → IProjectsService(本类)，
     * 构造器注入会形成循环依赖，启动直接 BeanCurrentlyInCreationException。
     */
    private final ProjectReservationConfigMapper projectReservationConfigMapper;
    private final ReservationUtil reservationUtil;

    @Override
    public IPage<Projects> selectProjectsList(Page<Projects> page, Projects projects) {
        DateRangeParamUtils.normalize(projects);
        return baseMapper.selectProjectsList(page, projects);
    }

    @Override
    public List<Projects> selectProjectsExportList(Projects projects) {
        DateRangeParamUtils.normalize(projects);
        return baseMapper.selectProjectsExportList(projects);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveProjects(Projects projects) {
        if (projects.getStatus() == null)
        {
            projects.setStatus(ProjectStatus.NORMAL.getCode());
        }
        projects.setCreateTime(DateUtils.getNowDate());
        boolean ok = save(projects);
        if (ok)
        {
            // projectId 由 MP 自增主键回填，此时已可用
            initReservationTimeConfig(projects);
        }
        return ok;
    }

    /**
     * 新增项目后，按项目的开放时段生成一套默认配置，每个时段一条、全部「可预约」。
     *
     * <p>配置<b>不区分日期</b>，对该项目每一天都生效；
     * 每段为左闭右开 {@code [本段起点, 下一段起点)}，最后一段的结束时间取项目的 reservationEndTime。</p>
     *
     * <p>幂等：该项目若已存在配置则直接跳过，不覆盖人工调整过的结果。<br>
     * 失败不影响项目本身入库（这里只在事务内记日志，由调用方决定是否回滚）。</p>
     *
     * @param project 已入库且带 projectId 的项目
     */
    private void initReservationTimeConfig(Projects project) {
        Long projectId = project.getProjectId();
        String startTime = project.getReservationStartTime();
        String endTime = project.getReservationEndTime();
        Integer interval = project.getReservationIntervalSecond();
        if (projectId == null) {
            return;
        }
        if (startTime == null || endTime == null || interval == null || interval <= 0) {
            log.warn("项目缺少预约时段参数，跳过时段配置初始化: projectId:{}, startTime:{}, endTime:{}, interval:{}",
                    projectId, startTime, endTime, interval);
            return;
        }

        List<String> times;
        try {
            times = reservationUtil.extractReservationTimes(startTime, endTime, interval);
        }
        catch (Exception e) {
            log.error("提取项目预约时段失败，跳过配置初始化: projectId:{}", projectId, e);
            return;
        }
        if (times.isEmpty()) {
            return;
        }

        Date now = DateUtils.getNowDate();
        String createBy = project.getCreateBy() == null ? "system" : project.getCreateBy();
        List<ProjectReservationConfig> configs = new ArrayList<>(times.size());
        for (int i = 0; i < times.size(); i++) {
            String slotStart = times.get(i);
            // 最后一段没有「下一段」，用项目结束时间收尾
            String slotEnd = (i + 1 < times.size()) ? times.get(i + 1) : endTime;
            // HH:mm:ss 定长零填充，字典序即时间序；end <= start 会撞表的 CHECK 约束，直接跳过
            if (slotEnd.compareTo(slotStart) <= 0) {
                continue;
            }
            ProjectReservationConfig config = new ProjectReservationConfig();
            config.setProjectId(projectId);
            config.setStartTime(slotStart);
            config.setEndTime(slotEnd);
            config.setReservationEnabled(Boolean.TRUE);
            config.setCreateBy(createBy);
            config.setCreateTime(now);
            configs.add(config);
        }
        if (configs.isEmpty()) {
            return;
        }
        // 一次网络往返写完（单条多值 insert）；insertBatch 要求入参非空，上面已保证
        projectReservationConfigMapper.insertBatch(configs);
        log.info("项目时段配置初始化完成: projectId:{}, 时段数:{}", projectId, configs.size());
    }

    @Transactional
    @Override
    public boolean updateProjects(Projects projects) {
        Projects record = this.selectProjectById(projects.getProjectId());
        String oldReservationStartTime = record.getReservationStartTime();
        String oldReservationEndTime = record.getReservationEndTime();
        Integer oldReservationIntervalSecond = record.getReservationIntervalSecond();

        projects.setUpdateBy(SecurityUtils.getUsername());
        projects.setUpdateTime(DateUtils.getNowDate());
        boolean b = updateById(projects);

        if(b) {
            // 用值比较（Objects.equals）判断时段参数是否真的变了：
            // 原代码用 `!=` 做 String 引用比较，内容相同但引用不同的字符串会误判为「已修改」，
            // 在硬删除场景下会误删全部配置，必须改成值比较。
            boolean timeOrIntervalChanged =
                    !Objects.equals(oldReservationStartTime, projects.getReservationStartTime())
                            || !Objects.equals(oldReservationEndTime, projects.getReservationEndTime())
                            || !Objects.equals(oldReservationIntervalSecond, projects.getReservationIntervalSecond());
            if (timeOrIntervalChanged) {
                log.warn("项目预约时段参数已修改，将硬删除原有配置并按新参数重建: projectId:{}, startTime:{}, endTime:{}, interval:{}",
                        projects.getProjectId(), projects.getReservationStartTime(), projects.getReservationEndTime(),
                        projects.getReservationIntervalSecond());
                int count = projectReservationConfigMapper.hardDeleteByProjectId(projects.getProjectId());
                log.info("硬删除原有项目预约配置记录条数: {}", count);
                initReservationTimeConfig(projects);
            }
        }

        return b;
    }

    @Override
    public boolean removeProjectsByIds(Long[] projectIds) {
        return removeByIds(Arrays.asList(projectIds));
    }

    @Override
    public Projects selectProjectById(Long projectId) {
        LambdaQueryWrapper<Projects> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Projects::getProjectId, projectId);
        return this.baseMapper.selectOne(lambdaQueryWrapper);
    }

}
