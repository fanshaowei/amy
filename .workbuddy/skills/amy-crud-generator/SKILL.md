---
name: amy-crud-generator
description: This skill should be used when the user wants to add a new CRUD sub-feature (新增子功能/新增业务表功能) to an existing backend maven submodule of the amy project (RuoYi-Cloud 风格微服务，如 amy-modules/amy-system、amy-biz-modules/amy-biz-* 等). 用户会提供字段信息和目标 maven 子模块；本 skill 据此生成 Domain、Mapper/Service/Controller 等文件（可选 DDL 与菜单权限 SQL），并完成可用性校验。持久层自动分支：新业务模块纯单表优先 MyBatis-Plus（免 Mapper XML），平台模块/多表/复杂 SQL 维持 MyBatis XML 模式。规范来源为项目根目录的 Agent.md 与 amy-modules/amy-system 存量代码。
agent_created: true
---

# amy 后端 CRUD 功能生成器

为 amy 项目（RuoYi-Cloud 风格）的指定 maven 子模块生成一套完整可用的 CRUD 功能代码，遵循项目根目录 `Agent.md` 的技术规范。

## 必需输入

| 输入 | 说明 | 缺失时处理 |
|---|---|---|
| 功能名 functionName | 如 "项目管理" | 必须有，缺失则询问 |
| 表名 tableName | 如 `biz_projects`，含模块前缀小写下划线 | 可由功能名推导，需向用户确认 |
| 字段列表 | 每个字段：Java 属性名、类型、注释；可选：是否查询(查询方式 EQ/LIKE/BETWEEN 等)、是否必填、是否导出、是否字典 | 至少给出字段名+类型+注释，其余按默认（均不查询、String 必填加 @NotBlank） |
| 目标 maven 子模块 | 如 `amy-modules/amy-system`、`amy-biz-modules/amy-biz-sun-palace-art-space` | 必须有，缺失则询问 |
| 主键 | 默认 `<表名单数>_id`（如 `project_id` → `projectId`，Long 自增） | 未说明则用默认 |
| 持久层模式 | `xml`（默认，存量/多表/复杂）或 `mp`（新业务模块纯单表优先） | 不指定时按规则推断：**目标为 `amy-biz-modules/**` 的新功能且为纯单表 → `mp`**；平台模块 `amy-modules/**`、含多表/复杂 SQL、或用户明确要求 → `xml` |
| 附加项 | 是否需要 DDL 脚本 / 菜单权限 SQL / 是否树形结构 / 是否需要数据权限（`@DataScope`） | 默认不生成树形；数据权限默认关（MP 模式下开数据权限会多生成一个最小 Mapper XML） |

字段类型映射：`bigint→Long`、`int→Integer`、`varchar/char/text→String`、`datetime/date→Date`、`decimal→BigDecimal`、`tinyint(1)→Integer`（状态类 char(1) 用 String）。

## 执行流程

### Step 1 — 定位模块与解析命名（禁止凭猜）

1. 确认目标模块存在：读取 `{module}/pom.xml` 与 `{module}/src/main/java/**/*Application.java`。
2. **根包一律从该模块 Application 类的实际 package 推导**，不推测。例如 `com.amy.system`、`com.amy.sunpalaceartspace`。
3. 权限前缀：平台模块用其语义名（`amy-system` → `system`）；业务模块用目录名去掉 `amy-biz-` 后的合并小写（`amy-biz-sun-palace-art-space` → `sunpalaceartspace`）。权限标识格式 `{prefix}:{businessName}:{list|query|add|edit|remove|export}`。
4. 读取该模块 pom.xml 确认是否已有 `org.projectlombok:lombok` 依赖：有 → 实体可用 Lombok `@Data`（校验注解放字段上）；无 → 手写 getter/setter（校验注解放 getter 上，与 `amy-system` 存量风格一致）。

### Step 2 — 模块可用性前置检查

逐项检查目标模块，不满足则先补齐或明确告知用户：

1. Application 类是否挂了 `@EnableCustomConfig`（内含 `@MapperScan("com.amy.**.mapper")`）与 `@EnableRyFeignClients`。缺失则加上（import 自 `com.amy.common.security.annotation.*`）。
2. pom 是否包含：`amy-common-core`、`amy-common-datasource`、`mysql-connector-j`、`nacos-discovery/config`、`sentinel`、`actuator`、`amy-common-log`、`amy-common-swagger`。按 `amy-modules/amy-system/pom.xml` 的写法补齐。
3. 业务模块若需要 Redis/数据权限，追加 `amy-common-redis` / `amy-common-datascope`。

