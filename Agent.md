# Agent.md — amy 后端开发技术规范

> 本文件是 amy 后端（Maven 多模块）的**唯一技术规范来源**。
> 所有人在新增模块、新增接口、写 CRUD、改 SQL、写代码生成器模板前，先读本文件。
> AI 编码助手在本仓库中生成后端代码时，必须严格遵循本文件；如与既有代码冲突，以本文件为准并同步修正代码。

---

## 1. 技术栈基线

| 类别 | 选型 | 版本 | 说明 |
|---|---|---|---|
| JDK | Java | 21 | `maven-compiler-plugin` 3.11.0，开启 `-parameters` |
| 构建 | Maven 多模块 | — | 根 `pom.xml`，groupId `com.amy`，版本 `3.6.8` |
| 基础框架 | Spring Boot | 4.0.6 | 父 BOM 由根 pom `dependencyManagement` 导入 |
| 微服务 | Spring Cloud | 2025.1.1 | — |
| 微服务（阿里） | Spring Cloud Alibaba | 2025.1.0.0 | Nacos / Sentinel |
| 注册/配置中心 | Nacos | 3.x（8848） | 配置用 `bootstrap.yml` + `spring.config.import` |
| 网关 | Spring Cloud Gateway | WebFlux | `amy-gateway`（8080） |
| 服务调用 | OpenFeign + OkHttp | — | 通过 `@EnableRyFeignClients` 启用 |
| 熔断限流 | Sentinel | — | — |
| 持久层 | MyBatis（XML 基线）+ MyBatis-Plus 3.5.17（Boot4 构建） | MyBatis 4.0.1 / MP 3.5.17 | 新功能纯单表优先 **MP**（免 Mapper XML）；多表/复杂 SQL/存量维持 **XML** |
| 分页 | PageHelper（XML 查询）/ MP `IPage`（MP 查询） | PageHelper 4.1.0 / MP 3.5.17 | XML 用 `startPage()`+`getDataTable()`；MP 用 `Page`+`IPage`（PaginationInnerInterceptor 已统一配置） |
| 数据源 | Druid + dynamic-datasource | 1.2.28 / 4.5.0 | 主从通过 `@Master` / `@Slave` 切换 |
| 数据库 | MySQL | 8.x | 库名 `amy` |
| 缓存 | Redis + Redisson | — | `RedisService` 封装 |
| 分布式事务 | Seata | — | `amy-common-seata` |
| 文档 | SpringDoc OpenAPI | 3.0.3 | `amy-common-swagger` |
| JSON | Fastjson2 / Jackson | 2.0.62 | 序列化统一 Jackson，Redis 序列化 Fastjson2 |
| 鉴权 | 自建 Token（`amy-common-security`） | — | JWT jjwt 0.9.1 |
| 工具 | Hutool / commons-lang3 / commons-io / POI | — | POI 4.1.2 用于 Excel |
| 模板引擎 | Velocity | 2.3 | 代码生成 `amy-gen` |
| 其他 | kaptcha 验证码、TTL 线程传递 | — | — |

**强制约束**

1. 禁止在子模块 `dependencyManagement` 中 `import` 第三方 BOM（会覆盖 Spring Boot 版本仲裁，详见 §14 已知坑）。
2. 第三方依赖版本统一写在子模块 `<properties>`，并在 `<dependency>` 上显式写 `<version>`。
3. 不使用 JPA；持久层以 **MyBatis XML 为基线**。**新业务模块的纯单表 CRUD 优先用 MyBatis-Plus（MP）**（省去 Mapper XML）；多表关联、复杂 SQL、存量功能维持 XML 模式不动。MP 与 PageHelper **禁止在同一查询中混用**（详见 §5.6）。

---

## 2. 工程与模块结构

```
amy/
├── amy-gateway/            网关（8080）：鉴权、白名单、验证码、限流、XSS
├── amy-auth/               认证中心（18080）：登录/注册/令牌
├── amy-modules/            平台级业务模块
│   ├── amy-system/         系统管理（9201）：用户/角色/菜单/字典/参数/通知
│   ├── amy-gen/            代码生成
│   ├── amy-job/            定时任务
│   └── amy-file/           文件服务（本地/MinIO）
├── amy-biz-modules/        **业务模块归集（新增业务放这里）**
│   └── amy-biz-sun-palace-art-space/
├── amy-api/                对外 Feign 接口契约
│   └── amy-api-system/     `com.amy.system.api`
├── amy-common/             通用能力（无业务）
│   ├── amy-common-core         核心：R、BaseEntity、BaseController、工具类、常量、异常
│   ├── amy-common-security     鉴权：注解、拦截器、TokenService、全局异常处理
│   ├── amy-common-datascope    数据权限切面
│   ├── amy-common-datasource   多数据源
│   ├── amy-common-log          操作日志切面
│   ├── amy-common-redis        缓存服务
│   ├── amy-common-sensitive    数据脱敏
│   ├── amy-common-seata        分布式事务
│   └── amy-common-swagger      SpringDoc 配置
├── amy-visual/             监控（Spring Boot Admin）
└── sql/                    建库建表脚本
```

### 2.1 模块归属原则

| 场景 | 放哪里 |
|---|---|
| 与平台/系统管理相关 | `amy-modules/amy-system` |
| **独立业务域（新业务）** | `amy-biz-modules/amy-biz-<业务名>` |
| 需要被其他服务远程调用的能力 | 在 `amy-api` 建 `amy-api-<业务名>` 模块，只放 `Remote*Service` + `domain` + `factory` |
| 与具体业务无关、可复用 | `amy-common/amy-common-<能力名>` |

### 2.2 新建业务模块步骤

