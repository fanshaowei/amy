# amy-module-template（功能模块模板）

这是一份「开箱即用」的 Amy 微服务子模块模板，沉淀自 `amy-biz-sun-palace-art-space` 的 MyBatis-Plus POC 验证。
**不要直接改这里**——它是复制源；真正落地请用 `amy-module-scaffold` 技能（见 `../README.md`）。

## 模板已内置能力

| 能力 | 说明 |
| --- | --- |
| 完整 Spring Web | `spring-boot-starter-web`，可直接写 `@RestController` |
| MyBatis-Plus CRUD | 单表零 XML（`BaseMapper` / `IService`），分页用 `IPage` |
| 数据权限 | 沿用若依 `@DataScope` 切面（`entity.params.dataScope` 注入 SQL） |
| Redis | `amy-common-redis`（`RedisService` / `amyRedisTemplate`） |
| Nacos | 服务注册发现 + 配置中心（`bootstrap.yaml` 已配，导入 `optional:nacos:...`） |
| 完整 POC | `MpPocDemo` 实体 + Mapper/Service/Controller + 运行时探针 + JUnit 测试 |

## 目录结构（清晰分层）

```
amy-module-template/
├── pom.xml                         # 父/子模块声明、依赖、surefire 3.5.5
├── sql/mp_poc_demo.sql             # POC 表 DDL（手动建表/重置用）
└── src
    ├── main
    │   ├── java/com/amy/template/
    │   │   ├── AmyTemplateApplication.java   # 启动类
    │   │   ├── MpPocProbe.java               # 运行时 POC 探针（--spring.profiles.active=mp-poc）
    │   │   ├── controller/MpPocDemoController.java  # REST + Redis 演示
    │   │   ├── domain/entity/MpPocDemo.java  # @TableName / @TableId(AUTO) / @TableLogic
    │   │   ├── mapper/MpPocDemoMapper.java   # extends BaseMapper
    │   │   ├── service/IMpPocDemoService.java
    │   │   └── service/impl/MpPocDemoServiceImpl.java
    │   └── resources/
    │       ├── bootstrap.yaml                # Nacos 注册/配置 + 端口
    │       ├── logback.xml                   # 日志（logs/<appName>）
    │       ├── mapper/template/MpPocDemoMapper.xml   # 仅保留带数据权限的列表查询
    │       └── nacos/nacos-dev.yml           # Nacos 配置样例（数据源/Redis）
    └── test
        ├── java/com/amy/template/MpPocDemoTest.java   # 不依赖 Nacos 的 POC 测试
        └── resources/bootstrap.yaml                     # 测试专用：禁用 Nacos
```

## 占位符（生成时被替换）

| 占位符 | 含义 | 例 |
| --- | --- | --- |
| `__APP_ARTIFACT__` | Maven artifactId | `amy-biz-demo` |
| `__APP_NAME__` | 应用名 / Nacos dataId 前缀 | `amy-biz-demo` |
| `__BASE_PACKAGE__` | 基础包名 | `com.amy.demo` |
| `__APP_CLASS__` | 启动类简单名 | `AmyBizDemoApplication` |
| `__APP_PORT__` | 服务端口 | `9410` |
| `__APP_DESC__` | 模块描述 | `演示模块` |
| `__PARENT_ARTIFACT__` | 所属聚合模块 artifactId | `amy-biz-modules` |
| `__TABLE_PREFIX__` | 业务表前缀（= `biz_<项目缩写>`） | `biz_spas` |

> 注：`com/amy/template` 与 `mapper/template` 目录也会按包名末段自动改名；`__TABLE_PREFIX__` 由脚手架按 `--table-prefix`（缺省取应用名首字母缩写）自动注入，用于新建业务表时统一加项目前缀（见 Agent.md §4.4）。POC 表 `mp_poc_demo` 不受此前缀约束。

## 本地跑 POC（无需 Nacos）

```bash
# 确保本地 MySQL(amy) 与 Redis 在线，然后：
mvn -pl <聚合模块>/<artifactId> -am test
# 或仅跑单个测试类
mvn -pl <聚合模块>/<artifactId> -am test -Dtest=MpPocDemoTest
```

测试会：
1. 用 `CREATE TABLE IF NOT EXISTS` 自愈 `mp_poc_demo` 表；
2. 验证 MP 通用 CRUD / `IPage` 分页 / `@TableLogic` 逻辑删除 / `@DataScope` 数据权限 / `RedisService`；
3. 自动清理 POC 数据。

## 运行时探针（需要真实数据源/Nacos 时）

```bash
java -jar <artifactId>.jar --spring.profiles.active=mp-poc
# 探针跑完会 System.exit(0)（PASS）或 (1)（FAIL）
```

## 换成真实业务

1. 复制 `domain/MpPocDemo.java` → 你的实体，改 `@TableName` 与字段。`delFlag` 已由 `BaseEntity` 提供（全局 `@TableLogic` 软删除），**业务表必须含 `del_flag` 列，且不要在实体里重复声明 `delFlag`**。业务表名须带项目前缀：`@TableName("__TABLE_PREFIX___<业务>")`（如 `__TABLE_PREFIX___projects`），详见 Agent.md §4.4 / §4.5。
2. Mapper 继承 `BaseMapper<Xxx>`，Service 继承 `IService<Xxx>` / `ServiceImpl<XxxMapper, Xxx>`。
3. 纯单表 CRUD 直接用 `save / getById / updateById / removeByIds / page`；**不要写 XML**。
4. 需要数据权限的列表查询：自定义 Mapper 方法 + 一个 XML（参考 `MpPocDemoMapper.xml`），SQL 末尾加 `${entity.params.dataScope}`，Controller 方法加 `@DataScope(deptAlias = "d")`。
5. 删除 POC 相关类（`MpPocDemo*`、`MpPocProbe`）即可交付干净模块。
