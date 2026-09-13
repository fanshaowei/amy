package com.amy.template.mapper;

import org.apache.ibatis.annotations.Param;
import com.amy.template.domain.entity.MpPocDemo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * MP 集成验证 Mapper（POC 模板）。
 * <p>
 * 继承 BaseMapper 即可获得 selectById / insert / updateById / deleteById / page 等通用 CRUD，无需 XML。
 * 仅「需要数据权限」的列表查询保留自定义方法 + XML（沿用若依 @DataScope 切面注入 ${params.dataScope}）。
 * </p>
 *
 * @author amy
 */
public interface MpPocDemoMapper extends BaseMapper<MpPocDemo>
{
    /**
     * 分页列表（带数据权限）
     *
     * @param page    MP 分页对象，自动物化分页 SQL（LIMIT/COUNT）
     * @param entity  查询条件 + 数据权限 SQL 片段所在参数
     * @return 分页结果
     */
    IPage<MpPocDemo> selectMpPocDemoList(IPage<MpPocDemo> page, @Param("entity") MpPocDemo entity);
}
