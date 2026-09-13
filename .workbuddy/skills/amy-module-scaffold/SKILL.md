---
name: amy-module-scaffold
description: 基于 quick-start/amy-module-template 快速生成一个新的 Amy 微服务子模块。复制模板、替换占位符、注册 Maven 模块、生成 Nacos 配置，并自动跑 POC 测试验证可用性。当用户要「新建/增加/起一个功能模块」「从模板复制模块」「脚手架生成微服务模块」时使用。
---

# amy-module-scaffold —— Amy 新功能模块脚手架

把 `quick-start/amy-module-template`（一个已验证的 MyBatis-Plus POC 子模块）复制成一个全新的、可直接开发业务的功能模块，并自动完成 Maven 注册、Nacos 配置生成与 POC 验证。

## 何时使用

用户表达以下意图之一时调用本技能：
- 「新建一个模块 `amy-biz-xxx`」「在 `amy-biz-modules` 下加个功能模块」
- 「用模板起一个新服务」「从 quick-start 模板复制一个模块」
- 「帮我脚手架生成一个微服务模块并跑通 POC」

## 输入参数（向用户确认，或合理推断默认值）

| 参数 | 含义 | 默认/推断 |
| --- | --- | --- |
| `--aggregator` | 目标聚合模块目录（模块将放其下，并注册进它的 pom `<modules>`） | `amy-biz-modules` |
| `--artifact` | Maven artifactId（目录名） | 必填，如 `amy-biz-demo` |
| `--package` | 基础包名 | 必填，须 `com.amy.<key>`，如 `com.amy.demo` |
| `--class` | 启动类简单名 | 由 artifact 推断，如 `amy-biz-demo`→`AmyBizDemoApplication` |
| `--port` | 服务端口 | `9401`（请避免与已有模块冲突） |
| `--desc` | 模块描述 | 必填，简短中文 |
| `--name` | 应用名 / Nacos dataId 前缀 | 默认同 artifact |
| `--parent` | 子模块 pom 的 parent artifactId | 默认 = 聚合模块目录名 |
| `--table-prefix` | 业务表前缀缩写（生成 `biz_<缩写>`），见 Agent.md §4.4 | 默认从 artifact 推导首字母缩写（`amy-biz-sun-palace-art-space`→`spas`） |
| `--publish` | 是否把 Nacos 配置发布到 Nacos 服务 | 默认不发布（仅生成本地文件） |

> artifactId 全局唯一性：生成前检查 `quick-start` 之外的同名目录与聚合 pom 是否已存在该 `<module>`，已存在则先询问用户或改用新名。

## 执行流程（调用脚本，不要手写复制逻辑）

```
bash .workbuddy/skills/amy-module-scaffold/scaffold.sh \
  --aggregator <聚合目录> --artifact <artifactId> --package <com.amy.xxx> \
  --class <AppClass> --port <port> --desc "<描述>" [--name <appName>] [--publish]
```

> 模板的 `domain` 包结构遵循 Agent.md §5.5.1（新模块 `domain/entity`、`domain/criteria`、`domain/vo` 等），脚手架生成的新模块自动套用此约定。

脚本会自动完成：
1. 复制 `quick-start/amy-module-template` → `<聚合目录>/<artifactId>`；
2. 替换占位符（含业务表前缀 `__TABLE_PREFIX__`，默认由 artifact 推导，可用 `--table-prefix` 覆盖；见 Agent.md §4.4）；
3. 按包名末段重命名 `com/amy/template`、`mapper/template` 目录；
4. 把 `<module><artifactId></module>` 写进聚合模块 pom 的 `<modules>`；
5. 生成 `<module>/src/main/resources/nacos/<appName>-dev.yml`（Nacos 可达且带 `--publish` 时通过 Open API 发布）；
6. 运行 `mvn -pl <聚合目录>/<artifactId> -am test`，执行 POC 测试类 `MpPocDemoTest`；
7. 解析结果并汇报（通过 / 失败 + 重跑命令）。

## POC 测试验证什么（用户关心的「可用性」）

测试 `MpPocDemoTest`（`@SpringBootTest`，已禁用 Nacos、自举数据源、自愈 `mp_poc_demo` 表）会断言：
- MyBatis-Plus 通用 CRUD + 自增主键回填；
- `IPage` 分页正确；
- `@TableLogic` 逻辑删除（del_flag 0→2，列表与 getById 不可见）；
- `@DataScope` 切面注入的数据权限 SQL 片段生效；
- `RedisService`（amy-common-redis）读写一致（Redis 不可用时仅告警不失败）。

本地需 MySQL(amy) 与 Redis 在线；Nacos 是否在线不影响测试。

## 完成后向用户汇报

- 模块路径、启动类全限定名、端口；
- 聚合 pom 是否已注册；
- Nacos 配置路径（及是否发布成功）；
- POC 测试结果（通过 / 失败 + 失败时的排查命令）。

## 注意事项

- `quick-start/` 本身不是构建模块，不要把它加进根 pom 的 `<modules>`。
- 若用户只是想看模板结构，直接打开 `quick-start/amy-module-template/README.md`，无需执行脚本。
- 模块交付前可删除 POC 相关类（`MpPocDemo*`、`MpPocProbe`、`MpPocDemoTest`、相关 XML/SQL）换成真实业务。
