package com.amy.template;

import java.util.Arrays;
import com.amy.template.domain.entity.MpPocDemo;
import com.amy.template.mapper.MpPocDemoMapper;
import com.amy.template.service.IMpPocDemoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus POC 运行时探针（仅 mp-poc profile 激活时运行）。
 * 复用与 @SpringBootTest 相同的 MP Bean，断言：通用 CRUD / IPage 分页 / @TableLogic 逻辑删除 / @DataScope 切面注入的数据权限。
 * 验证完成后以 System.exit 结束进程（仅用于一次性冒烟，不进入正常启动流程）。
 *
 * 运行方式：java -jar __APP_ARTIFACT__.jar --spring.profiles.active=mp-poc
 */
@Profile("mp-poc")
@Component
public class MpPocProbe implements ApplicationRunner
{
    @Autowired
    private IMpPocDemoService service;

    @Autowired
    private MpPocDemoMapper mapper;

    @Override
    public void run(ApplicationArguments args)
    {
        int code = 0;
        try
        {
            // 清理历史 POC 数据
            mapper.delete(new QueryWrapper<MpPocDemo>().like("demo_name", "MP_POC_"));

            // 1) MP 通用 save + 自增主键回填
            MpPocDemo a = new MpPocDemo();
            a.setDemoName("MP_POC_A");
            a.setStatus(0);
            a.setDeptId(100L);
            Assert.that(service.save(a), "MP save 应成功");
            Assert.that(a.getDemoId() != null, "MP 应回填自增主键");
            MpPocDemo loaded = service.getById(a.getDemoId());
            Assert.that(loaded != null && "MP_POC_A".equals(loaded.getDemoName()), "getById 应返回刚保存的记录");

            // 2) IPage 分页
            MpPocDemo b = new MpPocDemo();
            b.setDemoName("MP_POC_B");
            b.setDeptId(200L);
            service.save(b);
            MpPocDemo c = new MpPocDemo();
            c.setDemoName("MP_POC_C");
            c.setDeptId(100L);
            service.save(c);

            Page<MpPocDemo> page = new Page<>(1, 10);
            IPage<MpPocDemo> iPage = service.selectMpPocDemoList(page, new MpPocDemo());
            Assert.that(iPage.getTotal() == 3, "分页总数应为 3，实际=" + iPage.getTotal());
            Assert.that(iPage.getRecords().size() == 3, "分页记录数应为 3");

            // 3) @TableLogic 逻辑删除：removeByIds 后 getById 不可见、列表不计入
            Assert.that(service.removeByIds(Arrays.asList(a.getDemoId())), "removeByIds 应成功");
            Assert.that(service.getById(a.getDemoId()) == null, "@TableLogic 应使逻辑删除记录对 getById 不可见");
            Page<MpPocDemo> page2 = new Page<>(1, 10);
            IPage<MpPocDemo> iPage2 = service.selectMpPocDemoList(page2, new MpPocDemo());
            Assert.that(iPage2.getTotal() == 2, "逻辑删除后可见总数应为 2，实际=" + iPage2.getTotal());

            // 4) @DataScope 切面：模拟切面注入的 SQL 片段（运行时由切面按登录用户生成）
            MpPocDemo scopeQuery = new MpPocDemo();
            scopeQuery.getParams().put("dataScope", " AND (d.dept_id = 100)");
            Page<MpPocDemo> page3 = new Page<>(1, 10);
            IPage<MpPocDemo> iPage3 = service.selectMpPocDemoList(page3, scopeQuery);
            Assert.that(iPage3.getTotal() == 1, "数据权限应仅返回 dept_id=100 的记录（MP_POC_C），实际=" + iPage3.getTotal());
            Assert.that("MP_POC_C".equals(iPage3.getRecords().get(0).getDemoName()), "数据权限应仅返回 MP_POC_C");

            // 清理
            mapper.delete(new QueryWrapper<MpPocDemo>().like("demo_name", "MP_POC_"));
            System.out.println("[MP POC] PASS: 通用 CRUD / IPage 分页 / @TableLogic 逻辑删除 / @DataScope 数据权限 均正常");
        }
        catch (AssertionError e)
        {
            code = 1;
            System.out.println("[MP POC] FAIL: " + e.getMessage());
        }
        catch (Exception e)
        {
            code = 1;
            System.out.println("[MP POC] ERROR: " + e);
        }
        System.exit(code);
    }

    private static final class Assert
    {
        static void that(boolean cond, String msg)
        {
            if (!cond)
            {
                throw new AssertionError(msg);
            }
        }
    }
}
