# quick-start（快速起模块）

本目录是 Amy 项目「新功能模块」的复制源与脚手架技能入口。

```
quick-start/
├── README.md                  # 本文件
└── amy-module-template/       # Maven 子模块模板（复制源，详见其内部 README.md）
    ├── pom.xml
    ├── sql/mp_poc_demo.sql
    └── src/{main,test}/...
```

## 怎么用

不要手动复制。直接让 WorkBuddy 使用 **`amy-module-scaffold`** 技能：

> 「用 amy-module-scaffold 技能，在 `amy-biz-modules` 下新建模块 `amy-biz-demo`，
>  包名 `com.amy.demo`，端口 9410，描述『演示模块』，并跑 POC 验证。」

技能会自动完成：
1. 复制 `amy-module-template` → `<聚合模块>/<artifactId>`；
2. 替换占位符（含业务表前缀 `__TABLE_PREFIX__`），并按包名末段重命名 `com/amy/template`、`mapper/template` 目录；
3. 把 `<module>` 写进聚合模块 `pom.xml` 的 `<modules>`；
4. 生成 Nacos 配置文件 `<module>/src/main/resources/nacos/<appName>-dev.yml`（Nacos 可达时尝试发布）；
5. 执行 `mvn -pl <module> -am test` 跑 POC，校验 CRUD / 分页 / 逻辑删除 / 数据权限 / Redis，并回报结果。

## 业务表命名前缀（重要）

同一数据库会被多个项目/业务功能共用。为避免表名冲突、区分归属，业务表统一加**项目缩写前缀** `biz_<项目缩写>_<业务>`（如 `biz_spas_projects`）。完整规范见仓库根 `Agent.md` §4.4。新建业务表必须含 `del_flag` 列（软删除强制约定，见 Agent.md §4.5）；新模块实体继承 `BaseEntity` 即自动获得软删除能力，无需额外配置。

脚手架会自动推导前缀：
- 不传 `--table-prefix` 时，从 `--artifact` 推导首字母缩写：`amy-biz-sun-palace-art-space` → `spas`，故前缀为 `biz_spas`；
- 也可显式指定：`--table-prefix spas`。

生成后，`__TABLE_PREFIX__` 会被替换成 `biz_spas`，出现在 Nacos 配置注释与模板 README 的业务表命名示例中，方便后续建业务表直接套用。

## 模板能力一览

完整 Spring Web · MyBatis-Plus 单表 CRUD 零 XML · 数据权限 @DataScope · Redis · Nacos 注册/配置 · 内置 POC（测试 + 运行时探针）。

详见 `amy-module-template/README.md`。
