# amy CRUD 代码模板

占位符约定：

| 占位符 | 含义 | 示例 |
|---|---|---|
| `{package}` | 模块根包 | `com.amy.system` |
| `{packagePath}` | 根包转路径 | `com/amy/system` |
| `{Entity}` | 实体类名（UpperCamel） | `SysConfig` |
| `{entity}` | 实体变量名（lowerCamel） | `sysConfig` 或语义短名 `config` |
| `{table}` | 表名 | `sys_config` |
| `{pk}` / `{Pk}` | 主键属性 / 首字母大写 | `configId` / `ConfigId` |
| `{pkColumn}` | 主键列名 | `config_id` |
| `{businessName}` | URL 资源名（小写） | `config` |
| `{permPrefix}` | 权限前缀 | `system` |
| `{functionName}` | 功能中文名 | `参数配置` |
| `{Field}` / `{field}` | 普通字段 UpperCamel / lowerCamel | `ConfigName` / `configName` |
| `{fieldColumn}` | 普通字段列名 | `config_name` |
| `{FieldJavaType}` | 字段 Java 类型 | `String` |

---

## 1. 实体 {Entity}.java（手写 getter/setter 风格，模块无 lombok 时用）

```java
package {package}.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.amy.common.core.annotation.Excel;
import com.amy.common.core.web.domain.BaseEntity;

/**
 * {functionName}对象 {table}
 *
 * @author amy
 */
public class {Entity} extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** {pk注释} */
    @Excel(name = "{pk注释}")
    private {pkType} {pk};

    /** {字段注释} */
    @Excel(name = "{字段注释}")
    private {FieldJavaType} {field};

    public {pkType} get{Pk}()
    {
        return {pk};
    }

    public void set{Pk}({pkType} {pk})
    {
        this.{pk} = {pk};
    }

    // String 且必填的字段，校验注解放在 getter 上
    @NotBlank(message = "{字段注释}不能为空")
    @Size(min = 0, max = {长度}, message = "{字段注释}长度不能超过{长度}个字符")
    public {FieldJavaType} get{Field}()
    {
        return {field};
    }

    public void set{Field}({FieldJavaType} {field})
    {
        this.{field} = {field};
    }

    // Date 字段序列化格式（非必填字段校验注解可省略）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getCreateTime()
    {
        return super.getCreateTime();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("{pk}", get{Pk}())
            .append("{field}", get{Field}())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
```

**Lombok 变体**（模块 pom 已有 lombok 依赖时可用）：类上加 `@Data`，删除全部 getter/setter/toString，校验注解（`@NotBlank`/`@Size`/`@NotNull`）与 `@Excel`、`@JsonFormat` 全部放在**字段**上。布尔字段禁止 `isXxx` 命名，需要序列化为 `isXxx` 时加 `@JsonProperty("isRead")`。

**导出注解**：数字列 `@Excel(name = "...", cellType = Excel.ColumnType.NUMERIC)`；字典列 `@Excel(name = "...", readConverterExp = "0=男,1=女")`；日期列 `@Excel(name = "...", width = 30, dateFormat = "yyyy-MM-dd")`。

---

## 2. Mapper 接口 {Entity}Mapper.java

```java
package {package}.mapper;

import java.util.List;
import {package}.domain.{Entity};

/**
 * {functionName} 数据层
 *
 * @author amy
 */
public interface {Entity}Mapper
{
    /**
     * 查询{functionName}
     *
     * @param {pk} {functionName}主键
     * @return {functionName}
     */
    public {Entity} select{Entity}By{Pk}({pkType} {pk});

    /**
     * 查询{functionName}列表
     *
     * @param {entity} {functionName}
     * @return {functionName}集合
     */
    public List<{Entity}> select{Entity}List({Entity} {entity});

    /**
     * 新增{functionName}
     *
     * @param {entity} {functionName}
     * @return 结果
     */
    public int insert{Entity}({Entity} {entity});

    /**
     * 修改{functionName}
     *
     * @param {entity} {functionName}
     * @return 结果
     */
    public int update{Entity}({Entity} {entity});

    /**
     * 删除{functionName}
     *
     * @param {pk} {functionName}主键
     * @return 结果
     */
    public int delete{Entity}By{Pk}({pkType} {pk});

    /**
     * 批量删除{functionName}
     *
     * @param {pk}s 需要删除的数据主键集合
     * @return 结果
     */
    public int delete{Entity}By{Pk}s({pkType}[] {pk}s);
}
```