### Step 3 — 确定持久层模式并生成文件

先用 `持久层模式` 输入（或 Step 1 推断规则）决定走哪条分支：

- **`mp` 模式（MyBatis-Plus，纯单表优先）**：实体 + Mapper 接口（`extends BaseMapper`）+ Service 接口（`extends IService`）+ ServiceImpl（`extends ServiceImpl`）+ Controller。
  - **只有纯单表 CRUD（无数据权限）**：**不生成 Mapper XML**，全部用 MP `BaseMapper` 自带方法（save / getById / page / removeByIds / updateById 等）。
  - **需要数据权限（`@DataScope`）**：额外生成一个**最小 Mapper XML**，仅含带 `${entity.params.dataScope}` 的列表查询方法，其余 CRUD 仍走 MP。
  - 模板见 `references/templates.md` 的 **§MP 模式** 段；实体须带 `@TableName` / `@TableId(type=IdType.AUTO)` / `@TableLogic(value="0", delval="2")`，且**不要**自己加 `params` / `searchValue` 字段（`BaseEntity` 已处理）。
- **`xml` 模式（存量 / 多表 / 复杂 SQL）**：严格按 `references/templates.md` 的 §1–§6 生成六个文件（实体 / Mapper 接口 / Service 接口 / ServiceImpl / Controller / Mapper XML）。

> 模式判断默认：**新业务模块（`amy-biz-modules/**`）+ 纯单表 → `mp`**；平台模块、多表关联、复杂动态 SQL、或用户明确指定 → `xml`。同一查询**禁止 MP 与 PageHelper 混用**（MP 用 `Page`/`IPage`，XML 用 `PageHelper.startPage()`）。

文件清单：

| 模式 | 文件 | 路径 |
|---|---|---|
| 通用 | 实体 | `{module}/src/main/java/{packagePath}/domain/{Entity}.java` |
| 通用 | Controller | `{module}/src/main/java/{packagePath}/controller/{Entity}Controller.java` |
| mp | Mapper 接口（`extends BaseMapper<{Entity}>`） | `{module}/src/main/java/{packagePath}/mapper/{Entity}Mapper.java` |
| mp | Service 接口（`extends IService<{Entity}>`） | `{module}/src/main/java/{packagePath}/service/I{Entity}Service.java` |
| mp | Service 实现（`extends ServiceImpl<{Entity}Mapper,{Entity}>`） | `{module}/src/main/java/{packagePath}/service/impl/{Entity}ServiceImpl.java` |
| xml | Mapper 接口 | `{module}/src/main/java/{packagePath}/mapper/{Entity}Mapper.java` |
| xml | Service 接口 | `{module}/src/main/java/{packagePath}/service/I{Entity}Service.java` |
| xml | Service 实现 | `{module}/src/main/java/{packagePath}/service/impl/{Entity}ServiceImpl.java` |
| xml / mp(带数据权限) | Mapper XML | `{module}/src/main/resources/mapper/{moduleDir}/{Entity}Mapper.xml` |

严格按照 `references/templates.md` 的对应模板生成，模板中 `{...}` 占位符按 Step 1 的解析结果替换。

**XML 目录强制规则**：必须放在 `resources/mapper/<moduleDir>/`（如 `mapper/system/`、`mapper/sunpalaceartspace/`），因为 mybatis 的 mapperLocations 按 `classpath*:mapper/**/*Mapper.xml` 规则扫描。已知 `amy-biz-sun-palace-art-space` 存在错误的点号目录 `mapper.sumpalaceartspace`，**不得沿用**；若发现目标模块有此类目录，将新 XML 放入规范的 `mapper/<moduleDir>/` 并提示用户迁移历史文件。

**可选生成**：
- DDL：`sql/<tableName>.sql`，必须包含 `create_by/create_time/update_by/update_time/remark`（物理删除表再加 `del_flag char(1) default '0'`）与索引、字段注释。
- 菜单权限 SQL：`sys_menu` 插入语句（菜单 + 按钮六条：查询、详情、新增、修改、删除、导出），perms 值与 Controller 的 `@RequiresPermissions` 一致。