1. 在 `amy-biz-modules/` 下建 `amy-biz-<业务名>`，parent 指向 `amy-biz-modules`。
2. 在 `amy-biz-modules/pom.xml` 的 `<modules>` 中注册。
3. 依赖最小集：`nacos-discovery` + `nacos-config` + `sentinel` + `actuator` + `mysql-connector-j` + `amy-common-core` + `amy-common-datasource` + `amy-common-log` + `amy-common-swagger`（按需加 `redis` / `datascope` / `lombok`）。
4. 启动类：`Amy<BizName>Application`，加 `@SpringBootApplication @EnableCustomConfig @EnableRyFeignClients`。
5. `bootstrap.yml`：端口、应用名、`nacos` 地址、`config.import` 引入 `application-dev.yml` 与 `<appname>-dev.yml`。
6. 在 Nacos 建 `<appname>-dev.yml` 私有配置。

---

## 3. 包结构（模块内部）

统一以 `com.amy.<模块标识>` 为根包（如 `com.amy.system`、`com.amy.sunpalaceartspace`）。

```
com.amy.<module>/
├── controller/          # 仅接收请求、参数校验、返回响应；不写业务
├── service/             # 接口，IXxxService
│   └── impl/            # 实现，XxxServiceImpl
├── mapper/              # MyBatis 接口，XxxMapper
├── domain/              # 与表 1:1 的实体（DO），Xxx
│   ├── vo/              # 出参视图对象，XxxVo
│   ├── bo/              # 业务对象（复杂入参/内部传递），XxxBo
│   └── query/           # 查询条件对象（可选），XxxQuery
├── enums/               # 业务枚举
├── constant/            # 模块内常量
├── exception/           # 模块内业务异常（可选，优先用 ServiceException）
├── config/              # 模块内配置类、@ConfigurationProperties
├── util/                # 模块内工具类（能共用则下沉 common-core）
├── aspect/              # 切面
├── handler/             # 处理器（如文件、回调）
└── <Module>Application.java
resources/
├── mapper/<子目录>/XxxMapper.xml
├── banner.txt
├── logback.xml
└── bootstrap.yml
```

**跨层调用方向（严格单向，禁止逆向依赖）**

```
Controller  →  Service(接口)  →  Mapper  →  DB
     ↓              ↓
    VO/DTO      Domain/BO
```

- Controller **不允许**直接注入 Mapper。
- Service **不允许**注入 Controller、HttpServletRequest（需上下文用 `SecurityUtils`）。
- Mapper **不允许**跨模块直连别库的表；跨服务走 Feign。

---

## 4. 命名规范

### 4.1 通用

| 对象 | 规则 | 示例 |
|---|---|---|
| 包名 | 全小写，单数，无下划线 | `com.amy.system.service.impl` |
| 类名 | UpperCamelCase | `SysConfigController` |
| 方法/变量/参数 | lowerCamelCase | `selectConfigList`、`configId` |
| 常量 | 全大写 + 下划线 | `SYS_CONFIG_KEY` |
| 枚举类名 | 以 `Enum` 结尾或语义名；枚举值全大写 | `BusinessType.INSERT` |
| 泛型 | `E/T/K/V` 单字母或 `XxxDTO` | `R<LoginUser>` |
| 布尔字段 | **不要**加 `is` 前缀（避免序列化坑）；需要 `isXxx` 时加 `@JsonProperty` | `private boolean read; @JsonProperty("isRead")` |
| 数组/集合参数 | 复数形式 | `Long[] configIds`、`List<SysUser> userList` |
| 包级私有方法 | 命名体现意图，禁止 `handle()`、`process()` 这种无语义名 | `buildCacheKey()` |

### 4.2 分层后缀（强制）

| 层 | 命名 | 示例 |
|---|---|---|
| Controller | `<业务>Controller` | `SysConfigController`（**不加 C 后缀、不用复数**） |
| Service 接口 | `I<业务>Service` | `ISysConfigService` |
| Service 实现 | `<业务>ServiceImpl` | `SysConfigServiceImpl` |
| Mapper 接口 | `<实体>Mapper` | `SysConfigMapper` |
| 实体 | 与表对应，`SysXxx` / 业务名驼峰 | `SysConfig`、`Projects` |
| 出参对象 | `<业务>Vo` | `RouterVo`、`MetaVo` |
| 入参对象 | `<业务>Bo` / `<业务>Query` | `LoginBody`、`SysUserQuery` |
| Feign 接口 | `Remote<业务>Service` | `RemoteUserService` |
| Feign 降级 | `Remote<业务>FallbackFactory` | `RemoteUserFallbackFactory` |
| 常量类 | `<域>Constants` | `CacheConstants`、`UserConstants` |
| 异常类 | `<类型>Exception` | `ServiceException`、`UserException` |
| 配置类 | `<功能>Config` / `<功能>Configuration` | `RedisConfig`、`GatewayConfig` |
| 配置属性 | `<功能>Properties` | `XssProperties` |
| 切面 | `<功能>Aspect` | `LogAspect`、`DataScopeAspect` |
| 过滤器/拦截器 | `<功能>Filter` / `<功能>Interceptor` | `AuthFilter`、`HeaderInterceptor` |
| 工具类 | `<域>Utils`（复数，静态） | `StringUtils`、`DateUtils` |
| 枚举 | `<域>Enum` 或语义名 | `BusinessType`、`DesensitizedType` |
| 测试类 | `<类名>Test` | `SysConfigServiceTest` |

### 4.3 方法命名（CRUD 统一动词前缀）

| 语义 | 前缀 | 示例 |
|---|---|---|
| 单条查询 | `select...By<条件>` | `selectConfigById(Long id)` |
| 列表查询 | `select...List` | `selectConfigList(SysConfig config)` |
| 分页查询 | 复用 `select...List`（由 `startPage()` 织入分页） | — |
| 新增 | `insert...` | `insertConfig(SysConfig config)` |
| 修改 | `update...` | `updateConfig(SysConfig config)` |
| 删除 | `delete...By<条件>`，批量加 `s` | `deleteConfigById(Long id)` / `deleteConfigByIds(Long[] ids)` |
| 计数 | `count...` | `countUserByDeptId` |
| 校验唯一 | `check...Unique` | `checkConfigKeyUnique(SysConfig config)` |
| 业务动作 | 动词开头 | `resetConfigCache()`、`loadingConfigCache()` |

