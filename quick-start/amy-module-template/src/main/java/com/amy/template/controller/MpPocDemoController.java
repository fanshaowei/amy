package com.amy.template.controller;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.amy.common.core.utils.poi.ExcelUtil;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.domain.AjaxResult;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.datascope.annotation.DataScope;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.redis.service.RedisService;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.common.security.utils.SecurityUtils;
import com.amy.template.domain.entity.MpPocDemo;
import com.amy.template.service.IMpPocDemoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * MP 集成验证 Controller（POC 模板）。
 * <p>
 * 演示新功能纯单表：MP 通用 CRUD 零 XML；分页用 MP IPage；数据权限用 @DataScope 切面（表别名 d）。
 * 额外提供 /redis/ping 演示 amy-common-redis 的 RedisService 用法。
 * </p>
 *
 * @author amy
 */
@RestController
@RequestMapping("/mpdemo")
public class MpPocDemoController extends BaseController
{
    @Autowired
    private IMpPocDemoService mpPocDemoService;

    @Autowired
    private RedisService redisService;

    /**
     * 查询示例列表（MP 分页 + 数据权限）
     */
    @RequiresPermissions("template:mpdemo:list")
    @DataScope(deptAlias = "d")
    @GetMapping("/list")
    public TableDataInfo list(MpPocDemo mpPocDemo,
                              @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                              @RequestParam(value = "pageSize", defaultValue = "10") int pageSize)
    {
        Page<MpPocDemo> page = new Page<>(pageNum, pageSize);
        IPage<MpPocDemo> iPage = mpPocDemoService.selectMpPocDemoList(page, mpPocDemo);
        return new TableDataInfo(iPage.getRecords(), iPage.getTotal());
    }

    /**
     * 导出示例列表
     */
    @RequiresPermissions("template:mpdemo:export")
    @Log(title = "MP示例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MpPocDemo mpPocDemo)
    {
        Page<MpPocDemo> page = new Page<>(1, Integer.MAX_VALUE);
        IPage<MpPocDemo> iPage = mpPocDemoService.selectMpPocDemoList(page, mpPocDemo);
        ExcelUtil<MpPocDemo> util = new ExcelUtil<MpPocDemo>(MpPocDemo.class);
        util.exportExcel(response, iPage.getRecords(), "MP示例数据");
    }

    /**
     * 获取示例详细信息
     */
    @RequiresPermissions("template:mpdemo:query")
    @GetMapping(value = "/{demoId}")
    public AjaxResult getInfo(@PathVariable("demoId") Long demoId)
    {
        return success(mpPocDemoService.getById(demoId));
    }

    /**
     * 新增示例（MP 通用 save，零 XML）
     */
    @RequiresPermissions("template:mpdemo:add")
    @Log(title = "MP示例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MpPocDemo mpPocDemo)
    {
        mpPocDemo.setCreateBy(SecurityUtils.getUsername());
        return toAjax(mpPocDemoService.save(mpPocDemo));
    }

    /**
     * 修改示例（MP 通用 updateById，零 XML）
     */
    @RequiresPermissions("template:mpdemo:edit")
    @Log(title = "MP示例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MpPocDemo mpPocDemo)
    {
        mpPocDemo.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(mpPocDemoService.updateById(mpPocDemo));
    }

    /**
     * 删除示例（@TableLogic 触发逻辑删除 del_flag=2）
     */
    @RequiresPermissions("template:mpdemo:remove")
    @Log(title = "MP示例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{demoIds}")
    public AjaxResult remove(@PathVariable Long[] demoIds)
    {
        return toAjax(mpPocDemoService.removeByIds(Arrays.asList(demoIds)));
    }

    /**
     * Redis 可用性演示（amy-common-redis RedisService）
     */
    @RequiresPermissions("template:mpdemo:list")
    @GetMapping("/redis/ping")
    public AjaxResult redisPing()
    {
        String key = "mp_poc_redis:" + System.currentTimeMillis();
        redisService.setCacheObject(key, "ok", 30L, TimeUnit.SECONDS);
        String value = redisService.getCacheObject(key);
        redisService.deleteObject(key);
        return success("ok".equals(value) ? "REDIS_OK" : "REDIS_MISMATCH");
    }
}
