package com.amy.template.domain.entity;

import java.util.Date;
import com.amy.common.core.annotation.Excel;
import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * MP 集成验证用极小实体（POC 模板）。
 * <p>
 * 演示：新功能纯单表场景走 MyBatis-Plus（BaseMapper / IService），零 Mapper XML。
 * delFlag 继承自 BaseEntity（已配置 @TableLogic，默认软删除，0=存在 2=删除），无需在本类重复声明。
 * 想换成真实业务表，复制此类改 @TableName / 字段即可（继承 BaseEntity 即自动获得软删除能力）。
 * </p>
 *
 * @author amy
 */
@TableName("mp_poc_demo")
public class MpPocDemo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 示例ID */
    @Excel(name = "示例ID")
    @TableId(type = IdType.AUTO)
    private Long demoId;

    /** 示例名称 */
    @Excel(name = "示例名称")
    private String demoName;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private Integer status;

    /** 部门ID（用于数据权限验证） */
    private Long deptId;

    public Long getDemoId()
    {
        return demoId;
    }

    public void setDemoId(Long demoId)
    {
        this.demoId = demoId;
    }

    public String getDemoName()
    {
        return demoName;
    }

    public void setDemoName(String demoName)
    {
        this.demoName = demoName;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }
}