- 查询单条返回 null 时，由调用方决定抛异常还是返回空；**禁止**返回 `new Xxx()` 空对象充当 null。
- 批量删除参数统一用 `Long[] ids`（XML 中 `collection="array"`）。

### 4.4 数据库命名

| 对象 | 规则 | 示例 |
|---|---|---|
| 表（平台/系统级） | `小写下划线`，模块前缀 | `sys_config`、`sys_user_role` |
| 表（业务模块） | `biz_<项目缩写>_<业务>`，**项目缩写取自项目英文名首字母** | `biz_spas_projects`、`biz_xxx_orders` |
| 字段 | `小写下划线` | `config_key`、`create_time` |
| 主键 | `<表名单数>_id`，BIGINT 自增 | `config_id` |
| 状态/类型字段 | `status` / `xxx_type`，char(1) 或 varchar | `config_type`（Y/N） |
| 逻辑删除 | `del_flag` char(1)，`0` 存在 `2` 删除 | `del_flag` |
| 通用审计 | `create_by / create_time / update_by / update_time / remark` | 所有业务表必备 |
| 索引 | `idx_<字段缩写>`；唯一 `uk_<字段缩写>` | `idx_config_key`、`uk_config_key` |
| 时间字段 | `datetime`，新增 `sysdate()`，修改 `update_time = sysdate()` | — |

**业务表前缀约定（重要）**

- 同一 `amy` 数据库会被多个项目/业务功能共用。为在库内清晰区分「属于哪个项目」的表，业务模块（`amy-biz-modules/**`）的业务表统一加 **项目缩写前缀**：`biz_<项目缩写>_<业务>`。
- `<项目缩写>` 取项目英文名的**首字母缩写**（避免用全拼，太长）：太阳宫艺术空间 = Sun Palace Art Space → `spas`，故表为 `biz_spas_projects`。
- 后续新增的其它项目功能表，各自用其缩写前缀（如 `biz_xxx_*`），避免与系统表（`sys_/gen_/...`）或其它项目表重名/混淆。
- POC / 数据库冒烟测试用的临时表不受此约束（如模板的 `mp_poc_demo`），但正式业务表一律遵守。
- 用 `amy-module-scaffold` 新建模块时，脚手架会按 `--table-prefix`（缺省取应用名缩写）自动把模板里的 `__TABLE_PREFIX__` 占位符替换掉，业务表即带正确前缀。

---

## 5. 分层编码规范

### 5.1 Controller

```java
@RestController
@RequestMapping("/config")                       // 小写、模块内统一前缀
public class SysConfigController extends BaseController   // 大括号换行（Allman）
{
    @Autowired
    private ISysConfigService configService;

    /** 查询参数配置列表 */
    @RequiresPermissions("system:config:list")     // 权限标识：模块:资源:操作
    @GetMapping("/list")
    public TableDataInfo list(SysConfig config)
    {
        startPage();                                // 必须：PageHelper 分页
        List<SysConfig> list = configService.selectConfigList(config);
        return getDataTable(list);
    }

    /** 根据参数编号获取详细信息 */
    @GetMapping("/{configId}")
    public AjaxResult getInfo(@PathVariable Long configId)
    {
        return success(configService.selectConfigById(configId));
    }

    /** 新增参数配置 */
    @RequiresPermissions("system:config:add")
    @Log(title = "参数管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysConfig config)
    {
        if (!configService.checkConfigKeyUnique(config))
        {
            return error("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        config.setCreateBy(SecurityUtils.getUsername());
        return toAjax(configService.insertConfig(config));
    }

    /** 修改参数配置 */
    @RequiresPermissions("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysConfig config)
    {
        config.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(configService.updateConfig(config));
    }

    /** 删除参数配置 */
    @RequiresPermissions("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds)
    {
        return toAjax(configService.deleteConfigByIds(configIds));
    }
}
```

**硬性规则**

| # | 规则 |
|---|---|
| C1 | 一律 `@RestController` + `@RequestMapping("/小写资源名")`，**不允许**在 `@RestController` 里写路径（错误示例：`@RestController("/wechat")` 无效）。 |
| C2 | 继承 `BaseController`，复用 `startPage() / getDataTable() / success() / error() / toAjax()`。 |
| C3 | URL 与方法严格按 REST 语义：`GET /list` 列表、`GET /{id}` 详情、`POST` 新增、`PUT` 修改、`DELETE /{ids}` 删除、`POST /export` 导出。 |
| C4 | 分页列表返回 `TableDataInfo`；其余统一 `AjaxResult`（跨服务 Feign 用 `R<T>`）。 |
| C5 | `@RequestBody` 前必须有 `@Validated`。 |
| C6 | 写操作必须挂 `@Log(title=..., businessType=...)`；除 `list/详情` 外必须有 `@RequiresPermissions`。 |
| C7 | Controller 内**不写**业务逻辑、不写 SQL、不做 try-catch（交给 `GlobalExceptionHandler`）。 |
| C8 | 审计字段在 Controller 设置：`createBy` / `updateBy` = `SecurityUtils.getUsername()`。 |
| C9 | 导出行：`ExcelUtil<Xxx> util = new ExcelUtil<>(Xxx.class); util.exportExcel(response, list, "业务名数据");`，返回 `void`。 |
| C10 | 每个方法必须写 Javadoc（功能一句话），类头注释含 `@author`。 |

### 5.2 Service 接口

```java
public interface ISysConfigService
{
    SysConfig selectConfigById(Long configId);
    String    selectConfigByKey(String configKey);
    List<SysConfig> selectConfigList(SysConfig config);
    int  insertConfig(SysConfig config);
    int  updateConfig(SysConfig config);
    int  deleteConfigByIds(Long[] configIds);
    boolean checkConfigKeyUnique(SysConfig config);
}
```

规则：
- 接口名以 `I` 开头；方法必须 Javadoc（`@param` `@return`）。
- 返回 `int` 表示影响行数；返回领域对象表示查询结果。
- 接口中不出现 `Map<String,Object>`、`JSONObject` 这类弱类型返回。

