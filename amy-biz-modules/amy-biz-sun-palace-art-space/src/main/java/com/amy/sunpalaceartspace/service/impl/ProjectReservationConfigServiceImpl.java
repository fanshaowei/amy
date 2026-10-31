package com.amy.sunpalaceartspace.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.ProjectReservationConfig;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigGroupVO;
import com.amy.sunpalaceartspace.domain.vo.ProjectReservationConfigVO;
import com.amy.sunpalaceartspace.mapper.ProjectReservationConfigMapper;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.service.IProjectReservationConfigService;
import com.amy.sunpalaceartspace.service.ReservationRulesService;
import com.amy.sunpalaceartspace.utils.ProjectReservationConfigChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 项目预约时段配置 服务层实现（MyBatis-Plus 模式，列表走自定义 XML）
 *
 * <p>配置不区分日期，对项目每一天都生效；增删改后会触发
 * {@code ReservationRulesService#applyConfigToStock(projectId)} 把该项目【今天及以后】
 * 的库存开关对齐到最新配置。</p>
 *
 * @author fantasyfan
 * @Date 2026-10-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectReservationConfigServiceImpl
        extends ServiceImpl<ProjectReservationConfigMapper, ProjectReservationConfig>
        implements IProjectReservationConfigService
{
    /** 放号规则服务：配置变更后同步该日库存开关 */
    private final ReservationRulesService reservationRulesService;

    /**
     * 项目服务：分组查询时补齐项目名称。
     *
     * <p><b>这里注入 Service 是安全的，不会形成循环依赖</b>：
     * 本类 → ReservationRulesService → IProjectsService，而 IProjectsService(ProjectsServiceImpl)
     * 只注入 Mapper，不反向依赖本类的 Service。红线是：ProjectsServiceImpl 里不要注入
     * IProjectReservationConfigService。</p>
     */
    private final IProjectsService projectsService;

    @Override
    public IPage<ProjectReservationConfigVO> selectConfigList(Page<ProjectReservationConfigVO> page,
            ProjectReservationConfig config) {
        return baseMapper.selectConfigList(page, config);
    }

    @Override
    public List<ProjectReservationConfigVO> selectConfigExportList(ProjectReservationConfig config) {
        return baseMapper.selectConfigExportList(config);
    }

    @Override
    public ProjectReservationConfigVO selectConfigById(Long configId) {
        return baseMapper.selectConfigVOById(configId);
    }

    @Override
    public boolean saveConfig(ProjectReservationConfig config) {
        validateConfig(config);
        if (config.getReservationEnabled() == null) {
            config.setReservationEnabled(Boolean.TRUE);
        }
        config.setCreateTime(DateUtils.getNowDate());
        boolean ok;
        try {
            ok = save(config);
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_project_time (project_id, start_time, end_time, del_flag)
            throw new ServiceException("该项目已存在相同的时段配置");
        }
        if (ok) {
            //syncStock(config);
        }
        return ok;
    }

    @Override
    public boolean updateConfig(ProjectReservationConfig config) {
        if (config.getConfigId() == null) {
            throw new ServiceException("配置ID不能为空");
        }
        ProjectReservationConfig exist = getById(config.getConfigId());
        if (exist == null) {
            throw new ServiceException("配置不存在或已被删除");
        }
        validateConfig(config);
        config.setUpdateTime(DateUtils.getNowDate());
        boolean ok;
        try {
            ok = updateById(config);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("该项目已存在相同的时段配置");
        }
        if (ok) {
            //syncStock(config);
        }
        return ok;
    }

    @Override
    public boolean removeConfigByIds(Long[] configIds) {
        List<ProjectReservationConfig> list = listByIds(Arrays.asList(configIds));
        // 唯一键含 del_flag：先物理清掉历史软删行，避免「删除→重建→再删除」撞 1062
        baseMapper.purgeDeleted();
        boolean ok;
        try {
            ok = removeByIds(Arrays.asList(configIds));
        } catch (DuplicateKeyException e) {
            // 极端并发下才可能触发，重试一次即可
            baseMapper.purgeDeleted();
            ok = removeByIds(Arrays.asList(configIds));
        }
        if (ok && !list.isEmpty()) {
            //list.forEach(this::syncStock);
        }
        return ok;
    }

    @Override
    public List<ProjectReservationConfig> selectByProjectId(Long projectId) {
        return baseMapper.selectByProjectId(projectId);
    }

    @Override
    public List<ProjectReservationConfigGroupVO> selectGroupByProjectIds(Collection<Long> projectIds) {
        List<Long> ids = distinctKeepOrder(projectIds);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 单次 in 查询拿回全部配置，按项目分组（LinkedHashMap 保证项目顺序稳定）
        List<ProjectReservationConfigVO> configs = baseMapper.selectByProjectIds(ids);
        Map<Long, List<ProjectReservationConfigVO>> grouped = configs.stream()
                .collect(Collectors.groupingBy(ProjectReservationConfigVO::getProjectId,
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, String> projectNames = loadProjectNames(ids);

        List<ProjectReservationConfigGroupVO> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            ProjectReservationConfigGroupVO group = new ProjectReservationConfigGroupVO();
            group.setProjectId(id);
            group.setProjectName(projectNames.get(id));
            // 无配置的项目也要返回一条空分组，保证与入参一一对应
            group.setConfigs(grouped.getOrDefault(id, Collections.emptyList()));
            result.add(group);
        }
        return result;
    }

    /**
     * 项目ID去重并保持入参原有顺序（null 元素直接丢弃）
     */
    private List<Long> distinctKeepOrder(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : projectIds) {
            if (id != null) {
                seen.add(id);
            }
        }
        return new ArrayList<>(seen);
    }

    /**
     * 批量取项目名称。
     *
     * <p>项目名统一从项目表取，不从配置行的 join 结果取 —— 后者只能覆盖「有配置且项目未删」的情况，
     * 无配置的项目拿不到名字。MP 的 listByIds 会自动追加逻辑删除条件，已删项目不会返回。</p>
     *
     * <p>取名字失败不影响配置本身，仅降级为 null。</p>
     */
    private Map<Long, String> loadProjectNames(Collection<Long> ids) {
        try {
            return projectsService.listByIds(ids).stream()
                    .collect(Collectors.toMap(Projects::getProjectId, Projects::getProjectName,
                            (a, b) -> a, LinkedHashMap::new));
        } catch (Exception e) {
            log.warn("批量查询项目名称失败, projectIds:{}", ids, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean isTimeSlotOpen(Long projectId, LocalTime slotStart) {
        if (projectId == null || slotStart == null) {
            return true;
        }
        List<ProjectReservationConfig> configs = selectByProjectId(projectId);
        return ProjectReservationConfigChecker.isOpen(configs, slotStart);
    }

    @Override
    public boolean isTimeSlotOpen(Long projectId, Date reservationTime) {
        if (projectId == null || reservationTime == null) {
            return true;
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(reservationTime.toInstant(), ZoneId.systemDefault());
        return isTimeSlotOpen(projectId, dateTime.toLocalTime());
    }

    /**
     * 配置校验：时间格式合法、结束时间须晚于开始时间、同一项目同一开始时间不允许重复。
     *
     * <p>去重口径【比数据库唯一键更严】：库上 uk_project_time 是
     * (project_id, start_time, end_time, del_flag) 四列，
     * 允许「同起点不同终点」两条并存；这里按 start_time 唯一先拦一道，
     * 避免出现 09:00~10:00 关闭 + 09:00~12:00 开放这种自相矛盾的配置。</p>
     */
    private void validateConfig(ProjectReservationConfig config) {
        LocalTime start = ProjectReservationConfigChecker.parseTime(config.getStartTime());
        LocalTime end = ProjectReservationConfigChecker.parseTime(config.getEndTime());
        if (start == null) {
            throw new ServiceException("开始时间格式不正确，应为 HH:mm:ss");
        }
        if (end == null) {
            throw new ServiceException("结束时间格式不正确，应为 HH:mm:ss");
        }
        if (!end.isAfter(start)) {
            throw new ServiceException("结束时间必须晚于开始时间");
        }

        LambdaQueryWrapper<ProjectReservationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectReservationConfig::getProjectId, config.getProjectId())
                .eq(ProjectReservationConfig::getStartTime, config.getStartTime());
        if (config.getConfigId() != null) {
            wrapper.ne(ProjectReservationConfig::getConfigId, config.getConfigId());
        }
        if (count(wrapper) > 0) {
            throw new ServiceException("该项目已存在相同开始时间的时段配置");
        }
    }

    /**
     * 配置变更后同步该项目的库存开关（关闭时段不再可约）。
     *
     * <p>配置不绑定日期，所以同步范围是该项目【今天及以后】的全部库存行，
     * 历史日期的库存不动。失败不影响配置本身的保存结果，仅记日志。</p>
     */
    private void syncStock(ProjectReservationConfig config) {
        if (config == null || config.getProjectId() == null) {
            return;
        }
        try {
            reservationRulesService.applyConfigToStock(config.getProjectId());
        } catch (Exception e) {
            log.error("预约时段配置变更后同步库存失败, projectId:{}", config.getProjectId(), e);
        }
    }
}