### Step 4 — 可用性校验（全部通过才算完成）

1. **编译校验**（必须执行）：
   ```bash
   cd <项目根目录> && mvn -q compile -pl <module相对路径> -am
   ```
   编译报错必须修复后重跑，直到通过。
2. 静态检查清单：
   - Mapper XML 的 `namespace` == Mapper 接口全限定名
   - XML 在 `resources/mapper/<moduleDir>/` 下（见 Step 3 强制规则）
   - Mapper 接口在 `{package}.mapper` 包下（被 `@MapperScan("com.amy.**.mapper")` 扫到）
   - resultMap 的 `type` 用短类名（typeAliasesPackage 覆盖 `com.amy.**.domain`），且每个表字段都有 `<result>` 映射
   - insert 带 `useGeneratedKeys="true" keyProperty="<pk>"`；批量删除 `foreach collection="array"`
   - 查询列用 `<sql id>` 片段复用，禁 `select *`，参数全用 `#{}`，禁 `${}`
   - Controller 继承 `BaseController`；`/list` 用 `startPage()` + `getDataTable()`；REST 映射完整（GET /list、GET /{id}、POST、PUT、DELETE /{ids}、POST /export）
   - 写操作挂 `@Log(title=..., businessType=...)`；除 list 外挂 `@RequiresPermissions`
   - 实体继承 `BaseEntity`，实现 `Serializable` + `serialVersionUID`；必填字段有校验注解
   - ServiceImpl 多表写时加 `@Transactional(rollbackFor = Exception.class)`
3. **`mp` 模式额外校验**：
   - 前置依赖：`amy-common-core` 已引入 MP（`mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser-4.9` + `mybatis-plus-extension` + `mybatis-plus-spring`），且 `BaseEntity.params` / `searchValue` 已标 `@TableField(exist = false)`。**若编译报 `Type handler was null on parameter mapping for property 'params'`**，先确认 `BaseEntity` 的这个注解没被回退（详见 Agent.md §14 第 7 条）。
   - 实体须有 `@TableName("{table}")`、`@TableId(type = IdType.AUTO)`、`del_flag` 字段 `@TableLogic(value = "0", delval = "2")`。
   - Mapper 接口 `extends BaseMapper<{Entity}>`；Service 接口 `extends com.baomidou.mybatisplus.spring.service.IService<{Entity}>`；ServiceImpl `extends com.baomidou.mybatisplus.spring.service.impl.ServiceImpl<{Entity}Mapper, {Entity}>`（包名是 `...spring.service`，**不是** `extension.service`）。
   - **纯单表无数据权限时不应生成 Mapper XML**；需要数据权限时才生成一个最小 XML（仅带 `${entity.params.dataScope}` 的列表方法），且 XML 的 `resultType`/`type` 用短类名（`MpPocDemo` 这类，typeAliasesPackage 已覆盖）。
   - Controller `/list` 用 MP 分页：`new Page<>(pageNum, pageSize)` + `IPage`，返回 `new TableDataInfo(iPage.getRecords(), iPage.getTotal())`；**不要用** `startPage()`（那是 PageHelper）。
4. 若模块是首次运行（没有 bootstrap.yml 或未在 Nacos 注册），提醒用户完成 Nacos 私有配置与端口申请（规范见 Agent.md §10）。独立冒烟若报 `dynamic-datasource initial loaded [0] datasource`，用 `--spring.datasource.dynamic.datasource.master.*` 注入主库（见 Agent.md §14 第 11 条），勿改 Nacos。

### Step 5 — 汇报结果

最终回复必须包含：生成的文件清单（绝对路径）、暴露的 6 个 REST 端点、权限标识列表、需要用户手工执行的步骤（执行 DDL、执行菜单 SQL、重启服务、Nacos 配置如有新增）。

## 参考

- `references/templates.md` — 六个文件 + DDL + 菜单 SQL 的完整模板（生成时逐字对照）
- 项目根目录 `Agent.md` — 完整技术规范（命名、分层、注解、已知坑）
- 存量样板：`amy-modules/amy-system/.../SysConfig*`（手写 getter 风格）、`amy-modules/amy-gen/src/main/resources/vm/`（代码生成器模板）