### 5.3 Service 实现

```java
@Service
public class SysConfigServiceImpl implements ISysConfigService
{
    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public int insertConfig(SysConfig config)
    {
        int row = configMapper.insertConfig(config);
        if (row > 0)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    @Override
    public void deleteConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除", config.getConfigKey()));
            }
            configMapper.deleteConfigById(configId);
            redisService.deleteObject(getCacheKey(config.getConfigKey()));
        }
    }

    private String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
```

规则：
| # | 规则 |
|---|---|
| S1 | `@Service` 标注，实现方法全部 `@Override`。 |
| S2 | 依赖注入：字段 `@Autowired`（保持与存量代码一致）；**新模块**允许并推荐构造器注入（`private final` + `@AllArgsConstructor`）。同一模块内保持一种风格。 |
| S3 | 业务校验失败 `throw new ServiceException("可读的中文提示")`，**不要**返回错误码给 Controller 判断。 |
| S4 | 涉及多表写 / 主子表 / 批量写，必须 `@Transactional(rollbackFor = Exception.class)`。 |
| S5 | 缓存 key 集中在 `CacheConstants` 定义前缀，私有方法拼装；写操作同步失效/更新缓存。 |
| S6 | Service 不感知 HTTP（`HttpServletRequest/Response` 不下沉）。 |
| S7 | 数据权限查询加 `@DataScope(deptAlias = "d", userAlias = "u")`。 |
| S8 | 公共方法必须 Javadoc；私有工具方法可省略。 |

### 5.4 Mapper 接口与 XML

```java
public interface SysConfigMapper
{
    SysConfig selectConfig(SysConfig config);
    SysConfig selectConfigById(Long configId);
    List<SysConfig> selectConfigList(SysConfig config);
    SysConfig checkConfigKeyUnique(String configKey);
    int insertConfig(SysConfig config);
    int updateConfig(SysConfig config);
    int deleteConfigById(Long configId);
    int deleteConfigByIds(Long[] configIds);
}
```

