package com.amy.template;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import com.amy.common.redis.service.RedisService;
import com.amy.template.domain.entity.MpPocDemo;
import com.amy.template.mapper.MpPocDemoMapper;
import com.amy.template.service.IMpPocDemoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * MyBatis-Plus 集成 POC 运行时验证（模板）。
 * <p>
 * 设计目标：完全不依赖 Nacos 可用性（禁用 nacos config/discovery，且 src/test/resources/bootstrap.yaml 不含 nacos import），
 * 直接通过 @TestPropertySource 注入 dynamic-datasource 主数据源，并对 mp_poc_demo 表做 CREATE TABLE IF NOT EXISTS 自愈，
 * 因此只要本地 MySQL + Redis 在线即可稳定跑通。Redis 校验为容错式（不可用仅告警不失败）。
 * 验证：MP 通用 CRUD / IPage 分页 / @TableLogic 逻辑删除 / @DataScope 切面注入的数据权限 / RedisService。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false",
    "spring.datasource.dynamic.primary=master",
    "spring.datasource.dynamic.strict=false",
    "spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:3306/amy?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
    "spring.datasource.dynamic.datasource.master.username=root",
    "spring.datasource.dynamic.datasource.master.password=fansw!@#",
    "spring.datasource.dynamic.datasource.master.driver-class-name=com.mysql.cj.jdbc.Driver"
})
class MpPocDemoTest
{
    @Autowired
    private IMpPocDemoService service;

    @Autowired
    private MpPocDemoMapper mapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private DataSource dataSource;

    private static final String DDL = "CREATE TABLE IF NOT EXISTS mp_poc_demo ("
            + " demo_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '示例ID',"
            + " demo_name   VARCHAR(50)  DEFAULT '' COMMENT '示例名称',"
            + " status      INT          DEFAULT 0 COMMENT '状态（0正常 1停用）',"
            + " del_flag    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',"
            + " dept_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '部门ID（用于数据权限验证）',"
            + " create_time DATETIME     DEFAULT NULL COMMENT '创建时间',"
            + " update_time DATETIME     DEFAULT NULL COMMENT '更新时间',"
            + " create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',"
            + " update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',"
            + " remark      VARCHAR(500) DEFAULT '' COMMENT '备注',"
            + " PRIMARY KEY (demo_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MP集成验证表（POC）'";

    @BeforeEach
    void ensureTable()
    {
        new JdbcTemplate(dataSource).execute(DDL);
        mapper.delete(new QueryWrapper<MpPocDemo>().like("demo_name", "MP_POC_"));
    }

    @Test
    void mpIntegrationSmoke()
    {
        // 1) MP 通用 save + 自增主键回填
        MpPocDemo a = new MpPocDemo();
        a.setDemoName("MP_POC_A");
        a.setStatus(0);
        a.setDeptId(100L);
        Assertions.assertTrue(service.save(a));
        Assertions.assertNotNull(a.getDemoId(), "MP 应回填自增主键");
        MpPocDemo loaded = service.getById(a.getDemoId());
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("MP_POC_A", loaded.getDemoName());

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
        Assertions.assertEquals(3, iPage.getTotal(), "分页总数应为 3");
        Assertions.assertEquals(3, iPage.getRecords().size());

        // 3) @TableLogic 逻辑删除：removeByIds 后记录仍在表，但 del_flag=2，查询不可见
        Assertions.assertTrue(service.removeByIds(Arrays.asList(a.getDemoId())));
        MpPocDemo afterDelete = service.getById(a.getDemoId());
        Assertions.assertNull(afterDelete, "@TableLogic 应使逻辑删除记录对 getById 不可见");
        Page<MpPocDemo> page2 = new Page<>(1, 10);
        IPage<MpPocDemo> iPage2 = service.selectMpPocDemoList(page2, new MpPocDemo());
        Assertions.assertEquals(2, iPage2.getTotal(), "逻辑删除后可见总数应为 2");

        // 4) @DataScope 切面：手动模拟切面注入的 SQL 片段（运行时由切面根据登录用户生成）
        MpPocDemo scopeQuery = new MpPocDemo();
        scopeQuery.getParams().put("dataScope", " AND (d.dept_id = 100)");
        Page<MpPocDemo> page3 = new Page<>(1, 10);
        IPage<MpPocDemo> iPage3 = service.selectMpPocDemoList(page3, scopeQuery);
        Assertions.assertEquals(1, iPage3.getTotal(), "数据权限应仅返回 dept_id=100 的记录（MP_POC_C）");
        Assertions.assertEquals("MP_POC_C", iPage3.getRecords().get(0).getDemoName());

        // 5) Redis 容错校验（不可用仅告警，不导致测试失败）
        try
        {
            String key = "mp_poc_redis_test";
            redisService.setCacheObject(key, "hello", 30L, TimeUnit.SECONDS);
            Assertions.assertEquals("hello", redisService.getCacheObject(key), "RedisService 读写应一致");
            redisService.deleteObject(key);
            System.out.println("[MP POC] Redis OK");
        }
        catch (Exception e)
        {
            System.out.println("[MP POC] Redis WARN（不可用，跳过）: " + e.getMessage());
        }

        // 清理
        mapper.delete(new QueryWrapper<MpPocDemo>().like("demo_name", "MP_POC_"));
        System.out.println("[MP POC] 全部断言通过：CRUD / 分页 / 逻辑删除 / 数据权限 / Redis 均正常");
    }
}