有唯一性校验需求时追加：`public {Entity} check{Field}Unique(String {field});`

---

## 3. Service 接口 I{Entity}Service.java

```java
package {package}.service;

import java.util.List;
import {package}.domain.{Entity};

/**
 * {functionName} 服务层
 *
 * @author amy
 */
public interface I{Entity}Service
{
    /**
     * 查询{functionName}
     *
     * @param {pk} {functionName}主键
     * @return {functionName}
     */
    public {Entity} select{Entity}By{Pk}({pkType} {pk});

    /**
     * 查询{functionName}列表
     *
     * @param {entity} {functionName}
     * @return {functionName}集合
     */
    public List<{Entity}> select{Entity}List({Entity} {entity});

    /**
     * 新增{functionName}
     *
     * @param {entity} {functionName}
     * @return 结果
     */
    public int insert{Entity}({Entity} {entity});

    /**
     * 修改{functionName}
     *
     * @param {entity} {functionName}
     * @return 结果
     */
    public int update{Entity}({Entity} {entity});

    /**
     * 批量删除{functionName}
     *
     * @param {pk}s 需要删除的{functionName}主键集合
     * @return 结果
     */
    public int delete{Entity}By{Pk}s({pkType}[] {pk}s);
}
```

---

## 4. Service 实现 {Entity}ServiceImpl.java

```java
package {package}.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import {package}.domain.{Entity};
import {package}.mapper.{Entity}Mapper;
import {package}.service.I{Entity}Service;

/**
 * {functionName}服务层实现
 *
 * @author amy
 */
@Service
public class {Entity}ServiceImpl implements I{Entity}Service
{
    @Autowired
    private {Entity}Mapper {entity}Mapper;

    /**
     * 查询{functionName}
     *
     * @param {pk} {functionName}主键
     * @return {functionName}
     */
    @Override
    public {Entity} select{Entity}By{Pk}({pkType} {pk})
    {
        return {entity}Mapper.select{Entity}By{Pk}({pk});
    }

    /**
     * 查询{functionName}列表
     *
     * @param {entity} {functionName}
     * @return {functionName}
     */
    @Override
    public List<{Entity}> select{Entity}List({Entity} {entity})
    {
        return {entity}Mapper.select{Entity}List({entity});
    }

    /**
     * 新增{functionName}
     *
     * @param {entity} {functionName}
     * @return 结果
     */
    @Override
    public int insert{Entity}({Entity} {entity})
    {
        {entity}.setCreateTime(DateUtils.getNowDate());
        return {entity}Mapper.insert{Entity}({entity});
    }

    /**
     * 修改{functionName}
     *
     * @param {entity} {functionName}
     * @return 结果
     */
    @Override
    public int update{Entity}({Entity} {entity})
    {
        {entity}.setUpdateTime(DateUtils.getNowDate());
        return {entity}Mapper.update{Entity}({entity});
    }

    /**
     * 批量删除{functionName}
     *
     * @param {pk}s 需要删除的{functionName}主键
     * @return 结果
     */
    @Override
    public int delete{Entity}By{Pk}s({pkType}[] {pk}s)
    {
        return {entity}Mapper.delete{Entity}By{Pk}s({pk}s);
    }
}
```

补充规则：
- `import com.amy.common.core.utils.DateUtils;`
- 业务校验（存在性、唯一性、状态合法性）写在 Service：`throw new ServiceException("中文可读提示")`
- 多表写 / 主子表 / 先删后插：方法加 `@Transactional(rollbackFor = Exception.class)`
- 唯一校验方法（如需要）：
  ```java
  @Override
  public boolean check{Field}Unique({Entity} {entity})
  {
      {pkType} {pk} = StringUtils.isNull({entity}.get{Pk}()) ? -1L : {entity}.get{Pk}();
      {Entity} info = {entity}Mapper.check{Field}Unique({entity}.get{Field}());
      if (StringUtils.isNotNull(info) && info.get{Pk}().longValue() != {pk}.longValue())
      {
          return UserConstants.NOT_UNIQUE;
      }
      return UserConstants.UNIQUE;
  }
  ```