XML 约定（`resources/mapper/<模块>/XxxMapper.xml`）：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.amy.system.mapper.SysConfigMapper">

    <resultMap type="SysConfig" id="SysConfigResult">
        <id     property="configId"    column="config_id"    />
        <result property="configName"  column="config_name"  />
        <result property="createBy"    column="create_by"    />
        <result property="createTime"  column="create_time"  />
    </resultMap>

    <sql id="selectConfigVo">
        select config_id, config_name, config_key, config_value, config_type,
               create_by, create_time, update_by, update_time, remark
        from sys_config
    </sql>

    <select id="selectConfigList" parameterType="SysConfig" resultMap="SysConfigResult">
        <include refid="selectConfigVo"/>
        <where>
            <if test="configName != null and configName != ''">
                AND config_name like concat('%', #{configName}, '%')
            </if>
            <if test="params.beginTime != null and params.beginTime != ''">
                and date_format(create_time,'%Y%m%d') &gt;= date_format(#{params.beginTime},'%Y%m%d')
            </if>
        </where>
    </select>

    <insert id="insertConfig" parameterType="SysConfig" useGeneratedKeys="true" keyProperty="configId">
        insert into sys_config
        <trim prefix="(" suffix=")" suffixOverrides=",">
            <if test="configName != null and configName != ''">config_name,</if>
            create_by,
        </trim>
        <trim prefix="values (" suffix=")" suffixOverrides=",">
            <if test="configName != null and configName != ''">#{configName},</if>
            #{createBy},
        </trim>
    </insert>

    <delete id="deleteConfigByIds" parameterType="Long">
        delete from sys_config where config_id in
        <foreach item="configId" collection="array" open="(" separator="," close=")">#{configId}</foreach>
    </delete>
</mapper>
```

规则：
| # | 规则 |
|---|---|
| M1 | `namespace` 必须是 Mapper 接口全限定名；XML 与接口同名同目录结构。 |
| M2 | 一律用 `resultMap`（id 为 `<实体>Result`），**不用** `resultType="map"`；列名与属性名通过 resultMap 映射。 |
| M3 | 查询列用 `<sql id="selectXxxVo">` 片段 + `<include>` 复用，**禁止 `select *`**。 |
| M4 | 参数一律 `#{}`（预编译）；**禁止 `${}` 字符串拼接**（排序字段必须过 `SqlUtil.escapeOrderBySql`）。 |
| M5 | 动态条件一律 `<where>` + `<if>`，String 判空写 `!= null and xxx != ''`。 |
| M6 | 时间范围用 `params.beginTime` / `params.endTime`（来自 `BaseEntity.params`）。 |
| M7 | 分页**不在 XML 写 limit**，由 PageHelper 织入。 |
| M8 | 自增主键：`useGeneratedKeys="true" keyProperty="主键属性"`。 |
| M9 | 修改用 `<set>`，只更新非空字段；时间字段 `update_time = sysdate()`。 |
| M10 | 删除优先逻辑删除 `update ... set del_flag = '1'`；物理删除仅用于日志/关联表。 |
| M11 | 单表 SQL 不做 `join` 超过 3 张表；复杂查询拆成多次单表查询在 Service 组装。 |
| M12 | Mapper 方法参数超过 3 个时，改为传入 Query/BO 对象。 |

### 5.5 实体（Domain）

```java
public class SysConfig extends BaseEntity        // 树形结构继承 TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @Excel(name = "参数主键", cellType = ColumnType.NUMERIC)
    private Long configId;

    @NotBlank(message = "参数键名不能为空")
    @Size(min = 0, max = 100, message = "参数键名长度不能超过100个字符")
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    // ... 其余 getter/setter

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
```

规则：
- 平台模块（`amy-modules/**`）沿用**手写 getter/setter + `ToStringBuilder`** 风格（与代码生成模板一致）。
- 新业务模块（`amy-biz-modules/**`）允许使用 Lombok：`@Data`、`@AllArgsConstructor`、`@NoArgsConstructor`、`@Builder`；**但 `@Data` 不得用于继承 `BaseEntity` 后需要 `params` 特殊处理的场景**，避免 `equals/hashCode` 问题。
- 所有实体 `extends BaseEntity`（含 `searchValue/createBy/createTime/updateBy/updateTime/remark/params`），树形 `extends TreeEntity`，并实现 `Serializable` + `serialVersionUID`。
- 校验注解加在 **getter** 上（`@NotBlank` `@Size` `@Xss`），导出注解 `@Excel` 加在 **字段** 上。
- 时间字段用 `Date` + `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`；**`Long` 类型时间戳需在 VO/DTO 层手动格式化后再出参**（如 `formatLoginTime`），不直接返回裸时间戳给前端。
- 实体不允许承载与表无关的业务字段；组合出参放 `domain/vo`。

### 5.5.1 domain 包结构（新模块强制，旧模块不变化）

新功能模块（`amy-biz-modules/**`）的 `domain` 包按职责细分，避免数据库实体类与请求/响应/查询对象混用。包名与类名后缀一一对应：

| 包 | 类名后缀 | 职责 |
|---|---|---|
| `domain/entity` | （无后缀，与表名一致） | 数据库表对象，`@TableName`、`extends BaseEntity`，仅承载表字段 |
| `domain/criteria` | `**Criteria.java` | XML 自定义查询的参数对象（替代 `@Param("entity")` 把实体当查询条件） |
| `domain/vo` | `**VO.java` | XML 自定义查询的返回对象（`resultMap` / `type="XxxVO"`）；实体不允许承载与表无关的业务字段，需要时组合 VO 出参 |
| `domain/bo` | `**BO.java` | Service 层业务处理过程对象（多实体组装、跨表计算等） |
| `domain/req` | `**Req.java` | Controller 入参对象（`@RequestBody`），仅承载请求字段 + 校验注解 |
| `domain/resp` | `**Resp.java` | Controller 出参对象（`AjaxResult` data / 直接返回），仅承载响应字段 |

约束：
- 适用范围：**新模块 / 新功能**。旧 RuoYi 平台模块（`amy-modules/**`）的 `domain/` 维持现状不变。
- `entity` 仍 `extends BaseEntity` 以获得软删除/审计字段；**不要**把业务字段塞进 entity。
- `criteria` 是可选的：自定义 XML 查询时才用；纯 MP `BaseMapper` 单表查询可直接传实体参数。
- `MybatisPlusConfig` 的 `typeAliasesPackage = "com.amy.**.domain"` 通过递归扫描覆盖 `domain` 及其全部子包，`domain.entity/vo/...` 下的类会自动注册别名（`type="XxxVO"` 可解析）。
- 简单模块可只建 `entity` + `req` + `resp`；逻辑复杂、类多时再按上表补齐。

### 5.5.2 Controller 职责边界（强制）

Controller **只**做四件事：

1. 入参接收（`@RequestBody` / `@PathVariable` / Query 参数绑定）
2. 权限注解（`@RequiresPermissions`）+ 日志注解（`@Log`）
3. 委托 Service（**直接传入 `Req` 对象**，不在 Controller 做转换）
4. 返回 `AjaxResult` / `TableDataInfo` / `void`

**禁止**在 Controller 写：

- ❌ `req` ↔ `entity` 互转方法（如 `convertToEntity`）—— 放 Service
- ❌ 多实体组装 / 计算字段 → 放 Service（必要时引入 `domain/bo`）
- ❌ 设置审计字段（`createBy / createTime / updateBy / updateTime`）→ 放 Service，通过 `SecurityUtils.getUsername()` + `DateUtils.getNowDate()` 完成
- ❌ 生成单号 / 业务编号 / 默认状态等

Service 私有方法（如 `convertToEntity(req)`、`generateReservationNum()`）承担上述逻辑；若多处复用，抽到 `domain/bo` 或独立 Converter。

适用范围：所有**新模块 / 新功能**。旧 RuoYi 平台模块（`amy-modules/**`）维持现状。

---

## 5.6 MyBatis-Plus 使用规范（新业务模块纯单表优先）

> 适用范围：**新模块 / 新功能**的纯单表 CRUD。多表关联、复杂动态 SQL、存量功能**一律走 XML**（§5.4），不要改成 MP。MP 依赖已在 `amy-common-core` 统一引入，新模块继承即可，**禁止**在子模块 `import` MP 的 BOM（会污染版本仲裁，见 §14）。

### 5.6.1 何时用 MP / 何时用 XML

| 场景 | 选型 |
|---|---|
| 新表的纯单表增删改查（含分页、逻辑删除） | **MP**（实体 + `BaseMapper` + `IService`，无需 Mapper XML） |
| 单表 + 数据权限（`@DataScope`） | MP `BaseMapper` + **一个最小 Mapper XML**（仅放带 `${entity.params.dataScope}` 的列表查询） |
| 多表 join、复杂动态条件、存量功能 | **MyBatis XML**（维持 §5.4 不变） |

### 5.6.2 依赖与配置（已就绪，勿重复引入）

- `amy-common-core` 已引入：`mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser-4.9`（分页拦截器所在）+ `mybatis-plus-extension` + `mybatis-plus-spring`（IService/ServiceImpl 所在）。
- 全局配置 `com.amy.common.core.config.MybatisPlusConfig`：注册 `MybatisPlusInterceptor` + `PaginationInnerInterceptor(DbType.MYSQL)`，并设 `typeAliasesPackage=com.amy.**.domain`、`mapperLocations=classpath*:mapper/**/*Mapper.xml`（兼容既有 XML 的 resultMap/别名）。
- 新模块 **无需**再引 MP 依赖；直接 `extends BaseMapper` / `IService`。

### 5.6.3 实体注解约定

```java
@TableName("mp_poc_demo")
public class MpPocDemo extends BaseEntity {   // delFlag 继承自 BaseEntity（已配 @TableLogic，默认软删除），无需本类重复声明
    @TableId(type = IdType.AUTO)          // 自增主键
    private Long demoId;
    private String demoName;
    private Integer status;
    private Long deptId;
    // getter/setter
}
```

- `BaseEntity` 的 `params`（`Map`）与 `searchValue` 已标 `@TableField(exist = false)`，**务必保留**——否则 MP 的 `insert/save` 会把 `Map` 当列，报 `Type handler was null on parameter mapping for property 'params'`。
- `del_flag` 用 `@TableLogic` 自动织入逻辑删除条件；自定义 XML 列表查询里仍手写 `WHERE d.del_flag = '0'`。

### 5.6.4 Mapper / Service / Controller 写法

```java
public interface MpPocDemoMapper extends BaseMapper<MpPocDemo> {
    // 需要数据权限时，补一个 XML 方法（见 5.6.5）；纯单表可用 BaseMapper 自带方法
    IPage<MpPocDemo> selectMpPocDemoList(IPage<MpPocDemo> page, @Param("entity") MpPocDemo entity);
}

public interface IMpPocDemoService extends com.baomidou.mybatisplus.spring.service.IService<MpPocDemo> { ... }

@Service
public class MpPocDemoServiceImpl
    extends com.baomidou.mybatisplus.spring.service.impl.ServiceImpl<MpPocDemoMapper, MpPocDemo>
    implements IMpPocDemoService { ... }
```

```java
@GetMapping("/list")
@DataScope(deptAlias = "d")            // 仅列表查询加；切面把 SQL 片段注入 entity.params.dataScope
public TableDataInfo list(MpPocDemo demo) {
    Page<MpPocDemo> page = new Page<>(1, 10);
    IPage<MpPocDemo> iPage = service.selectMpPocDemoList(page, demo);
    return new TableDataInfo(iPage.getRecords(), iPage.getTotal());
}
```

### 5.6.5 数据权限（MP + XML 混写最小集）

MP 的 `@DataScope` 切面与 MyBatis XML 完全兼容（切面是 Java 层、把 SQL 片段写进 `BaseEntity.params.dataScope`）。MP 分页 + 数据权限用一个 **最小 Mapper XML** 即可：

```xml
<select id="selectMpPocDemoList" resultType="MpPocDemo">
    select demo_id, demo_name, status, del_flag, dept_id, create_time, update_time
    from mp_poc_demo d
    <where>
        d.del_flag = '0'
        <if test="entity.demoName != null and entity.demoName != ''">AND d.demo_name like concat('%', #{entity.demoName}, '%')</if>
        <if test="entity.status != null">AND d.status = #{entity.status}</if>
        ${entity.params.dataScope}   <!-- @DataScope 切面注入 -->
    </where>
</select>
```

### 5.6.6 与 PageHelper 的边界

- **MP 路径**：`new Page<>(pageNum, pageSize)` + `IPage` 接收，`PaginationInnerInterceptor` 自动织入 limit。
- **XML 路径**：`PageHelper.startPage()` + `getDataTable(list)`。
- 同一查询**二选一**，禁止 `PageHelper.startPage()` 后再套 MP `Page`（分页参数会打架）。

---

## 6. 统一响应与分页

| 场景 | 类型 | 结构 |
|---|---|---|
| 管理端非分页接口 | `AjaxResult` | `code/msg/data` |
| 管理端分页列表 | `TableDataInfo` | `{ total, rows, code, msg }` |
| 服务间 Feign 调用 | `R<T>` | `{ code, msg, data }` |

```java
return success();              // AjaxResult 成功
return success(data);          // 带数据
return error("提示信息");       // 失败
return warn("提示信息");
return toAjax(rows);           // rows > 0 ? success : error
return R.ok(data);             // Feign
return R.fail("失败原因");
```

- **禁止**自定义新的响应包装类；**禁止**直接返回 `String`/`Boolean` 裸类型给前端。
- `HttpStatus` 定义状态码，不硬编码数字。
- 分页：`startPage()` → 查询 → `getDataTable(list)`；分页参数来自 `pageNum/pageSize/orderByColumn/isAsc`（`PageDomain`/`TableSupport`）。
- 若一次请求内多次分页查询，中间必须 `clearPage()`。

---

## 7. 参数校验与异常处理

- 入参校验：Controller 用 `@Validated` + JSR-303（`@NotBlank @NotNull @Size @Pattern`）；嵌套校验用 `@Valid`。
- XSS：字符串入参加 `@Xss(message = "...")`（注解在 getter）。
- 业务异常：`throw new ServiceException("中文可读提示")`。
- 系统异常不吞：统一由 `amy-common-security` 的 `GlobalExceptionHandler` 兜底（捕获 `ServiceException`、`BindException`、`MethodArgumentNotValidException`、`Exception`）。
- **禁止**：`e.printStackTrace()`、`catch(Exception e) {}`、 swallow 异常后返回 `success()`。
- 工具类判空统一 `StringUtils` / `ObjectUtils`（common-core）。

---

## 8. 通用能力使用规范

| 能力 | 用法 | 约束 |
|---|---|---|
| 权限 | `@RequiresPermissions("system:config:list")`；角色 `@RequiresRoles`；登录 `@RequiresLogin` | 权限标识格式 `模块:资源:操作`，取值 `list/query/add/edit/remove/export/import` |
| 操作日志 | `@Log(title="参数管理", businessType=BusinessType.INSERT)` | 敏感参数用 `excludeParamNames = {"password"}` 排除 |
| 数据权限 | `@DataScope(deptAlias="d", userAlias="u")` | 只加在 Service 的**列表查询**方法上 |
| 内部调用 | `@InnerAuth` + 请求头 `from-source: inner` | Feign 调用链自动透传，禁止外网暴露 |
| 多数据源 | `@Master` / `@Slave` 标在 Service 方法 | 默认主库 |
| 缓存 | `RedisService`（`setCacheObject/getCacheObject/deleteObject/keys`） | key 前缀必须在 `CacheConstants` 注册，带业务隔离 |
| 脱敏 | `@Sensitive` + `DesensitizedType` | 手机号/身份证/银行卡等出参必脱敏 |
| 幂等/分布式锁 | 使用 Redisson 或 Redis 原子操作 | 写接口在必要时加分布式锁 |
| 事务 | `@Transactional(rollbackFor = Exception.class)` | 事务方法内不做远程调用、不加锁后调用外部接口 |
| 异步 | `@Async` + 自定义线程池（禁止默认线程池） | 上下文传递用 TTL（`transmittable-thread-local`） |
| 定时任务 | 表达式由前端 `CronGenerator` 生成后落库 | 不在代码里硬编码业务 cron |

---

## 9. 跨服务调用（Feign）规范

契约模块 `amy-api/amy-api-<业务名>`，包名 `com.amy.<业务>.api`：

```
com.amy.system.api/
├── RemoteUserService.java      # @FeignClient 接口
├── domain/SysUser.java         # 跨服务传输实体（简洁，仅必要字段）
├── model/LoginUser.java        # 复合模型
└── factory/RemoteUserFallbackFactory.java   # 降级
```

```java
@FeignClient(contextId = "remoteUserService",
             value = ServiceNameConstants.SYSTEM_SERVICE,
             fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService
{
    @GetMapping("/user/info/{username}")
    R<LoginUser> getUserInfo(@PathVariable("username") String username,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
```

规则：
| # | 规则 |
|---|---|
| F1 | 服务名用 `ServiceNameConstants` 常量，**不写字符串字面量**。 |
| F2 | 每个 Feign 接口必须配 `contextId`（唯一）和 `fallbackFactory`（降级）。 |
| F3 | 返回统一 `R<T>`，调用方用 `R.isSuccess(ret)` 判断后再取 `getData()`。 |
| F4 | 内部接口加 `@RequestHeader(SecurityConstants.FROM_SOURCE)`，被调方方法加 `@InnerAuth`。 |
| F5 | `api` 模块**只放契约**，不放实现、不放业务；不得依赖具体业务模块。 |
| F6 | 降级工厂必须 `log.error` 记录原因，并返回 `R.fail("...失败:" + throwable.getMessage())`。 |
| F7 | 跨服务禁止传递大对象/文件流；文件上传走 `RemoteFileService`。 |

---

## 10. 配置规范

- 每个服务仅保留 `bootstrap.yml`（端口、应用名、Nacos 地址、`config.import`），其余配置全部放 Nacos。
- Nacos 共享配置：`application-dev.yml`（数据源、Redis、MyBatis、Feign、Sentinel、线程池）。
- Nacos 私有配置：`<spring.application.name>-dev.yml`。
- 端口分配：`gateway 8080`、`auth 18080`、`system 9201`、`gen 9202`、`job 9203`、`file 9300`、`wechat 9400`、`monitor 9100`；新业务模块从 `95xx` 起申请。
- 密钥、密码不进代码仓库；本地开发与线上通过 Nacos 区分。
- 日志：`logback.xml` 输出到 `logs/<appname>/`，禁止 `System.out.println`（启动横幅除外）。

---

## 11. 代码风格（与存量代码一致）

- **大括号换行（Allman 风格）**：`{` 独占一行（平台模块强制；新业务模块至少保持文件内统一）。
- 缩进 4 空格，禁用 Tab；文件编码 UTF-8；换行符 LF。
- 行宽不超过 120 字符。
- 类/方法/字段必须有 Javadoc；平台模块保留 `@author amy` 或改为实际作者。
- 注释用中文，`//` 后加一个空格；禁止块注释包裹大段废代码。
- 不留 `TODO`/`FIXME` 直接提交；如必须，写 `// TODO(作者): 原因与计划时间`。
- import：不使用通配符 `*`；顺序 `java / jakarta → 第三方 → com.amy`；静态 import 放最后。
- 常量不得硬编码在业务代码（状态码、缓存前缀、字典 key 等一律进 `*Constants`）。
- 魔法值：除 `0/1/-1` 外一律定义常量或枚举。
- 方法长度建议 ≤ 80 行；参数 ≤ 5 个。

---

## 12. CRUD 落地清单（新增一张业务表必做）

1. **建表**：`sql/` 下补 DDL，含 `create_by/create_time/update_by/update_time/remark/del_flag`（软删除强制约定见 §4.5），加索引与注释。**业务表名须符合 §4.4 的 `biz_<项目缩写>_<业务>` 前缀约定**。实体须 `extends BaseEntity` 以自动获得软删除能力，不要在实体里重复声明 `delFlag`。
2. **生成**：优先用 `amy-gen` 代码生成（模板在 `amy-modules/amy-gen/src/main/resources/vm/java|xml`），生成后再按本规范微调。
3. **补四层**：`domain/Xxx` → `mapper/XxxMapper` → `service/IXxxService` + `service/impl/XxxServiceImpl` → `controller/XxxController`；以及 `resources/mapper/<模块>/XxxMapper.xml`。
4. **补注解**：Controller 加 `@RequiresPermissions` + `@Log`；Service 列表加 `@DataScope`（如需要）。
5. **补校验**：实体 getter 上加 `@NotBlank/@Size/@Xss`；Controller 加 `@Validated`。
6. **补菜单/权限 SQL**：`sys_menu` 菜单 + 权限标识（`模块:资源:操作`）。
7. **自测**：分页、详情、新增、修改、删除、导出 6 个接口全部跑通；唯一性校验生效；审计字段正确。

---

## 13. 代码审查自查（提交前逐项确认）

- [ ] 包结构、类名、方法名符合 §3/§4
- [ ] Controller 无业务逻辑、无 try-catch、REST 语义正确、已挂权限与日志
- [ ] Service 事务边界正确、无 Mapper 之外的跨层依赖
- [ ] XML 无 `select *`、无 `${}`、无手写 limit、都用 resultMap
- [ ] 入参校验、XSS 注解齐全
- [ ] 缓存 key 走常量、写操作有失效
- [ ] 无硬编码常量、无 `System.out.println`、无吞异常
- [ ] 实体实现 `Serializable` + `toString`
- [ ] 新增依赖未引入 BOM、未污染父 pom 版本仲裁
- [ ] Nacos 配置与 `bootstrap.yml` 已同步更新
- [ ] 菜单/权限 SQL 已提交

---

## 14. 已知坑（踩过的，别再踩）

1. **子模块禁止 import 第三方 BOM**
   `wx-java-bom 4.8.6.B` 的父 pom 会把 `jackson-bom` 降到 2.18.4、`slf4j-api` 锁到 1.7.30、`logback-classic` 变成 test scope，导致 Jackson 3 缺 `JsonSerializeAs` 启动即崩、日志全丢。
   → 做法：在依赖上直接写 `<version>${wx-java.version}</version>`，见 `amy-wechat/pom.xml` 注释。

2. **Spring Boot 与 Spring Cloud 版本必须匹配**
   Spring Cloud 2025.1.x 的兼容性校验器只接受 Boot 4.0.x，使用 4.1.x 会被拒绝启动。
   → 应急 `spring.cloud.compatibility-verifier.enabled=false`；根治：回退 Boot 到 4.0.x 或升级匹配的 Cloud train。当前基线锁 **Boot 4.0.6 + Cloud 2025.1.1**。

3. **`@RestController("/xxx")` 是无效的**，路径必须写在 `@RequestMapping` 上。

4. **PageHelper 分页是线程变量**：`startPage()` 后必须紧跟查询；多次查询之间 `clearPage()`。

5. **`Long` 时间戳不要直接出参**，需在 VO/DTO 层格式化后再返回前端。

6. **导入 `wx-java` 等 starter 时留意自动配置冲突**，必要时在 `application-dev.yml` 中排除 `DruidDataSourceAutoConfigure` 等自动配置类。

7. **MyBatis-Plus 下 `BaseEntity.params` / `searchValue` 必须 `@TableField(exist = false)`**：这俩是 `Map` / 非表字段。XML 模式从不在列里写它们所以无感；但 MP 自动生成的 `insert/save` 会把 `params` 当列去绑定，报 `Type handler was null on parameter mapping for property 'params'`（javaType Map 无 TypeHandler）。已在 `amy-common-core/.../BaseEntity.java` 补齐，**改动前确认没被回退**（这是全局共享类，影响所有继承 BaseEntity 的 MP 实体）。

8. **MP 3.5.17（Boot4 构建）模块被拆碎且多为 optional**：`PaginationInnerInterceptor` 在 `mybatis-plus-jsqlparser-4.9`（包 `com.baomidou.mybatisplus.extension.plugins.inner`）；`MybatisPlusInterceptor` 在 `mybatis-plus-extension`；`IService`/`ServiceImpl` 被挪到 `mybatis-plus-spring`（包名是 `com.baomidou.mybatisplus.spring.service`，**不是** `extension.service`）；`Page` 在 `mybatis-plus-extension`、`IPage` 在 `mybatis-plus-core`。三者均 `<optional>true</optional>`，须在 `amy-common-core` 显式声明，否则 `compile` / 运行报找不到类。

9. **surefire 默认 2.12.4（根 pom 非 spring-boot-starter-parent，未管理插件版本），不支持 JUnit Platform**：跑 JUnit5/6 测试前须在对应模块 pom 显式声明 `maven-surefire-plugin` ≥ 3.5.x，否则 `Tests run: 0` 且静默通过。

10. **PageHelper 自动配置 `PageHelperAutoConfiguration` 引用 `org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration`（MP 下该类不存在）会打 WARN**，属无害，不影响 PageHelper 分页；不要因此去排除 PageHelper。

11. **独立模块做启动冒烟时，Nacos 私有 `<appname>-dev.yml` 为空会导致 dynamic-datasource 找不到主库**（`initial loaded [0] datasource`）。冒烟可用最高优先级的 `--spring.datasource.dynamic.datasource.master.*`（url/username/password/driver-class-name）+ `--spring.datasource.dynamic.primary=master` 注入主库，**不要去改 Nacos**；`@DataScope` / 分页 / 逻辑删除即可正常验证。

---

## 15. 参考落点（可直接照抄的样板）

| 想写什么 | 照抄哪个文件 |
|---|---|
| Controller | `amy-modules/amy-system/.../controller/SysConfigController.java` |
| Service 接口 | `amy-modules/amy-system/.../service/ISysConfigService.java` |
| Service 实现 + 缓存 | `amy-modules/amy-system/.../service/impl/SysConfigServiceImpl.java` |
| Mapper 接口 | `amy-modules/amy-system/.../mapper/SysConfigMapper.java` |
| Mapper XML | `amy-modules/amy-system/src/main/resources/mapper/system/SysConfigMapper.xml` |
| 实体（手写 getter 风格） | `amy-modules/amy-system/.../domain/SysConfig.java` |
| 实体（Lombok 风格） | `amy-biz-modules/amy-biz-sun-palace-art-space/.../domain/Projects.java` |
| MP 实体 / Mapper / Service / Controller（POC 样例） | `amy-biz-modules/amy-biz-sun-palace-art-space/.../domain/MpPocDemo.java`、`.../mapper/MpPocDemoMapper.java`、`.../service/IMpPocDemoService.java`、`.../controller/MpPocDemoController.java`、`.../resources/mapper/sunpalaceartspace/MpPocDemoMapper.xml` |
| MP 全局配置 | `amy-common/amy-common-core/.../config/MybatisPlusConfig.java` |
| Feign 契约 | `amy-api/amy-api-system/.../api/RemoteUserService.java` |
| Feign 降级 | `amy-api/amy-api-system/.../api/factory/RemoteFileFallbackFactory.java` |
| 启动类 | `amy-modules/amy-system/.../AmySystemApplication.java` |
| 模块 pom | `amy-modules/amy-system/pom.xml` |
| bootstrap.yml | `amy-modules/amy-system/src/main/resources/bootstrap.yml` |
| 生成模板 | `amy-modules/amy-gen/src/main/resources/vm/java/*.vm`、`vm/xml/mapper.xml.vm` |