---

## 5. Controller {Entity}Controller.java

```java
package {package}.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.amy.common.core.utils.poi.ExcelUtil;
import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.domain.AjaxResult;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.common.security.utils.SecurityUtils;
import {package}.domain.{Entity};
import {package}.service.I{Entity}Service;

/**
 * {functionName} 信息操作处理
 *
 * @author amy
 */
@RestController
@RequestMapping("/{businessName}")
public class {Entity}Controller extends BaseController
{
    @Autowired
    private I{Entity}Service {entity}Service;

    /**
     * 查询{functionName}列表
     */
    @RequiresPermissions("{permPrefix}:{businessName}:list")
    @GetMapping("/list")
    public TableDataInfo list({Entity} {entity})
    {
        startPage();
        List<{Entity}> list = {entity}Service.select{Entity}List({entity});
        return getDataTable(list);
    }

    /**
     * 导出{functionName}列表
     */
    @RequiresPermissions("{permPrefix}:{businessName}:export")
    @Log(title = "{functionName}", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, {Entity} {entity})
    {
        List<{Entity}> list = {entity}Service.select{Entity}List({entity});
        ExcelUtil<{Entity}> util = new ExcelUtil<{Entity}>({Entity}.class);
        util.exportExcel(response, list, "{functionName}数据");
    }

    /**
     * 获取{functionName}详细信息
     */
    @RequiresPermissions("{permPrefix}:{businessName}:query")
    @GetMapping(value = "/{{pk}}")
    public AjaxResult getInfo(@PathVariable("{pk}") {pkType} {pk})
    {
        return success({entity}Service.select{Entity}By{Pk}({pk}));
    }

    /**
     * 新增{functionName}
     */
    @RequiresPermissions("{permPrefix}:{businessName}:add")
    @Log(title = "{functionName}", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody {Entity} {entity})
    {
        {entity}.setCreateBy(SecurityUtils.getUsername());
        return toAjax({entity}Service.insert{Entity}({entity}));
    }

    /**
     * 修改{functionName}
     */
    @RequiresPermissions("{permPrefix}:{businessName}:edit")
    @Log(title = "{functionName}", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody {Entity} {entity})
    {
        {entity}.setUpdateBy(SecurityUtils.getUsername());
        return toAjax({entity}Service.update{Entity}({entity}));
    }

    /**
     * 删除{functionName}
     */
    @RequiresPermissions("{permPrefix}:{businessName}:remove")
    @Log(title = "{functionName}", businessType = BusinessType.DELETE)
    @DeleteMapping("/{{pk}}s")
    public AjaxResult remove(@PathVariable {pkType}[] {pk}s)
    {
        return toAjax({entity}Service.delete{Entity}By{Pk}s({pk}s));
    }
}
```

- `@Validated` 需要 `import org.springframework.validation.annotation.Validated;`
- 有唯一性校验时在 add/edit 中先调 `check{Field}Unique`，失败 `return error("新增{functionName}'" + {entity}.get{Field}() + "'失败，{字段注释}已存在");`

---

## 6. Mapper XML {Entity}Mapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
"http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{package}.mapper.{Entity}Mapper">

    <resultMap type="{Entity}" id="{Entity}Result">
        <id     property="{pk}"       column="{pkColumn}"       />
        <result property="{field}"    column="{fieldColumn}"    />
        <result property="createBy"   column="create_by"   />
        <result property="createTime" column="create_time" />
        <result property="updateBy"   column="update_by"   />
        <result property="updateTime" column="update_time" />
        <result property="remark"     column="remark"      />
    </resultMap>

    <sql id="select{Entity}Vo">
        select {pkColumn}, {fieldColumn}, create_by, create_time, update_by, update_time, remark from {table}
    </sql>

    <select id="select{Entity}List" parameterType="{Entity}" resultMap="{Entity}Result">
        <include refid="select{Entity}Vo"/>
        <where>
            <if test="{field} != null  and {field} != ''"> and {fieldColumn} = #{field}</if>
            <!-- LIKE 查询写法： -->
            <!-- <if test="{field} != null and {field} != ''"> and {fieldColumn} like concat('%', #{field}, '%')</if> -->
            <!-- BETWEEN 查询写法（ BaseEntity.params 传参）： -->
            <!-- <if test="params.begin{Field} != null and params.begin{Field} != '' and params.end{Field} != null and params.end{Field} != ''"> and {fieldColumn} between #{params.begin{Field}} and #{params.end{Field}}</if> -->
        </where>
    </select>

    <select id="select{Entity}By{Pk}" parameterType="{pkType}" resultMap="{Entity}Result">
        <include refid="select{Entity}Vo"/>
        where {pkColumn} = #{pk}
    </select>

    <insert id="insert{Entity}" parameterType="{Entity}" useGeneratedKeys="true" keyProperty="{pk}">
        insert into {table}
        <trim prefix="(" suffix=")" suffixOverrides=",">
            <if test="{field} != null and {field} != ''">{fieldColumn},</if>
            <if test="createBy != null">create_by,</if>
            create_time
        </trim>
        <trim prefix="values (" suffix=")" suffixOverrides=",">
            <if test="{field} != null and {field} != ''">#{field},</if>
            <if test="createBy != null">#{createBy},</if>
            sysdate()
        </trim>
    </insert>

    <update id="update{Entity}" parameterType="{Entity}">
        update {table}
        <trim prefix="SET" suffixOverrides=",">
            <if test="{field} != null and {field} != ''">{fieldColumn} = #{field},</if>
            <if test="updateBy != null and updateBy != ''">update_by = #{updateBy},</if>
            <if test="remark != null">remark = #{remark},</if>
            update_time = sysdate()
        </trim>
        where {pkColumn} = #{pk}
    </update>

    <delete id="delete{Entity}By{Pk}" parameterType="{pkType}">
        delete from {table} where {pkColumn} = #{pk}
    </delete>

    <delete id="delete{Entity}By{Pk}s" parameterType="String">
        delete from {table} where {pkColumn} in
        <foreach item="{pk}" collection="array" open="(" separator="," close=")">
            #{pk}
        </foreach>
    </delete>

</mapper>
```

要点：
- 每个业务字段都要出现在 `resultMap`、`<sql id>` 切片、`insert`、`update`、动态 `<if>` 中（按需），遗漏任一处都是常见 bug。
- 逻辑删除表：delete 语句改为 `update {table} set del_flag = '2' where ...`，且所有 select 追加 `where del_flag = '0'`（或 `<sql>` 切片中体现）。
- XML 严格放在 `resources/mapper/<moduleDir>/`，文件名与 Mapper 接口同名。

---

## 7. DDL 模板（可选，sql/{table}.sql）

```sql
-- ----------------------------
-- {functionName}表
-- ----------------------------
drop table if exists {table};
create table {table} (
  {pkColumn}          bigint(20)      not null auto_increment    comment '{pk注释}',
  {fieldColumn}       varchar({长度})  default ''                 comment '{字段注释}',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime                                   comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime                                   comment '更新时间',
  remark              varchar(500)    default null               comment '备注',
  primary key ({pkColumn})
) engine=innodb auto_increment=100 comment = '{functionName}表';
```

---

## 8. 菜单权限 SQL 模板（可选）

```sql
-- 一级菜单（若已有目录/菜单，parent_id 相应调整；menu_id 用不冲突的值）
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values ('{functionName}', 2000, 1, '{businessName}', '{模块视图路径}/{businessName}/index', 1, 0, 'C', '0', '0', '{permPrefix}:{businessName}:list', '#', 'admin', sysdate(), '{functionName}菜单');

-- 按钮（parent_id 为上面菜单的 id）
select @parentId := last_insert_id();
insert into sys_menu values(null, '{functionName}查询', @parentId, 1,  '#', '', 1, 0, 'F', '0', '0', '{permPrefix}:{businessName}:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(null, '{functionName}新增', @parentId, 2,  '#', '', 1, 0, 'F', '0', '0', '{permPrefix}:{businessName}:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(null, '{functionName}修改', @parentId, 3,  '#', '', 1, 0, 'F', '0', '0', '{permPrefix}:{businessName}:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(null, '{functionName}删除', @parentId, 4,  '#', '', 1, 0, 'F', '0', '0', '{permPrefix}:{businessName}:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(null, '{functionName}导出', @parentId, 5,  '#', '', 1, 0, 'F', '0', '0', '{permPrefix}:{businessName}:export', '#', 'admin', sysdate(), '', null, '');
```

生成后提醒用户：核对 `sys_menu` 列数与当前库表结构一致（不同版本列可能不同），menu_id/parent_id 按实际调整。

---

## §MP 模式（MyBatis-Plus，纯单表优先，**无数据权限时不生成 Mapper XML**）

> 适用：新业务模块（`amy-biz-modules/**`）的纯单表 CRUD。多表/复杂 SQL 仍走上面 §1–§6 的 xml 模式。
> MP 依赖与全局配置已在 `amy-common-core`（`MybatisPlusConfig` 注册 `PaginationInnerInterceptor`、`typeAliasesPackage=com.amy.**.domain`）就绪，新模块直接继承即可。
> **`BaseEntity` 的 `params`/`searchValue` 已标 `@TableField(exist = false)`，实体里不要重复声明，也不要删掉那两个注解。**

### MP.1 实体 {Entity}.java（Lombok 变体，模块有 lombok 时用）

```java
package {package}.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.amy.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * {functionName}对象 {table}
 *
 * @author amy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("{table}")
public class {Entity} extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** {pk注释} */
    @TableId(type = IdType.AUTO)
    private {pkType} {pk};

    /** {字段注释} */
    @NotBlank(message = "{字段注释}不能为空")
    @Size(min = 0, max = {长度}, message = "{字段注释}长度不能超过{长度}个字符")
    private {FieldJavaType} {field};

    /** 逻辑删除：0 存在 / 2 删除（RuoYi del_flag 约定） */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
```

> 无 lombok 的模块：改为手写 getter/setter + `@ToStringStyle.MULTI_LINE_STYLE` 的 `toString()`，校验注解放 getter 上（与 §1 一致）。

### MP.2 Mapper 接口 {Entity}Mapper.java

```java
package {package}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import {package}.domain.{Entity};
import org.apache.ibatis.annotations.Param;

/**
 * {functionName} 数据层（MP）
 *
 * @author amy
 */
public interface {Entity}Mapper extends BaseMapper<{Entity}>
{
    // 仅“需要数据权限”时保留下面这个方法（对应 MP.6 的最小 XML）；纯单表无数据权限则整个接口只写 extends BaseMapper<{Entity}>
    IPage<{Entity}> select{Entity}List(IPage<{Entity}> page, @Param("entity") {Entity} entity);
}
```

### MP.3 Service 接口 I{Entity}Service.java

```java
package {package}.service;

import com.baomidou.mybatisplus.spring.service.IService;
import {package}.domain.{Entity};

/**
 * {functionName} 服务层（MP）
 *
 * @author amy
 */
public interface I{Entity}Service extends IService<{Entity}>
{
}
```

> 如需自定义方法（如唯一性校验），在接口与实现里自行加；MP 已提供 save / getById / page / removeByIds / updateById / list 等。

### MP.4 Service 实现 {Entity}ServiceImpl.java

```java
package {package}.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import {package}.domain.{Entity};
import {package}.mapper.{Entity}Mapper;
import {package}.service.I{Entity}Service;
import org.springframework.stereotype.Service;

/**
 * {functionName}服务层实现（MP）
 *
 * @author amy
 */
@Service
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}> implements I{Entity}Service
{
}
```

### MP.5 Controller {Entity}Controller.java

```java
package {package}.controller;

import com.amy.common.core.web.controller.BaseController;
import com.amy.common.core.web.domain.AjaxResult;
import com.amy.common.core.web.page.TableDataInfo;
import com.amy.common.log.annotation.Log;
import com.amy.common.log.enums.BusinessType;
import com.amy.common.security.annotation.RequiresPermissions;
import com.amy.common.security.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import {package}.domain.{Entity};
import {package}.service.I{Entity}Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * {functionName} 信息操作处理（MP）
 *
 * @author amy
 */
@RestController
@RequestMapping("/{businessName}")
public class {Entity}Controller extends BaseController
{
    @Autowired
    private I{Entity}Service {entity}Service;

    /** 查询{functionName}列表（MP 分页 + 可选 @DataScope 数据权限） */
    @RequiresPermissions("{permPrefix}:{businessName}:list")
    @DataScope(deptAlias = "d")
    @GetMapping("/list")
    public TableDataInfo list({Entity} {entity})
    {
        Page<{Entity}> page = new Page<>(1, 10); // 实际 pageNum/pageSize 来自请求，参考下方说明
        IPage<{Entity}> iPage = {entity}Service.select{Entity}List(page, {entity}); // 无数据权限时直接 baseMapper.page 或 service.page(...)
        return new TableDataInfo(iPage.getRecords(), iPage.getTotal());
    }

    /** 获取{functionName}详细信息 */
    @RequiresPermissions("{permPrefix}:{businessName}:query")
    @GetMapping(value = "/{{pk}}")
    public AjaxResult getInfo(@PathVariable("{pk}") {pkType} {pk})
    {
        return success({entity}Service.getById({pk}));
    }

    /** 新增{functionName} */
    @RequiresPermissions("{permPrefix}:{businessName}:add")
    @Log(title = "{functionName}", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody {Entity} {entity})
    {
        {entity}.setCreateBy(SecurityUtils.getUsername());
        return toAjax({entity}Service.save({entity}));
    }

    /** 修改{functionName} */
    @RequiresPermissions("{permPrefix}:{businessName}:edit")
    @Log(title = "{functionName}", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody {Entity} {entity})
    {
        {entity}.setUpdateBy(SecurityUtils.getUsername());
        return toAjax({entity}Service.updateById({entity}));
    }

    /** 删除{functionName} */
    @RequiresPermissions("{permPrefix}:{businessName}:remove")
    @Log(title = "{functionName}", businessType = BusinessType.DELETE)
    @DeleteMapping("/{{pk}}s")
    public AjaxResult remove(@PathVariable {pkType}[] {pk}s)
    {
        return toAjax({entity}Service.removeByIds(java.util.Arrays.asList({pk}s)));
    }
}
```

> 分页入参：若沿用 RuoYi `PageDomain`/`TableSupport`（`pageNum/pageSize/orderByColumn/isAsc`），在 `list` 起始处用 `PageDomain pd = getPageDomain(); Page<{Entity}> page = new Page<>(pd.getPageNum(), pd.getPageSize());` 即可。MP 的 `PaginationInnerInterceptor` 会自动织入 `limit`。**切勿**在此再调用 `PageHelper.startPage()`。

### MP.6 最小 Mapper XML {Entity}Mapper.xml（**仅需要数据权限 `@DataScope` 时生成**）

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{package}.mapper.{Entity}Mapper">

    <select id="select{Entity}List" resultType="{Entity}">
        select {pkColumn}, {fieldColumn}, del_flag, create_by, create_time, update_by, update_time, remark
        from {table} d
        <where>
            d.del_flag = '0'
            <if test="entity.{field} != null and entity.{field} != ''">AND d.{fieldColumn} like concat('%', #{entity.{field}}, '%')</if>
            ${entity.params.dataScope}   <!-- @DataScope 切面注入，如 deptAlias="d" 会注入 AND (d.dept_id IN (...)) -->
        </where>
    </select>

</mapper>
```

要点：
- 这个 XML 只放一个带 `${entity.params.dataScope}` 的列表方法；其余 CRUD 仍由 MP `BaseMapper` 自动完成，**不要**为了“完整”而补一整套 XML（那就退化成 xml 模式了）。
- `resultType="{Entity}"` 用短类名即可（`typeAliasesPackage=com.amy.**.domain` 已覆盖）。
- `${entity.params.dataScope}` 用 `$` 是 RuoYi `@DataScope` 设计，注入的是由切面拼好的 SQL 片段，非用户入参，安全。
